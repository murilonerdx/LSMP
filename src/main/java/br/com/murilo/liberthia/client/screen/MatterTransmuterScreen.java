package br.com.murilo.liberthia.client.screen;

import br.com.murilo.liberthia.menu.MatterTransmuterMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * v0.1.52: GUI redesenhada do Matter Transmuter — antes era PNG estático
 * confuso. Agora desenho procedural com tema "transmutação cíclica":
 *
 * <pre>
 *     Ciclo:  DM ─→ CM ─→ YM ─→ DM ...
 *
 *     [IN]   ──→  [arrow]  ──→   [OUT]
 *
 *             [CAT] (catalisador define direção)
 * </pre>
 *
 * <p>Topo: visualização do ciclo de transmutação (3 bolas DM/CM/YM com setas).
 * Centro: INPUT à esquerda, arrow de progresso, OUTPUT à direita.
 * Embaixo: CATALYST que determina pra qual direção o ciclo gira.
 *
 * <p>Paleta: violeta/cyan/dourado pra os 3 matters + magenta pulsante no arrow
 * durante crafting.
 */
public class MatterTransmuterScreen extends AbstractContainerScreen<MatterTransmuterMenu> {

    public MatterTransmuterScreen(MatterTransmuterMenu menu, Inventory inv, Component title) {
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

        // Painel principal — gradient violeta-índigo
        g.fill(x, y, x + imageWidth, y + imageHeight, 0xFF0A0118);
        g.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, 0xFF180826);
        // Highlight top
        g.fill(x + 1, y + 1, x + imageWidth - 1, y + 2, 0xFF7040C0);

        // ── Visualização do ciclo no topo (DM → CM → YM → DM) ──
        // Layout: 3 esferas em linha horizontal + setas indicando o ciclo
        // Cada esfera é um pixel-art ~6x6
        int cycY = y + 14;
        drawOrb(g, x + 30, cycY, 0xFFAA60FF, 0xFFE0C0FF);  // DM (violeta)
        drawOrb(g, x + 84, cycY, 0xFFB0E8FF, 0xFFFFFFFF);  // CM (branco)
        drawOrb(g, x + 138, cycY, 0xFFFFD23F, 0xFFFFF080); // YM (dourado)
        // Setas conectando: → → ↻
        drawSmallArrow(g, x + 40, cycY + 2, x + 78, 0xFFC080FF);   // DM → CM
        drawSmallArrow(g, x + 94, cycY + 2, x + 132, 0xFFE0DC80);  // CM → YM

        // Painel central (working area)
        g.fill(x + 6, y + 26, x + imageWidth - 6, y + 78, 0xFF050010);
        for (int gy = y + 28; gy < y + 78; gy += 4)
            g.fill(x + 8, gy, x + imageWidth - 8, gy + 1, 0xFF140626);

        // ── SLOTS ──
        // INPUT (48, 35) — borda violeta
        slot(g, x + 48 - 1, y + 35 - 1, 0xFFAA60FF);
        // CATALYST (48, 53) — borda magenta
        slot(g, x + 48 - 1, y + 53 - 1, 0xFFFF60C0);
        // OUTPUT (116, 35) — borda dourada
        slot(g, x + 116 - 1, y + 35 - 1, 0xFFFFD23F);

        // ── Linha catalyst → arrow (indica que o catalyst alimenta a transmutação) ──
        for (int dy = 0; dy < 12; dy++) {
            g.fill(x + 57, y + 45 + dy, x + 58, y + 46 + dy, 0xFFFF60C0);
        }

        // ── Arrow central INPUT → OUTPUT ──
        int arrowX = x + 68, arrowY = y + 36, arrowLen = 44;
        // outline + background
        g.fill(arrowX, arrowY + 4, arrowX + arrowLen, arrowY + 5, 0xFF55208A);
        g.fill(arrowX, arrowY + 5, arrowX + arrowLen, arrowY + 11, 0xFF0A0220);
        g.fill(arrowX, arrowY + 11, arrowX + arrowLen, arrowY + 12, 0xFF55208A);
        // ponta
        g.fill(arrowX + arrowLen, arrowY + 4, arrowX + arrowLen + 2, arrowY + 12, 0xFF0A0220);
        g.fill(arrowX + arrowLen + 2, arrowY + 5, arrowX + arrowLen + 4, arrowY + 11, 0xFF0A0220);
        g.fill(arrowX + arrowLen + 4, arrowY + 6, arrowX + arrowLen + 6, arrowY + 10, 0xFF0A0220);

        if (menu.isCrafting()) {
            int fill = menu.getScaledProgress() * arrowLen / 24;
            for (int i = 0; i < fill; i++) {
                float pct = i / (float) arrowLen;
                int color = lerpColor(0xFFAA60FF, 0xFFFFD23F, pct);
                g.fill(arrowX + i, arrowY + 5, arrowX + i + 1, arrowY + 11, color);
            }
            if (fill > 0) {
                g.fill(arrowX + fill - 1, arrowY + 4, arrowX + fill + 1, arrowY + 12, 0xFFFFFFFF);
            }
        }

        // ── Player inv ──
        g.fill(x + 6, y + 80, x + imageWidth - 6, y + imageHeight - 6, 0xFF080018);
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                slot(g, x + 8 + col * 18 - 1, y + 84 + row * 18 - 1, 0xFF55208A);
        for (int col = 0; col < 9; col++)
            slot(g, x + 8 + col * 18 - 1, y + 142 - 1, 0xFF7F40C8);
    }

    /** Orbe pixel-art de matter (6x6 com gradient suave). */
    private static void drawOrb(GuiGraphics g, int cx, int cy, int color, int highlight) {
        int r = 3;
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -r; dy <= r; dy++) {
                int d2 = dx * dx + dy * dy;
                if (d2 > r * r) continue;
                int c = (d2 <= 1) ? highlight : color;
                g.fill(cx + dx, cy + dy, cx + dx + 1, cy + dy + 1, c);
            }
        }
    }

    private static void drawSmallArrow(GuiGraphics g, int x1, int y, int x2, int color) {
        g.fill(x1, y, x2, y + 1, color);
        // ponta
        g.fill(x2 - 1, y - 1, x2, y + 2, color);
        g.fill(x2 - 2, y - 2, x2, y + 3, color);
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
                Component.translatable("container.liberthia.matter_transmuter")
                        .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD),
                titleLabelX, titleLabelY, 0xFFFFFF, true);
        g.drawString(this.font, this.playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x9966CC, false);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        super.render(g, mx, my, pt);
        renderTooltip(g, mx, my);
    }
}
