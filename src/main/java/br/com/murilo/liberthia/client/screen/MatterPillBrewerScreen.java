package br.com.murilo.liberthia.client.screen;

import br.com.murilo.liberthia.menu.MatterPillBrewerMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * v0.1.44: GUI do Matter Pill Brewer — visual estilo alquimia
 * (rosa-pálido + dourado + esmeralda) pra distinguir do Purifier.
 *
 * <p>Layout:
 * <ul>
 *   <li>2 slots de input (ingot + bottle) na esquerda</li>
 *   <li>Arrow no meio (progresso)</li>
 *   <li>1 slot de output na direita (pílulas)</li>
 *   <li>Sem barra de energia (não usa energy)</li>
 * </ul>
 */
public class MatterPillBrewerScreen extends AbstractContainerScreen<MatterPillBrewerMenu> {

    public MatterPillBrewerScreen(MatterPillBrewerMenu m, Inventory inv, Component title) {
        super(m, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.titleLabelY = 6;
        this.inventoryLabelY = 72;
    }

    @Override
    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        int x = (this.width - imageWidth) / 2;
        int y = (this.height - imageHeight) / 2;

        // Painel principal — gradient rose dark → indigo
        g.fill(x, y, x + imageWidth, y + imageHeight, 0xFF1A0612);
        g.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, 0xFF2A0A24);
        // Highlight superior dourado
        g.fill(x + 1, y + 1, x + imageWidth - 1, y + 2, 0xFFFFB060);

        // Área de trabalho (alquimia panel)
        g.fill(x + 6, y + 16, x + imageWidth - 6, y + 78, 0xFF0A0210);
        // Grid sutil rosa
        for (int gy = y + 18; gy < y + 78; gy += 4)
            g.fill(x + 8, gy, x + imageWidth - 8, gy + 1, 0xFF200818);

        // v0.1.49: barra de energia vertical à esquerda (entre x+12 e x+20)
        int barX = x + 12, barY = y + 18, barW = 8, barH = 54;
        g.fill(barX - 1, barY - 1, barX + barW + 1, barY + barH + 1, 0xFF000000);
        g.fill(barX, barY, barX + barW, barY + barH, 0xFF101020);
        int e = menu.getEnergy(), eMax = menu.getMaxEnergy();
        if (eMax > 0 && e > 0) {
            int filled = (int) ((long) barH * e / eMax);
            for (int i = 0; i < filled; i++) {
                float pct = i / (float) barH;
                int color = lerpColor(0xFFFF60C0, 0xFFFFD23F, pct);
                g.fill(barX, barY + barH - 1 - i, barX + barW, barY + barH - i, color);
            }
        }

        // SLOT_INGOT em (38, 35) — borda violet
        slot(g, x + 38 - 1, y + 35 - 1, 0xFFC080FF);
        // SLOT_BOTTLE em (62, 35) — borda cyan (vidro)
        slot(g, x + 62 - 1, y + 35 - 1, 0xFF80E0FF);
        // SLOT_OUTPUT em (122, 35) — borda dourada
        slot(g, x + 122 - 1, y + 35 - 1, 0xFFFFD23F);

        // Arrow de progresso entre BOTTLE e OUTPUT — pixel art
        int arrowX = x + 84, arrowY = y + 36, arrowLen = 32;
        // background
        g.fill(arrowX, arrowY + 5, arrowX + arrowLen, arrowY + 11, 0xFF080018);
        // ponta da arrow
        g.fill(arrowX + arrowLen, arrowY + 4, arrowX + arrowLen + 2, arrowY + 12, 0xFF080018);
        g.fill(arrowX + arrowLen + 2, arrowY + 5, arrowX + arrowLen + 4, arrowY + 11, 0xFF080018);
        g.fill(arrowX + arrowLen + 4, arrowY + 6, arrowX + arrowLen + 6, arrowY + 10, 0xFF080018);

        int prog = menu.getProgress();
        int maxProg = menu.getMaxProgress();
        if (maxProg > 0 && prog > 0) {
            int fillW = (int) ((long) arrowLen * prog / maxProg);
            // gradient rosa → dourado
            for (int i = 0; i < fillW; i++) {
                float pct = i / (float) arrowLen;
                int color = lerpColor(0xFFFF80C0, 0xFFFFD23F, pct);
                g.fill(arrowX + i, arrowY + 5, arrowX + i + 1, arrowY + 11, color);
            }
            // brilho na ponta
            if (fillW > 0) {
                g.fill(arrowX + fillW - 1, arrowY + 4, arrowX + fillW + 1, arrowY + 12, 0xFFFFFFFF);
            }
        }

        // Icone "+" entre ingot e bottle (decorativo)
        int plusX = x + 56, plusY = y + 39;
        g.fill(plusX, plusY + 2, plusX + 6, plusY + 4, 0xFFFFB060);
        g.fill(plusX + 2, plusY, plusX + 4, plusY + 6, 0xFFFFB060);

        // Player inv panel
        g.fill(x + 6, y + 80, x + imageWidth - 6, y + imageHeight - 6, 0xFF080018);
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                slot(g, x + 8 + col * 18 - 1, y + 84 + row * 18 - 1, 0xFF55208A);
        for (int col = 0; col < 9; col++)
            slot(g, x + 8 + col * 18 - 1, y + 142 - 1, 0xFF7F40C8);
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
                Component.translatable("container.liberthia.matter_pill_brewer")
                        .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                titleLabelX, titleLabelY, 0xFFFFFF, true);
        g.drawString(this.font, this.playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x9966CC, false);

        // Info de energia (FE)
        int e = menu.getEnergy(), eMax = menu.getMaxEnergy();
        String estr = String.format("§eFE: §f%d§7/§f%d", e, eMax);
        g.drawString(this.font, Component.literal(estr), 28, 64, 0xFFFFFF, false);

        // Info de progresso
        int prog = menu.getProgress(), maxProg = menu.getMaxProgress();
        if (prog > 0 && maxProg > 0) {
            String p = String.format("§6Brewing: §f%d%%", 100 * prog / maxProg);
            g.drawString(this.font, Component.literal(p), 75, 24, 0xFFFFFF, false);
        } else {
            g.drawString(this.font,
                    Component.literal("§7Ingot + Glass Bottle → §63 Pílulas"),
                    20, 24, 0xFFFFFF, false);
        }
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        super.render(g, mx, my, pt);
        renderTooltip(g, mx, my);
    }
}
