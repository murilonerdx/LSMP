package br.com.murilo.liberthia.client.screen;

import br.com.murilo.liberthia.menu.MatterPurifierMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * GUI do Matter Purifier — visual moderno violeta-cyan com:
 * - Slot de input (esquerda) com indicador do tipo de matter detectado
 * - Barra de progresso central (arrow estilo)
 * - Slot de output (direita) — só mostra resultado
 * - Barra de energia vertical à esquerda do input
 *
 * Cores principais: violeta profundo (0x1A0828), branco-cyan (0x80E0FF),
 * dourado (0xFFD23F) — pra dar feeling de purificação alquímica.
 */
public class MatterPurifierScreen extends AbstractContainerScreen<MatterPurifierMenu> {

    public MatterPurifierScreen(MatterPurifierMenu m, Inventory inv, Component title) {
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

        // Painel principal — gradient violeta dark
        g.fill(x, y, x + imageWidth, y + imageHeight, 0xFF0A0118);
        g.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, 0xFF1A0828);
        // Highlight superior
        g.fill(x + 1, y + 1, x + imageWidth - 1, y + 2, 0xFF6020A0);

        // Área da máquina (header)
        g.fill(x + 6, y + 16, x + imageWidth - 6, y + 78, 0xFF050010);
        // Grid sutil
        for (int gy = y + 18; gy < y + 78; gy += 4)
            g.fill(x + 8, gy, x + imageWidth - 8, gy + 1, 0xFF100425);

        // Barra de energia vertical à esquerda (entre x+10 e x+18, altura y+18 a y+72)
        int barX = x + 10, barY = y + 18, barW = 8, barH = 54;
        g.fill(barX - 1, barY - 1, barX + barW + 1, barY + barH + 1, 0xFF000000);
        g.fill(barX, barY, barX + barW, barY + barH, 0xFF101020);
        int e = menu.getEnergy();
        int eMax = menu.getMaxEnergy();
        if (eMax > 0 && e > 0) {
            int filled = (int) ((long) barH * e / eMax);
            // gradient cyan-violeta
            for (int i = 0; i < filled; i++) {
                float pct = i / (float) barH;
                int r = (int) (0x40 + pct * 0x80);
                int gC = (int) (0x80 + pct * 0x40);
                int b = (int) (0xFF);
                int color = 0xFF000000 | (r << 16) | (gC << 8) | b;
                g.fill(barX, barY + barH - 1 - i, barX + barW, barY + barH - i, color);
            }
        }

        // Slot input (44, 35) — borda violeta clara
        slot(g, x + 44 - 1, y + 35 - 1, 0xFFAA60FF);
        // Slot output (116, 35) — borda dourada
        slot(g, x + 116 - 1, y + 35 - 1, 0xFFFFD23F);

        // Arrow de progresso entre os slots — pixel art moderna
        int arrowX = x + 66, arrowY = y + 36, arrowLen = 48;
        // background da arrow (vazio)
        g.fill(arrowX, arrowY + 5, arrowX + arrowLen, arrowY + 11, 0xFF080018);
        // ponta da arrow
        g.fill(arrowX + arrowLen, arrowY + 4, arrowX + arrowLen + 2, arrowY + 12, 0xFF080018);
        g.fill(arrowX + arrowLen + 2, arrowY + 5, arrowX + arrowLen + 4, arrowY + 11, 0xFF080018);
        g.fill(arrowX + arrowLen + 4, arrowY + 6, arrowX + arrowLen + 6, arrowY + 10, 0xFF080018);

        int prog = menu.getProgress();
        int maxProg = menu.getMaxProgress();
        if (maxProg > 0 && prog > 0) {
            int fillW = (int) ((long) arrowLen * prog / maxProg);
            // gradient azul-violeta animado
            for (int i = 0; i < fillW; i++) {
                float pct = i / (float) arrowLen;
                int color = lerpColor(0xFF60A0FF, 0xFFE080FF, pct);
                g.fill(arrowX + i, arrowY + 5, arrowX + i + 1, arrowY + 11, color);
            }
            // brilho na ponta da fill (animação)
            if (fillW > 0) {
                g.fill(arrowX + fillW - 1, arrowY + 4, arrowX + fillW + 1, arrowY + 12, 0xFFFFFFFF);
            }
        }

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
                Component.translatable("container.liberthia.matter_purifier")
                        .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD),
                titleLabelX, titleLabelY, 0xFFFFFF, true);
        g.drawString(this.font, this.playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x9966CC, false);

        // Info de energia + progresso
        String eStr = String.format("§b%d§7 / §b%d FE", menu.getEnergy(), menu.getMaxEnergy());
        g.drawString(this.font, Component.literal(eStr),
                25, 64, 0xFFFFFF, false);
        int prog = menu.getProgress(), maxProg = menu.getMaxProgress();
        if (prog > 0 && maxProg > 0) {
            String p = String.format("§dPurificando: §f%d%%", 100 * prog / maxProg);
            g.drawString(this.font, Component.literal(p), 75, 24, 0xFFFFFF, false);
        }
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        super.render(g, mx, my, pt);
        renderTooltip(g, mx, my);
    }
}
