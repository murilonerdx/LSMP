package br.com.murilo.liberthia.client.screen;

import br.com.murilo.liberthia.menu.MatterInfuserMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * v0.1.52: GUI redesenhada do Matter Infuser — antes era um PNG estático
 * com layout estranho. Agora desenho 100% procedural com layout em triângulo:
 *
 * <pre>
 *   [DM]──╮
 *   [CM]──┼──→ [CAT] ══►══ [OUT]
 *   [YM]──╯
 * </pre>
 *
 * <p>3 ingots de matter convergem por linhas em direção ao slot catalisador,
 * que dispara energia rumo ao output. Paleta cyber-magenta (violeta + cyan +
 * dourado) pra dar sensação de alquimia avançada.
 */
public class MatterInfuserScreen extends AbstractContainerScreen<MatterInfuserMenu> {

    public MatterInfuserScreen(MatterInfuserMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.titleLabelY = 6;
        this.inventoryLabelY = 72;
    }

    @Override
    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        int x = (this.width - imageWidth) / 2;
        int y = (this.height - imageHeight) / 2;

        // Painel principal — gradient indigo profundo
        g.fill(x, y, x + imageWidth, y + imageHeight, 0xFF0A0220);
        g.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, 0xFF160830);
        // Highlight top dourado-violeta
        g.fill(x + 1, y + 1, x + imageWidth - 1, y + 2, 0xFF8060FF);

        // Área de processamento (header)
        g.fill(x + 6, y + 16, x + imageWidth - 6, y + 78, 0xFF050010);
        // Grid técnico sutil
        for (int gy = y + 18; gy < y + 78; gy += 4)
            g.fill(x + 8, gy, x + imageWidth - 8, gy + 1, 0xFF140626);

        // ── SLOTS ──
        // DM (24, 17) — borda violeta
        slot(g, x + 24 - 1, y + 17 - 1, 0xFFAA60FF);
        // CM (24, 35) — borda cyan
        slot(g, x + 24 - 1, y + 35 - 1, 0xFF80E0FF);
        // YM (24, 53) — borda dourada
        slot(g, x + 24 - 1, y + 53 - 1, 0xFFFFD23F);
        // CATALYST (62, 35) — borda magenta (centro pulsante)
        slot(g, x + 62 - 1, y + 35 - 1, 0xFFFF60C0);
        // OUTPUT (122, 35) — borda branco/dourado (resultado)
        slot(g, x + 122 - 1, y + 35 - 1, 0xFFFFFFEE);

        // ── LINHAS CONVERGENTES (DM/CM/YM → CAT) ──
        // Cores específicas por linha pra leitura clara
        drawConvergeLine(g, x + 42, y + 25, x + 61, y + 43, 0xFFAA60FF); // DM → CAT
        drawConvergeLine(g, x + 42, y + 43, x + 61, y + 43, 0xFF80E0FF); // CM → CAT (horizontal)
        drawConvergeLine(g, x + 42, y + 61, x + 61, y + 43, 0xFFFFD23F); // YM → CAT

        // ── ARROW CENTRAL (CAT → OUT) — barra de progresso ──
        int arrowX = x + 82, arrowY = y + 36, arrowLen = 38;
        // background da arrow
        g.fill(arrowX, arrowY + 5, arrowX + arrowLen, arrowY + 11, 0xFF0A0220);
        // outline
        g.fill(arrowX, arrowY + 4, arrowX + arrowLen, arrowY + 5, 0xFF55208A);
        g.fill(arrowX, arrowY + 11, arrowX + arrowLen, arrowY + 12, 0xFF55208A);
        // ponta da arrow
        g.fill(arrowX + arrowLen, arrowY + 4, arrowX + arrowLen + 2, arrowY + 12, 0xFF0A0220);
        g.fill(arrowX + arrowLen + 2, arrowY + 5, arrowX + arrowLen + 4, arrowY + 11, 0xFF0A0220);
        g.fill(arrowX + arrowLen + 4, arrowY + 6, arrowX + arrowLen + 6, arrowY + 10, 0xFF0A0220);

        if (menu.isCrafting()) {
            int fill = menu.getScaledProgress() * arrowLen / 24;
            for (int i = 0; i < fill; i++) {
                float pct = i / (float) arrowLen;
                int color = lerpColor(0xFFFF60C0, 0xFFFFD23F, pct);
                g.fill(arrowX + i, arrowY + 5, arrowX + i + 1, arrowY + 11, color);
            }
            if (fill > 0) {
                g.fill(arrowX + fill - 1, arrowY + 4, arrowX + fill + 1, arrowY + 12, 0xFFFFFFFF);
            }
        }

        // ── Label CATALYST embaixo do slot ──
        // (já tem ícone do slot, label é só decoração se quiser; mantém minimal)

        // ── Player inventory panel ──
        g.fill(x + 6, y + 80, x + imageWidth - 6, y + imageHeight - 6, 0xFF080018);
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                slot(g, x + 8 + col * 18 - 1, y + 84 + row * 18 - 1, 0xFF55208A);
        for (int col = 0; col < 9; col++)
            slot(g, x + 8 + col * 18 - 1, y + 142 - 1, 0xFF7F40C8);
    }

    /** Linha pixel-art entre 2 pontos (Bresenham simplificado). */
    private static void drawConvergeLine(GuiGraphics g, int x1, int y1, int x2, int y2, int color) {
        int dx = Math.abs(x2 - x1), dy = Math.abs(y2 - y1);
        int sx = x1 < x2 ? 1 : -1, sy = y1 < y2 ? 1 : -1;
        int err = dx - dy;
        int x = x1, y = y1;
        while (true) {
            g.fill(x, y, x + 1, y + 1, color);
            if (x == x2 && y == y2) break;
            int e2 = err * 2;
            if (e2 > -dy) { err -= dy; x += sx; }
            if (e2 < dx)  { err += dx; y += sy; }
        }
    }

    private static int lerpColor(int c1, int c2, float t) {
        int a1 = (c1 >> 24) & 0xFF, r1 = (c1 >> 16) & 0xFF, g1 = (c1 >> 8) & 0xFF, b1 = c1 & 0xFF;
        int a2 = (c2 >> 24) & 0xFF, r2 = (c2 >> 16) & 0xFF, g2 = (c2 >> 8) & 0xFF, b2 = c2 & 0xFF;
        int a = (int) (a1 + (a2 - a1) * t);
        int r = (int) (r1 + (r2 - r1) * t);
        int gC = (int) (g1 + (g2 - g1) * t);
        int b = (int) (b1 + (b2 - b1) * t);
        return (a << 24) | (r << 16) | (gC << 8) | b;
    }

    private void slot(GuiGraphics g, int x, int y, int hi) {
        g.fill(x, y, x + 18, y + 1, 0xFF2A0D44);
        g.fill(x, y, x + 1, y + 18, 0xFF2A0D44);
        g.fill(x, y + 17, x + 18, y + 18, hi);
        g.fill(x + 17, y, x + 18, y + 18, hi);
        g.fill(x + 1, y + 1, x + 17, y + 17, 0xFF080018);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mx, int my) {
        g.drawString(this.font,
                Component.translatable("container.liberthia.matter_infuser")
                        .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD),
                titleLabelX, titleLabelY, 0xFFFFFF, true);
        g.drawString(this.font, this.playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x9966CC, false);

        // Hint do catalyst — texto pequenininho sob o slot
        g.drawString(this.font,
                Component.literal("§dCAT"),
                64, 56, 0xFFFFFF, false);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        super.render(g, mx, my, pt);
        renderTooltip(g, mx, my);
    }
}
