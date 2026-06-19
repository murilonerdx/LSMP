package br.com.murilo.liberthia.client.screen;

import br.com.murilo.liberthia.matter.MatterContent;
import br.com.murilo.liberthia.menu.MatterTesterMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

/**
 * GUI da Matter Tester — desenhada inteiramente por código (sem PNG): painel escuro
 * com slots de amostra à esquerda, um "reator" central que pulsa durante o scan, um
 * painel de RESULTADO com a cor da mutação + barras DM/WM/YM, e um painel de LOGS à
 * direita com os últimos testes. Ícone indica se há um Computador conectado.
 */
public class MatterTesterScreen extends AbstractContainerScreen<MatterTesterMenu> {

    private static final int W = 256, H = 200;

    // mesma ordem do enum MatterContent.Mutation
    private static final int[] MUT_COLOR = {
            0xFF555555, // NONE
            0xFFAAAAAA, // INERT
            0xFFAA33CC, // WILD (dark purple)
            0xFFEFEFEF, // COGNITIVE (white)
            0xFFFFE23F, // ERRATIC (yellow)
            0xFFE060FF, // SYMBIOTIC (light purple)
            0xFFFFB52E, // STRATEGIST (gold)
            0xFFFF4040, // UNSTABLE (red)
    };
    private static final String[] MUT_NAME = {
            "Nenhuma", "Inerte", "Selvagem", "Cognitiva", "Errática", "Simbiótica", "Estrategista", "Instável"
    };
    private static final String[] MUT_DESC = {
            "Sem traços de matéria.", "Equilíbrio estável.",
            "Matéria escura pura — agressiva, infectante.",
            "Matéria branca pura — apaga memórias, teletransporte.",
            "Matéria amarela pura — alucinações, descontrole.",
            "DM + WM — consciência maligna, vontade própria.",
            "YM + WM — fria, calculista, intensifica vontades.",
            "DM + YM — REPULSÃO. Pode explodir.",
    };

    public MatterTesterScreen(MatterTesterMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = W;
        this.imageHeight = H;
        this.inventoryLabelY = H - 94;
        this.titleLabelY = 6;
        this.titleLabelX = 8;
    }

    @Override
    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        int x = (this.width - imageWidth) / 2;
        int y = (this.height - imageHeight) / 2;

        // painel de fundo (vidro escuro com borda ciano)
        g.fill(x, y, x + W, y + H, 0xFF0A0E14);
        g.fill(x + 1, y + 1, x + W - 1, y + H - 1, 0xFF11161F);
        drawBorder(g, x, y, W, H, 0xFF1E2A38);

        // ── coluna esquerda: amostras A/B + reator ──
        drawSlotFrame(g, x + 30, y + 23, 0xFF6A6AFF);  // A
        drawSlotFrame(g, x + 30, y + 49, 0xFFFF8844);  // B
        g.drawString(font, "A", x + 22, y + 27, 0xFF8888FF, false);
        g.drawString(font, "B", x + 22, y + 53, 0xFFFFAA66, false);

        // reator central (entre as amostras e o resultado)
        int rcx = x + 78, rcy = y + 42;
        drawReactor(g, rcx, rcy, pt);

        // seta de progresso amostras → resultado
        int arrowX = x + 92, arrowY = y + 39, arrowW = 26;
        g.fill(arrowX, arrowY, arrowX + arrowW, arrowY + 6, 0xFF05080C);
        int prog = menu.getScaledProgress(arrowW);
        for (int i = 0; i < prog; i++) {
            int col = lerp(0xFF39C6FF, 0xFFB36BFF, i / (float) arrowW);
            g.fill(arrowX + i, arrowY, arrowX + i + 1, arrowY + 6, col);
        }

        // ── painel de RESULTADO ──
        int px = x + 124, py = y + 18, pw = 122, ph = 56;
        g.fill(px, py, px + pw, py + ph, 0xFF05080C);
        drawBorder(g, px, py, pw, ph, 0xFF243240);

        int mut = menu.mutationId();
        if (mut <= 0) {
            g.drawString(font, "Aguardando 2 amostras...", px + 8, py + 24, 0xFF55626F, false);
        } else {
            int idx = Math.min(mut - 1, MUT_COLOR.length - 1);
            int color = MUT_COLOR[idx];
            // título da mutação (+ selo NOVA)
            g.drawString(font, "§lRESULTADO", px + 6, py + 5, 0xFF7A8A99, false);
            g.drawString(font, MUT_NAME[idx], px + 6, py + 16, color, true);
            if (menu.isNew()) {
                String tag = "✦ NOVA";
                g.drawString(font, tag, px + pw - font.width(tag) - 6, py + 16, 0xFFFFE23F, true);
            }
            // barras DM/WM/YM
            int d = menu.resDark(), w = menu.resWhite(), ye = menu.resYellow();
            int maxv = Math.max(1, Math.max(d, Math.max(w, ye)));
            int barX = px + 6, barW = pw - 12;
            drawStat(g, barX, py + 28, barW, "DM", d, maxv, 0xFFAA33CC);
            drawStat(g, barX, py + 37, barW, "WM", w, maxv, 0xFFEAEAEA);
            drawStat(g, barX, py + 46, barW, "YM", ye, maxv, 0xFFFFE23F);
        }

        // ── ícone de conexão com Computador ──
        boolean linked = menu.computerLinked();
        int icx = x + 124, icy = y + 78;
        g.fill(icx, icy, icx + 122, icy + 10, 0xFF05080C);
        g.drawString(font, linked ? "§a⬛ Computador conectado" : "§8⬛ Sem computador",
                icx + 4, icy + 1, linked ? 0xFF55FF77 : 0xFF556070, false);

        // ── painel de LOGS (abaixo do resultado) ──
        int lx = x + 124, ly = y + 92, lw = 122, lh = imageHeight - 92 - 8;
        // (logs desenhados em renderLabels pra ficar sobre o fundo, ver abaixo)
        g.fill(lx, ly, lx + lw, ly + lh, 0xFF05080C);
        drawBorder(g, lx, ly, lw, lh, 0xFF243240);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mx, int my) {
        // título
        g.drawString(font, Component.translatable("container.liberthia.matter_tester")
                .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD), titleLabelX, titleLabelY, 0xFFFFFF, false);
        g.drawString(font, this.playerInventoryTitle, 8, inventoryLabelY, 0xFF9AA7B4, false);

        // logs (coordenadas relativas ao canto do GUI — renderLabels já está transladado)
        int lx = 124, ly = 92;
        g.drawString(font, "§7§lLOGS", lx + 4, ly + 2, 0xFF7A8A99, false);
        List<String> logs = menu.getBlockEntity().getLogs();
        int row = ly + 13;
        int maxRows = (imageHeight - 92 - 8 - 14) / 9;
        for (int i = 0; i < logs.size() && i < maxRows; i++) {
            String line = trim(logs.get(i), 120);
            g.drawString(font, line, lx + 4, row, 0xFFFFFFFF, false);
            row += 9;
        }
        if (logs.isEmpty()) {
            g.drawString(font, "§8(nenhum teste ainda)", lx + 4, row, 0xFF556070, false);
        }
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        super.render(g, mx, my, pt);

        // tooltip da mutação ao passar o mouse no painel de resultado
        int x = (this.width - imageWidth) / 2, y = (this.height - imageHeight) / 2;
        int mut = menu.mutationId();
        if (mut > 0 && mx >= x + 124 && mx <= x + 246 && my >= y + 18 && my <= y + 74) {
            int idx = Math.min(mut - 1, MUT_DESC.length - 1);
            g.renderTooltip(font, List.of(
                    Component.literal(MUT_NAME[idx]).withStyle(style -> style.withColor(MUT_COLOR[idx] & 0xFFFFFF)),
                    Component.literal(MUT_DESC[idx]).withStyle(ChatFormatting.GRAY)
            ), java.util.Optional.empty(), mx, my);
        }
        renderTooltip(g, mx, my);
    }

    // ── helpers de desenho ──
    private void drawReactor(GuiGraphics g, int cx, int cy, float pt) {
        boolean scanning = menu.isScanning();
        // anel externo
        g.fill(cx - 9, cy - 9, cx + 9, cy + 9, 0xFF05080C);
        drawBorder(g, cx - 9, cy - 9, 18, 18, 0xFF2A3A4A);
        // núcleo pulsante
        long t = System.currentTimeMillis();
        float pulse = scanning ? (float) (0.5 + 0.5 * Math.sin(t / 120.0)) : 0.25F;
        int core = lerp(0xFF1A2630, scanning ? 0xFF39C6FF : 0xFF243240, pulse);
        g.fill(cx - 5, cy - 5, cx + 5, cy + 5, core);
        if (scanning) {
            int spark = lerp(0xFF39C6FF, 0xFFB36BFF, pulse);
            g.fill(cx - 1, cy - 7, cx + 1, cy + 7, spark);
            g.fill(cx - 7, cy - 1, cx + 7, cy + 1, spark);
        }
    }

    private void drawStat(GuiGraphics g, int x, int y, int w, String label, int v, int maxv, int color) {
        g.drawString(font, label, x, y, 0xFF8A97A4, false);
        int bx = x + 20, bw = w - 44;
        g.fill(bx, y, bx + bw, y + 6, 0xFF11161F);
        int fill = (int) (bw * Math.min(1f, v / (float) maxv));
        g.fill(bx, y, bx + fill, y + 6, color);
        String num = String.valueOf(v);
        g.drawString(font, num, x + w - font.width(num), y, 0xFFB0BCC8, false);
    }

    private void drawSlotFrame(GuiGraphics g, int x, int y, int color) {
        // moldura 18x18 estilo slot vanilla, com borda colorida
        g.fill(x - 1, y - 1, x + 17, y + 17, color);
        g.fill(x, y, x + 16, y + 16, 0xFF0A0E14);
    }

    private void drawBorder(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + 1, color);
        g.fill(x, y + h - 1, x + w, y + h, color);
        g.fill(x, y, x + 1, y + h, color);
        g.fill(x + w - 1, y, x + w, y + h, color);
    }

    private static int lerp(int a, int b, float t) {
        int aa = (a >> 24) & 0xFF, ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
        int ba = (b >> 24) & 0xFF, br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
        int ca = (int) (aa + (ba - aa) * t), cr = (int) (ar + (br - ar) * t);
        int cg = (int) (ag + (bg - ag) * t), cb = (int) (ab + (bb - ab) * t);
        return (ca << 24) | (cr << 16) | (cg << 8) | cb;
    }

    private static String trim(String s, int maxPx) {
        return s; // logs já são curtos; mantém códigos de cor
    }
}
