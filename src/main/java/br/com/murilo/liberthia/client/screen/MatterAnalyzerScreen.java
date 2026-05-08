package br.com.murilo.liberthia.client.screen;

import br.com.murilo.liberthia.matter.MatterContent;
import br.com.murilo.liberthia.matter.MatterContentRegistry;
import br.com.murilo.liberthia.menu.MatterAnalyzerMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * GUI estilo "computador de pesquisa" — mostra leitura de matéria do item
 * inserido com 3 barras (DM/WM/YM), valor de energia equivalente, e a tag de
 * mutação composta com cor.
 */
public class MatterAnalyzerScreen extends AbstractContainerScreen<MatterAnalyzerMenu> {

    public MatterAnalyzerScreen(MatterAnalyzerMenu m, Inventory inv, Component title) {
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

        // Painel
        g.fill(x, y, x + imageWidth, y + imageHeight, 0xFF0E0212);
        g.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, 0xFF161028);
        g.fill(x + 1, y + 1, x + imageWidth - 1, y + 2, 0xFF3B1A5C);

        // Área da máquina
        g.fill(x + 6, y + 16, x + imageWidth - 6, y + 76, 0xFF080018);
        // grid
        for (int gy = y + 18; gy < y + 76; gy += 4)
            g.fill(x + 8, gy, x + imageWidth - 8, gy + 1, 0xFF120428);

        // Slot input
        slot(g, x + 24 - 1, y + 35 - 1, 0xFFAA60FF);

        // "Tela" com barras 3-eixos — mais compacta, deixa espaço pra labels embaixo
        int panelX = x + 60, panelY = y + 18, panelW = 110, panelH = 42;
        g.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0xFF000000);
        g.fill(panelX + 1, panelY + 1, panelX + panelW - 1, panelY + panelH - 1, 0xFF002010);
        // scanline
        for (int sy = panelY + 3; sy < panelY + panelH - 2; sy += 4)
            g.fill(panelX + 2, sy, panelX + panelW - 2, sy + 1, 0xFF003020);

        // 3 barras horizontais (DM, WM, YM) — espaçamento 12 cada
        MatterContent c = menu.currentContent();
        drawMatterRow(g, panelX + 4, panelY + 4,  "DM", c.dark(),   0xFF8B40D8);
        drawMatterRow(g, panelX + 4, panelY + 16, "WM", c.white(),  0xFFE6E6FF);
        drawMatterRow(g, panelX + 4, panelY + 28, "YM", c.yellow(), 0xFFFFD23F);

        // Player inv panel
        g.fill(x + 6, y + 80, x + imageWidth - 6, y + imageHeight - 6, 0xFF120420);
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                slot(g, x + 8 + col * 18 - 1, y + 84 + row * 18 - 1, 0xFF55208A);
        for (int col = 0; col < 9; col++)
            slot(g, x + 8 + col * 18 - 1, y + 142 - 1, 0xFF7F40C8);
    }

    private void drawMatterRow(GuiGraphics g, int x, int y, String label, float value, int color) {
        // label
        g.drawString(this.font, Component.literal(label), x, y, 0xFFCCFF, false);
        // bar
        int bx = x + 18, by = y + 1;
        int bw = 80, bh = 7;
        g.fill(bx - 1, by - 1, bx + bw + 1, by + bh + 1, 0xFF000000);
        g.fill(bx, by, bx + bw, by + bh, 0xFF101020);
        int filled = (int) (bw * Math.min(1f, value / 100f));
        if (filled > 0) {
            g.fill(bx, by, bx + filled, by + bh, color);
            g.fill(bx, by, bx + filled, by + 1, 0xFFFFFFFF); // glow top
        }
        // valor numérico
        String txt = String.format("%.0f", value);
        g.drawString(this.font, Component.literal(txt), bx + bw + 4, y, 0xCCCCCC, false);
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
                Component.translatable("container.liberthia.matter_analyzer")
                        .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD),
                titleLabelX, titleLabelY, 0xFFFFFF, true);
        g.drawString(this.font, this.playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x9966CC, false);

        // Linha de status: nome do item, mutação, energia — tudo abaixo da tela.
        // Tela vai até y=60 (panelY 18 + 42). Inv label fica em y=72. Tem
        // espaço de y=62..70 pra status (uma linha de 9px de fonte).
        ItemStack input = menu.getInputStack();
        if (input.isEmpty()) {
            g.drawString(this.font, Component.literal("[ Insira uma amostra ]")
                    .withStyle(ChatFormatting.DARK_GRAY), 60, 62, 0xFFFFFF, false);
        } else {
            MatterContent c = menu.currentContent();
            MatterContent.Mutation mut = c.dominantMutation();
            String name = input.getHoverName().getString();
            // Linha 1 (esquerda do slot) — nome curto do item
            g.drawString(this.font, Component.literal(truncate(name, 16))
                    .withStyle(ChatFormatting.AQUA), 8, 62, 0xFFFFFF, false);
            // Linha 2 (direita, abaixo da tela) — mutação + energia em UMA linha
            String muText = "» " + mut.displayName;
            g.drawString(this.font, Component.literal(muText).withStyle(mut.color),
                    60, 62, 0xFFFFFF, false);
            int muWidth = this.font.width(muText);
            g.drawString(this.font,
                    Component.literal("  " + c.energyEquivalent() + " FE-eq")
                            .withStyle(ChatFormatting.GOLD),
                    60 + muWidth, 62, 0xFFFFFF, false);
        }
    }

    private static String truncate(String s, int max) {
        return s.length() > max ? s.substring(0, max - 1) + "…" : s;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        super.render(g, mx, my, pt);
        // tooltip da tela: descrição completa da mutação
        int x = (this.width - imageWidth) / 2;
        int y = (this.height - imageHeight) / 2;
        if (mx >= x + 60 && mx < x + 170 && my >= y + 18 && my < y + 74 && !menu.getInputStack().isEmpty()) {
            MatterContent.Mutation mut = menu.currentContent().dominantMutation();
            g.renderComponentTooltip(this.font, java.util.List.of(
                    Component.literal(mut.displayName).withStyle(mut.color, ChatFormatting.BOLD),
                    Component.literal(mut.description).withStyle(ChatFormatting.GRAY)
            ), mx, my);
        }
        renderTooltip(g, mx, my);
    }
}
