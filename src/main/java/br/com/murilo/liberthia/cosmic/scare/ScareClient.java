package br.com.murilo.liberthia.cosmic.scare;

import br.com.murilo.liberthia.LiberthiaMod;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Random;

/**
 * r173: Cliente do sistema de 4ª parede. Mantém o overlay ativo e o renderiza
 * por cima de tudo. CRASH/KICK abrem telas falsas; o resto é overlay temporizado.
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ScareClient {

    private ScareClient() {}

    private static final Random RNG = new Random();
    public static final int FACE_COUNT = 6;

    private static ScareType active;
    private static int ticksLeft;
    private static int total;
    private static int variant;
    private static String text = "";

    public static void receive(ScareS2CPacket p) {
        Minecraft mc = Minecraft.getInstance();
        switch (p.type) {
            case CRASH -> mc.setScreen(new FakeCrashScreen());
            case KICK -> mc.setScreen(new FakeKickScreen(p.text));
            case FAKEDEATH -> mc.setScreen(new FakeDeathScreen(p.text));
            case BSOD -> mc.setScreen(new FakeBsodScreen());
            case FAKECHAT -> {
                if (mc.gui != null && p.text != null && !p.text.isEmpty()) {
                    mc.gui.getChat().addMessage(net.minecraft.network.chat.Component.literal(
                            p.text.replace('&', '§')));
                }
            }
            case RELOADFACES -> {
                FaceImages.reload();
                if (mc.gui != null) {
                    mc.gui.getChat().addMessage(net.minecraft.network.chat.Component.literal(
                            "§5✦ Rostos recarregados: §d" + FaceImages.count() + " §7(1–6 embutidos + extras de liberthia_faces/)"));
                }
            }
            case FAKEJOIN -> injectFakeJoin(mc, p.variant, p.text);
            case SCREENSHOT -> { injectScreenshot(mc, p.text); setOverlay(p); }
            default -> setOverlay(p);
        }
    }

    private static void setOverlay(ScareS2CPacket p) {
        active = p.type;
        total = ticksLeft = Math.max(1, p.duration);
        variant = p.variant;
        text = p.text;
    }

    /** Linha de chat falsa de entrou/saiu/morreu (parece outro player na partida). */
    private static void injectFakeJoin(Minecraft mc, int v, String name) {
        if (mc.gui == null) return;
        String n = (name == null || name.isEmpty()) ? "?????" : name;
        String msg = switch (Math.floorMod(v, 3)) {
            case 1 -> "§e" + n + " left the game";
            case 2 -> "§7" + n + " foi morto por algo invisível";
            default -> "§e" + n + " joined the game";
        };
        mc.gui.getChat().addMessage(net.minecraft.network.chat.Component.literal(msg));
    }

    /** Mensagem falsa de "captura de tela salva" — como se alguém tivesse te fotografado. */
    private static void injectScreenshot(Minecraft mc, String name) {
        if (mc.gui == null) return;
        String fn = (name == null || name.isEmpty()) ? "voce" : name;
        mc.gui.getChat().addMessage(net.minecraft.network.chat.Component.literal(
                "§7Captura de tela salva: §f" + fn + "_" + (1000 + RNG.nextInt(9000)) + ".png"));
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        if (ticksLeft > 0) { ticksLeft--; if (ticksLeft <= 0) active = null; }
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post e) {
        if (active == null || ticksLeft <= 0) return;
        GuiGraphics g = e.getGuiGraphics();
        Minecraft mc = Minecraft.getInstance();
        int w = mc.getWindow().getGuiScaledWidth();
        int h = mc.getWindow().getGuiScaledHeight();
        switch (active) {
            case ALERT -> renderAlert(g, mc, w, h);
            case SHAKE -> renderShake(g, mc, w, h);
            case STATIC -> renderStatic(g, w, h);
            case FLASH -> renderFlash(g, w, h);
            case WHISPER -> renderWhisper(g, mc, w, h);
            case BLACKOUT -> renderBlackout(g, w, h);
            case EYES -> renderEyes(g, w, h);
            case HEARTBEAT -> renderHeartbeat(g, w, h);
            case GLITCH -> renderGlitch(g, w, h);
            case CRACK -> renderCrack(g, w, h);
            case COUNTDOWN -> renderCountdown(g, mc, w, h);
            case WATERMARK -> renderWatermark(g, mc, w, h);
            case EYE -> renderEye(g, w, h);
            case TUNNEL -> renderTunnel(g, w, h);
            case SCREENSHOT -> renderScreenshot(g, w, h);
            case SCANROLL -> renderScanroll(g, w, h);
            case NOTRESPONDING -> renderNotResponding(g, mc, w, h);
            case CORRUPTHUD -> renderCorruptHud(g, mc, w, h);
            case NARRATOR -> renderNarrator(g, mc, w, h);
            case CURSOR -> renderCursor(g, w, h);
            case FALSECOORDS -> renderFalseCoords(g, mc, w, h);
            case BLINK -> renderBlink(g, w, h);
            case LOWBATTERY -> renderLowBattery(g, mc, w, h);
            case DISCORDPING -> renderDiscordPing(g, mc, w, h);
            case TYPETEXT -> renderTypeText(g, mc, w, h);
            default -> {}   // REVERSE (câmera) e MIRROR (flip do HUD) tratados em hooks próprios
        }
    }

    // MIRROR: espelha o HUD horizontalmente (push no Pre, pop no Post LOWEST).
    @SubscribeEvent
    public static void onGuiPre(RenderGuiEvent.Pre e) {
        if (active == ScareType.MIRROR && ticksLeft > 0) {
            var g = e.getGuiGraphics();
            int w = Minecraft.getInstance().getWindow().getGuiScaledWidth();
            g.pose().pushPose();
            g.pose().translate(w, 0, 0);
            g.pose().scale(-1F, 1F, 1F);
        }
    }

    @SubscribeEvent(priority = net.minecraftforge.eventbus.api.EventPriority.LOWEST)
    public static void onGuiPostUnflip(RenderGuiEvent.Post e) {
        if (active == ScareType.MIRROR && ticksLeft > 0) {
            e.getGuiGraphics().pose().popPose();
        }
    }

    /** Popup falso de "bateria fraca" do PC (canto superior direito). */
    private static void renderLowBattery(GuiGraphics g, Minecraft mc, int w, int h) {
        int bw = 150, bh = 38, x = w - bw - 8, y = 8;
        g.fill(x, y, x + bw, y + bh, 0xEE202022);
        g.fill(x, y, x + bw, y + 1, 0xFF555560);
        // ícone de bateria quase vazia
        int bx = x + 8, by = y + 13;
        g.fill(bx, by, bx + 22, by + 12, 0xFFAAAAAA);
        g.fill(bx + 1, by + 1, bx + 21, by + 11, 0xFF101010);
        g.fill(bx + 1, by + 1, bx + 4, by + 11, 0xFFFF3030);   // 7%
        g.fill(bx + 22, by + 4, bx + 24, by + 8, 0xFFAAAAAA);
        g.drawString(mc.font, "Bateria fraca", x + 38, y + 8, 0xFFFFFFFF, false);
        g.drawString(mc.font, "7% restante", x + 38, y + 21, 0xFFBBBBBB, false);
    }

    /** Toast falso de notificação (canto inferior direito), desliza ao surgir. */
    private static void renderDiscordPing(GuiGraphics g, Minecraft mc, int w, int h) {
        float p = ticksLeft / (float) Math.max(1, total);
        int slide = (int) ((1F - Math.min(1F, (1F - p) * 4F)) * 60); // entra deslizando
        int bw = 168, bh = 34, x = w - bw - 8 + slide, y = h - bh - 8;
        g.fill(x, y, x + bw, y + bh, 0xF02B2D31);
        g.fill(x, y, x + 4, y + bh, 0xFF5865F2);       // barra "blurple"
        g.fill(x + 9, y + 8, x + 27, y + 26, 0xFF5865F2);
        g.drawString(mc.font, "1 nova mensagem", x + 33, y + 7, 0xFFFFFFFF, false);
        g.drawString(mc.font, "?????: ele te viu", x + 33, y + 20, 0xFFB9BBBE, false);
    }

    /** Alguém "digitando" no chat: indicador + texto revelado char a char. */
    private static void renderTypeText(GuiGraphics g, Minecraft mc, int w, int h) {
        String full = text.isEmpty() ? "eu sei onde voce esta" : text;
        float p = 1F - (ticksLeft / (float) Math.max(1, total)); // 0→1
        int shown = (int) Math.min(full.length(), Math.max(0, (p - 0.25F) / 0.6F * full.length()));
        String typed = full.substring(0, Math.max(0, shown));
        boolean caret = (System.currentTimeMillis() / 300L) % 2 == 0;
        int y = h - 48;
        if (p < 0.3F) {
            g.drawString(mc.font, "§7?????  está digitando...", 6, y, 0xFF888888, false);
        } else {
            g.drawString(mc.font, "§f<?????> §7" + typed + (caret && shown < full.length() ? "_" : ""), 6, y, 0xFFE0E0E0, true);
        }
    }

    /** REVERSE: vira o MUNDO de cabeça pra baixo (roll de 180° na câmera). */
    @SubscribeEvent
    public static void onCameraAngles(net.minecraftforge.client.event.ViewportEvent.ComputeCameraAngles e) {
        if (active == ScareType.REVERSE && ticksLeft > 0) e.setRoll(180F);
    }

    /** Fake "Não Respondendo" — wash cinza + barra de título + spinner girando. */
    private static void renderNotResponding(GuiGraphics g, Minecraft mc, int w, int h) {
        g.fill(0, 0, w, h, 0x33B0B0B0);
        g.fill(0, 0, w, 14, 0xE0202024);
        g.drawString(mc.font, "Minecraft* (Nao Respondendo)", 6, 3, 0xFFFFFFFF, false);
        double a = System.currentTimeMillis() / 90.0;
        int cx = w / 2, cy = h / 2;
        for (int i = 0; i < 8; i++) {
            double ang = a + i * (Math.PI / 4);
            int sx = cx + (int) (Math.cos(ang) * 14), sy = cy + (int) (Math.sin(ang) * 14);
            int al = (int) (255 * (i / 8.0));
            g.fill(sx - 2, sy - 2, sx + 2, sy + 2, (al << 24) | 0x00FFFFFF);
        }
    }

    /** HUD corrompido — glitch sobre a hotbar + valores de vida/fome errados. */
    private static void renderCorruptHud(GuiGraphics g, Minecraft mc, int w, int h) {
        int top = h - 48;
        int[] c = {0x90FF2020, 0x9020FF60, 0x902060FF, 0x90FFFFFF, 0x90000000};
        for (int i = 0; i < 30; i++) {
            int x = RNG.nextInt(w), y = top + RNG.nextInt(48);
            g.fill(x, y, x + 4 + RNG.nextInt(20), y + 2 + RNG.nextInt(5), c[RNG.nextInt(c.length)]);
        }
        g.drawString(mc.font, "HP " + (RNG.nextInt(2) == 0 ? "-" : "") + RNG.nextInt(99),
                w / 2 - 91, h - 38, 0xFFFF4040, true);
        g.drawString(mc.font, "FOME " + RNG.nextInt(40), w / 2 + 40, h - 38, 0xFFFFC020, true);
    }

    /** Legenda de "narrador" no rodapé, te descrevendo. */
    private static void renderNarrator(GuiGraphics g, Minecraft mc, int w, int h) {
        String t = text.isEmpty() ? "ele sabe que voce esta lendo isto" : text;
        int tw = mc.font.width(t), x = (w - tw) / 2, y = h - 62;
        g.fill(x - 6, y - 3, x + tw + 6, y + 11, 0xB0000000);
        g.drawString(mc.font, t, x, y, 0xFFE8E8E8, false);
    }

    /** Cursor de mouse fantasma percorrendo a tela em curva de Lissajous. */
    private static void renderCursor(GuiGraphics g, int w, int h) {
        double t = System.currentTimeMillis() / 1000.0;
        int cx = (int) (w * (0.5 + 0.40 * Math.sin(t * 1.3)));
        int cy = (int) (h * (0.5 + 0.40 * Math.sin(t * 0.9 + 1.0)));
        arrow(g, cx, cy, 0xFF000000, 1); // contorno
        arrow(g, cx, cy, 0xFFFFFFFF, 0); // corpo
    }

    private static void arrow(GuiGraphics g, int x, int y, int color, int pad) {
        for (int i = 0; i <= 11; i++) {
            int len = (i <= 8) ? (i + 1) : Math.max(1, 8 - (i - 8) * 2);
            g.fill(x - pad, y + i - pad, x + len + pad, y + i + 1 + pad, color);
        }
    }

    /** Coords/nome reais do player rabiscados na tela, como anotações de alguém. */
    private static void renderFalseCoords(GuiGraphics g, Minecraft mc, int w, int h) {
        if (mc.player == null) return;
        java.util.Random r = new java.util.Random(System.currentTimeMillis() / 1500L);
        String[] notes = {
                String.format("x:%d y:%d z:%d", (int) mc.player.getX(), (int) mc.player.getY(), (int) mc.player.getZ()),
                mc.player.getGameProfile().getName(),
                "ele esta aqui",
                "eu o encontrei"
        };
        for (int i = 0; i < notes.length; i++) {
            int x = 16 + r.nextInt(Math.max(1, w - 150));
            int y = 16 + r.nextInt(Math.max(1, h - 70));
            g.drawString(mc.font, notes[i], x, y, 0x90FF4040, false);
        }
    }

    /** Piscada: pálpebras pretas fecham (1ª metade) e abrem (2ª metade) do tempo. */
    private static void renderBlink(GuiGraphics g, int w, int h) {
        float p = (total - ticksLeft) / (float) Math.max(1, total); // 0→1
        float close = p < 0.5F ? (p / 0.5F) : (1F - (p - 0.5F) / 0.5F);
        int band = (int) ((h / 2.0F) * close);
        g.fill(0, 0, w, band, 0xFF000000);
        g.fill(0, h - band, w, h, 0xFF000000);
    }

    /** Tela trincada: rachaduras radiais a partir de um ponto (padrão estável por variant). */
    private static void renderCrack(GuiGraphics g, int w, int h) {
        java.util.Random r = new java.util.Random(1337L + variant);
        int ix = Math.max(20, Math.min(w - 20, w / 2 + (variant == 0 ? 0 : r.nextInt(Math.max(1, w / 2)) - w / 4)));
        int iy = (int) (h * 0.42);
        g.fill(0, 0, w, h, 0x22000000);
        int branches = 10 + r.nextInt(4);
        for (int b = 0; b < branches; b++) {
            crackLine(g, ix, iy, r.nextDouble() * Math.PI * 2, Math.min(w, h) * (0.3 + r.nextDouble() * 0.55), r);
        }
        for (int i = 0; i < 6; i++) {
            int ex = ix + r.nextInt(15) - 7, ey = iy + r.nextInt(15) - 7;
            g.fill(ex, ey, ex + 2, ey + 2, 0xFFEAEAF2);
        }
    }

    private static void crackLine(GuiGraphics g, int x0, int y0, double ang, double len, java.util.Random r) {
        double x = x0, y = y0;
        for (int s = 0; s < (int) len; s += 2) {
            ang += (r.nextDouble() - 0.5) * 0.16;
            x += Math.cos(ang) * 2; y += Math.sin(ang) * 2;
            int px = (int) x, py = (int) y;
            g.fill(px - 1, py - 1, px + 2, py + 2, 0xAA0C0C10); // sombra
            g.fill(px, py, px + 1, py + 1, 0xFFE0E0EC);          // núcleo claro
        }
    }

    /** Contagem regressiva grande no centro (vermelha nos últimos 3s). */
    private static void renderCountdown(GuiGraphics g, Minecraft mc, int w, int h) {
        g.fill(0, 0, w, h, 0x55000000);
        int secs = (ticksLeft + 19) / 20;
        float frac = (ticksLeft % 20) / 20F;
        float scale = 5.5F + (1F - frac) * 1.8F;
        int col = 0xFF000000 | (secs <= 3 ? 0x00FF2020 : 0x00FFFFFF);
        String s = String.valueOf(Math.max(0, secs));
        g.pose().pushPose();
        g.pose().translate(w / 2.0, h / 2.0, 0);
        g.pose().scale(scale, scale, 1F);
        g.drawString(mc.font, s, -mc.font.width(s) / 2, -4, col, true);
        g.pose().popPose();
        if (!text.isEmpty()) line(g, mc.font, text, w / 2, h / 2 + (int) (scale * 9), 1.4F, 0xFFC0C0C0);
    }

    /** Marca d'água gigante translúcida surgindo e sumindo. */
    private static void renderWatermark(GuiGraphics g, Minecraft mc, int w, int h) {
        float prog = ticksLeft / (float) Math.max(1, total);
        float a = (float) Math.sin((1 - prog) * Math.PI);
        int alpha = (int) (Math.max(0F, Math.min(1F, a)) * 90) << 24;
        String t = text.isEmpty() ? "EU TE VEJO" : text;
        float scale = Math.max(2F, (w * 0.8F) / Math.max(1, mc.font.width(t)));
        g.pose().pushPose();
        g.pose().translate(w / 2.0, h / 2.0, 0);
        g.pose().scale(scale, scale, 1F);
        g.drawString(mc.font, t, -mc.font.width(t) / 2, -4, alpha | 0x00FF3030, false);
        g.pose().popPose();
    }

    /** Olho gigante que abre, te encara e pisca. */
    private static void renderEye(GuiGraphics g, int w, int h) {
        g.fill(0, 0, w, h, 0xE0000000);
        int cx = w / 2, cy = h / 2;
        int ew = (int) (Math.min(w, h) * 0.34);
        int eh = (int) (ew * 0.62);
        float open = Math.min(1F, (total - ticksLeft) / Math.max(1F, total * 0.25F));
        long ms = System.currentTimeMillis();
        boolean blink = (ms / 140L) % 18 == 0;
        int ryNow = (int) (eh * (blink ? 0.08F : open));
        if (ryNow < 2) return;
        ellipse(g, cx, cy, ew, ryNow, 0xFFE8E4D8);               // esclera
        int ir = (int) (ryNow * 0.95);
        ellipse(g, cx, cy, ir, ir, 0xFF7A1010);                  // íris vermelha
        int pr = Math.max(2, (int) (ir * 0.45));
        int px = cx + (int) (Math.sin(ms / 900.0) * ir * 0.3);   // pupila se move
        ellipse(g, px, cy, pr, pr, 0xFF000000);                  // pupila
        ellipse(g, px - pr / 3, cy - pr / 3, Math.max(1, pr / 4), Math.max(1, pr / 4), 0xCCFFFFFF);
    }

    private static void ellipse(GuiGraphics g, int cx, int cy, int rx, int ry, int color) {
        if (rx <= 0 || ry <= 0) return;
        for (int dy = -ry; dy <= ry; dy++) {
            double f = 1.0 - (double) (dy * dy) / (double) (ry * ry);
            if (f <= 0) continue;
            int half = (int) (rx * Math.sqrt(f));
            g.fill(cx - half, cy + dy, cx + half, cy + dy + 1, color);
        }
    }

    /** Visão de túnel: bordas pretas fecham até quase tudo e seguram (desmaio). */
    private static void renderTunnel(GuiGraphics g, int w, int h) {
        float p = (total - ticksLeft) / (float) Math.max(1, total);
        float close = (float) Math.sin(Math.min(1F, p / 0.8F) * (Math.PI / 2));
        int band = (int) ((Math.min(w, h) / 2.0) * close) + (int) (Math.sin(System.currentTimeMillis() / 120.0) * 3);
        int black = 0xFF000000;
        g.fillGradient(0, 0, w, Math.max(0, band), black, 0x00000000);
        g.fillGradient(0, h - Math.max(0, band), w, h, 0x00000000, black);
        g.fillGradient(0, 0, Math.max(0, band), h, black, 0x00000000);
        g.fillGradient(w - Math.max(0, band), 0, w, h, 0x00000000, black);
        int core = Math.max(0, band - 24);
        g.fill(0, 0, w, core, black);
        g.fill(0, h - core, w, h, black);
    }

    /** Flash branco de "foto" que some rápido. */
    private static void renderScreenshot(GuiGraphics g, int w, int h) {
        int alpha = (int) ((ticksLeft / (float) Math.max(1, total)) * 255) << 24;
        g.fill(0, 0, w, h, alpha | 0x00FFFFFF);
    }

    /** Barra de varredura rolando + desync RGB + scanlines (perda de sinal). */
    private static void renderScanroll(GuiGraphics g, int w, int h) {
        long ms = System.currentTimeMillis();
        int barH = Math.max(8, h / 8);
        int y = (int) ((ms / 6L) % (h + barH)) - barH;
        g.fillGradient(0, y, w, y + barH / 2, 0x00FFFFFF, 0x55FFFFFF);
        g.fillGradient(0, y + barH / 2, w, y + barH, 0x55FFFFFF, 0x00FFFFFF);
        g.fill(0, y, w, y + 1, 0x80FF0033);
        g.fill(0, y + barH, w, y + barH + 1, 0x803366FF);
        for (int yy = 0; yy < h; yy += 3) g.fill(0, yy, w, yy + 1, 0x14000000);
    }

    /** Sussurro: texto fantasma que surge e some no centro. */
    private static void renderWhisper(GuiGraphics g, Minecraft mc, int w, int h) {
        float prog = ticksLeft / (float) Math.max(1, total);
        float a = (float) Math.sin(prog * Math.PI); // fade-in/out
        int alpha = (int) (Math.max(0F, Math.min(1F, a)) * 200) << 24;
        String t = text.isEmpty() ? "ele te vê" : text;
        g.pose().pushPose();
        g.pose().translate(w / 2.0, h / 2.0, 0);
        g.pose().scale(1.8F, 1.8F, 1F);
        g.drawString(mc.font, t, -mc.font.width(t) / 2, -4, alpha | 0x00C0C0C8, false);
        g.pose().popPose();
    }

    /** Apagão total. */
    private static void renderBlackout(GuiGraphics g, int w, int h) {
        g.fill(0, 0, w, h, 0xFF000000);
    }

    /** Pares de olhos brilhantes piscando nos cantos/bordas. */
    private static void renderEyes(GuiGraphics g, int w, int h) {
        long seed = (System.currentTimeMillis() / 700L);
        java.util.Random r = new java.util.Random(seed);
        int pairs = 3 + r.nextInt(3);
        for (int i = 0; i < pairs; i++) {
            int ex = 20 + r.nextInt(Math.max(1, w - 40));
            int ey = 20 + r.nextInt(Math.max(1, h - 40));
            boolean blink = ((System.currentTimeMillis() / 120L) % 9) != 0; // pisca ocasional
            if (!blink) continue;
            int eye = 0xFFCC1010;
            g.fill(ex, ey, ex + 6, ey + 3, eye);
            g.fill(ex + 12, ey, ex + 18, ey + 3, eye);
            g.fill(ex + 2, ey + 1, ex + 4, ey + 2, 0xFFFF7070);
            g.fill(ex + 14, ey + 1, ex + 16, ey + 2, 0xFFFF7070);
        }
    }

    /** Vinheta vermelha pulsando como um coração. */
    private static void renderHeartbeat(GuiGraphics g, int w, int h) {
        double t = (total - ticksLeft) * 0.18;
        float beat = (float) Math.max(0, Math.sin(t) * Math.sin(t)); // batida dupla-ish
        int a = (int) (beat * 150);
        int col = (a << 24) | 0x00A00000;
        int band = (int) (Math.min(w, h) * (0.18F + beat * 0.12F));
        g.fillGradient(0, 0, w, band, col, 0x00A00000);
        g.fillGradient(0, h - band, w, h, 0x00A00000, col);
        g.fillGradient(0, 0, band, h, col, 0x00A00000);
        g.fillGradient(w - band, 0, w, h, 0x00A00000, col);
    }

    /** Glitch: split RGB + barras coloridas + scanlines deslocando. */
    private static void renderGlitch(GuiGraphics g, int w, int h) {
        for (int i = 0; i < 14; i++) {
            int y = RNG.nextInt(h);
            int bh = 2 + RNG.nextInt(10);
            int off = RNG.nextInt(24) - 12;
            int[] cols = {0x80FF0033, 0x8000FF66, 0x803366FF, 0x80FFFFFF};
            int c = cols[RNG.nextInt(cols.length)];
            g.fill(off, y, w + off, y + bh, c);
        }
        for (int y = 0; y < h; y += 2) g.fill(0, y, w, y + 1, 0x14000000);
    }

    private static void renderAlert(GuiGraphics g, Minecraft mc, int w, int h) {
        g.fill(0, 0, w, h, 0xFFB00000);
        var font = mc.font;
        line(g, font, "EMERGENCY ALERT", w / 2, h / 2 - 70, 2.4F, 0xFFF2F2F2);
        line(g, font, "AUTHORITIES ISSUED", w / 2, h / 2 - 32, 1.3F, 0xFFE0C0C0);
        line(g, font, text.isEmpty() ? "CIVIL DANGER ALERT" : text, w / 2, h / 2 + 6, 2.2F, 0xFFF2F2F2);
        line(g, font, "^^^ THIS IS NOT A TEST ^^^", w / 2, h / 2 + 56, 1.2F, 0xFFD08080);
    }

    private static void renderShake(GuiGraphics g, Minecraft mc, int w, int h) {
        var font = mc.font;
        int dx = RNG.nextInt(9) - 4;
        int dy = RNG.nextInt(9) - 4;
        String t = text.isEmpty() ? "ELES TE VIRAM" : text;
        line(g, font, t, w / 2 + dx, h / 2 + dy, 2.6F, 0xFFFF3333);
    }

    private static void renderStatic(GuiGraphics g, int w, int h) {
        // ruído branco esparso + scanlines, re-randomizado cada frame (parece estática)
        for (int i = 0; i < 240; i++) {
            int x = RNG.nextInt(w), y = RNG.nextInt(h);
            int a = (0x30 + RNG.nextInt(0x80)) << 24;
            g.fill(x, y, x + 2, y + 2, a | 0x00FFFFFF);
        }
        for (int y = 0; y < h; y += 3) g.fill(0, y, w, y + 1, 0x18000000);
    }

    private static void renderFlash(GuiGraphics g, int w, int h) {
        // r175: usa o catálogo (6 embutidas + PNGs da pasta liberthia_faces/)
        FaceImages.Face face = FaceImages.get(variant + 1);
        int size = (int) (Math.min(w, h) * 0.85F);
        int x = (w - size) / 2, y = (h - size) / 2;
        // fade conforme o tempo restante
        float alpha = Math.min(1F, ticksLeft / (float) Math.max(1, total) + 0.2F);
        g.fill(0, 0, w, h, 0xCC000000);
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1F, 1F, 1F, alpha);
        g.blit(face.tex(), x, y, size, size, 0, 0, face.w(), face.h(), face.w(), face.h());
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        RenderSystem.disableBlend();
    }

    private static void line(GuiGraphics g, net.minecraft.client.gui.Font font, String s,
                             int cx, int cy, float scale, int color) {
        g.pose().pushPose();
        g.pose().translate(cx, cy, 0);
        g.pose().scale(scale, scale, 1F);
        g.drawString(font, s, -font.width(s) / 2, -4, color, true);
        g.pose().popPose();
    }
}
