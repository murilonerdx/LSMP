package br.com.murilo.liberthia.client.fourthwall;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.fourthwall.FourthWallFeature;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.CustomizeGuiOverlayEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * Lado cliente da quarta parede. Recebe os gatilhos do servidor
 * ({@code FourthWallTriggerS2CPacket} → {@link #onTrigger}) e renderiza cada
 * efeito por uma janela curta. Tudo baseado em timers de relógio real — nada
 * fica ligado pra sempre.
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID, value = Dist.CLIENT)
public final class FourthWallClient {

    private FourthWallClient() {}

    private static final Random RNG = new Random();
    private static final DateTimeFormatter HHMM = DateTimeFormatter.ofPattern("HH:mm");

    private static long crosshairEyeUntil = 0L;
    private static long f3LieUntil = 0L;
    private static int fakeOffX = 0, fakeOffZ = 0;
    private static Component pendingDeathText = null;
    private static long deathTextUntil = 0L;

    private static final String[] CLOCK_LINES = {
            "são %h aí. devia dormir.",
            "%h. ele também está acordado.",
            "ainda de pé às %h?",
            "%h… eu conto cada minuto seu.",
            "são %h. ninguém mais online. ou está?"
    };

    /** Chamado pelo packet (já na thread do cliente). */
    public static void onTrigger(int ordinal, String text, int duration) {
        FourthWallFeature[] vals = FourthWallFeature.values();
        if (ordinal < 0 || ordinal >= vals.length) return;
        FourthWallFeature f = vals[ordinal];
        Minecraft mc = Minecraft.getInstance();
        long now = System.currentTimeMillis();
        long ms = Math.max(0, duration) * 50L;

        switch (f) {
            case CROSSHAIR_EYE -> crosshairEyeUntil = now + (ms > 0 ? ms : 3000L);
            case F3_LIE -> {
                f3LieUntil = now + (ms > 0 ? ms : 30000L);
                fakeOffX = 64 + RNG.nextInt(4096);
                fakeOffZ = 64 + RNG.nextInt(4096);
            }
            case REAL_CLOCK -> showClockTaunt(mc);
            case FAKE_CRASH -> mc.setScreen(new FakeCrashScreen());
            case DEATH_SCREEN -> {
                Component c = Component.literal(text);
                if (duration == -1) { // teste: abre a tela AGORA
                    boolean hc = mc.level != null && mc.level.getLevelData().isHardcore();
                    mc.setScreen(new DeathScreen(c, hc));
                } else {
                    pendingDeathText = c;
                    deathTextUntil = now + 8000L;
                }
            }
            default -> {}
        }
    }

    private static void showClockTaunt(Minecraft mc) {
        if (mc.player == null || mc.gui == null) return;
        String hhmm = LocalTime.now().format(HHMM);
        String line = CLOCK_LINES[RNG.nextInt(CLOCK_LINES.length)].replace("%h", hhmm);
        mc.gui.setOverlayMessage(Component.literal("§8§o" + line), false);
    }

    // ── Crosshair vira olho ──────────────────────────────────────────
    @SubscribeEvent
    public static void onCrosshair(RenderGuiOverlayEvent.Pre event) {
        if (System.currentTimeMillis() >= crosshairEyeUntil) return;
        if (event.getOverlay() != VanillaGuiOverlay.CROSSHAIR.type()) return;
        event.setCanceled(true); // esconde a mira normal
        GuiGraphics g = event.getGuiGraphics();
        int cx = event.getWindow().getGuiScaledWidth() / 2;
        int cy = event.getWindow().getGuiScaledHeight() / 2;
        // lente (almond) avermelhada
        for (int dy = -4; dy <= 4; dy++) {
            int half = 7 - Math.abs(dy);
            if (half <= 0) continue;
            g.fill(cx - half, cy + dy, cx + half, cy + dy + 1, 0xCC6E0A0A);
        }
        // pupila — fenda vertical preta
        g.fill(cx - 1, cy - 4, cx + 1, cy + 5, 0xFF000000);
    }

    // ── F3 mente ─────────────────────────────────────────────────────
    @SubscribeEvent
    public static void onDebugText(CustomizeGuiOverlayEvent.DebugText event) {
        if (System.currentTimeMillis() >= f3LieUntil) return;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null) return;
        double fx = p.getX() + fakeOffX, fy = p.getY() - 13.0, fz = p.getZ() + fakeOffZ;
        List<String> left = event.getLeft();
        for (int i = 0; i < left.size(); i++) {
            String s = left.get(i);
            if (s.startsWith("XYZ:")) {
                left.set(i, String.format(Locale.ROOT, "XYZ: %.3f / %.5f / %.3f", fx, fy, fz));
            } else if (s.startsWith("Block:")) {
                left.set(i, "Block: " + (int) fx + " " + (int) fy + " " + (int) fz);
            } else if (s.startsWith("Chunk:")) {
                left.set(i, "Chunk: ?? ?? ?? in ?? ?? ??");
            }
        }
        List<String> right = event.getRight();
        for (int i = 0; i < right.size(); i++) {
            if (right.get(i).startsWith("Biome:")) right.set(i, "Biome: minecraft:the_void");
        }
    }

    // ── Tela de morte torta ──────────────────────────────────────────
    @SubscribeEvent
    public static void onScreenOpen(ScreenEvent.Opening event) {
        if (pendingDeathText == null) return;
        if (System.currentTimeMillis() >= deathTextUntil) { pendingDeathText = null; return; }
        if (event.getNewScreen() instanceof DeathScreen) {
            Minecraft mc = Minecraft.getInstance();
            boolean hc = mc.level != null && mc.level.getLevelData().isHardcore();
            Component c = pendingDeathText;
            pendingDeathText = null;
            event.setNewScreen(new DeathScreen(c, hc));
        }
    }
}
