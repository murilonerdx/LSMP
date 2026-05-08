package br.com.murilo.liberthia.client.screen;

import br.com.murilo.liberthia.menu.DarkMatterBatteryMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

public class DarkMatterBatteryScreen extends AbstractContainerScreen<DarkMatterBatteryMenu> {

    public DarkMatterBatteryScreen(DarkMatterBatteryMenu m, Inventory inv, Component title) {
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
        g.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, 0xFF1B0830);
        g.fill(x + 1, y + 1, x + imageWidth - 1, y + 2, 0xFF3B1A5C);

        // Área machine (top)
        g.fill(x + 6, y + 16, x + imageWidth - 6, y + 76, 0xFF120420);

        // Barra de energia GRANDE no centro
        int bx = x + 30, by = y + 22, bw = 116, bh = 30;
        g.fill(bx - 2, by - 2, bx + bw + 2, by + bh + 2, 0xFF000000);
        g.fill(bx - 1, by - 1, bx + bw + 1, by + bh + 1, 0xFF3B1A5C);
        g.fill(bx, by, bx + bw, by + bh, 0xFF080012);

        // Marcas de escala
        for (int t = 1; t < 4; t++) {
            int tx = bx + (bw * t) / 4;
            g.fill(tx, by, tx + 1, by + 4, 0xFF6A2C9E);
            g.fill(tx, by + bh - 4, tx + 1, by + bh, 0xFF6A2C9E);
        }

        // Preenchimento da barra
        float frac = menu.energyFrac();
        int filled = (int) (bw * frac);
        if (filled > 0) {
            for (int i = 0; i < filled; i++) {
                float t = i / (float) Math.max(1, bw);
                int r = (int) (90 + 165 * t);
                int gC = (int) (10 + 50 * t);
                int b = (int) (160 + 95 * t);
                g.fill(bx + i, by, bx + i + 1, by + bh, 0xFF000000 | (r << 16) | (gC << 8) | b);
            }
            // Brilho no topo
            g.fill(bx, by, bx + filled, by + 1, 0xFFFFCCFF);
        }

        // Player inv panel
        g.fill(x + 6, y + 80, x + imageWidth - 6, y + imageHeight - 6, 0xFF120420);
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                slot(g, x + 8 + col * 18 - 1, y + 84 + row * 18 - 1, 0xFF55208A);
        for (int col = 0; col < 9; col++)
            slot(g, x + 8 + col * 18 - 1, y + 142 - 1, 0xFF7F40C8);
    }

    private void slot(GuiGraphics g, int x, int y, int hi) {
        g.fill(x, y, x + 18, y + 1, 0xFF2A0D44);
        g.fill(x, y, x + 1, y + 18, 0xFF2A0D44);
        g.fill(x, y + 17, x + 18, y + 18, hi);
        g.fill(x + 17, y, x + 18, y + 18, hi);
        g.fill(x + 1, y + 1, x + 17, y + 17, 0xFF1B0830);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mx, int my) {
        // Título: nome do tier
        String tierName = switch (menu.tier()) {
            case BASIC    -> "Bateria Básica";
            case ADVANCED -> "Bateria Avançada";
            case QUANTUM  -> "Bateria Quântica";
        };
        g.drawString(this.font,
                Component.literal(tierName)
                        .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD),
                titleLabelX, titleLabelY, 0xFFFFFF, true);
        g.drawString(this.font, this.playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x9966CC, false);

        // Valores numéricos abaixo da barra
        int e = menu.rawEnergy();
        int max = menu.rawEnergyMax();
        int pct = max == 0 ? 0 : (int) (e * 100L / max);
        String line1 = String.format("%s / %s FE", formatFE(e), formatFE(max));
        String line2 = pct + "%";
        // Centralizado
        int line1Width = this.font.width(line1);
        int line2Width = this.font.width(line2);
        g.drawString(this.font,
                Component.literal(line1).withStyle(ChatFormatting.LIGHT_PURPLE),
                (imageWidth - line1Width) / 2, 56, 0xFFFFFF, false);
        g.drawString(this.font,
                Component.literal(line2).withStyle(ChatFormatting.GOLD),
                (imageWidth - line2Width) / 2, 64, 0xFFFFFF, false);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        super.render(g, mx, my, pt);
        // Tooltip detalhado na barra
        int x = (this.width - imageWidth) / 2;
        int y = (this.height - imageHeight) / 2;
        int bx = x + 30, by = y + 22, bw = 116, bh = 30;
        if (mx >= bx && mx < bx + bw && my >= by && my < by + bh) {
            g.renderComponentTooltip(this.font, List.of(
                    Component.literal(String.format("%,d FE", menu.rawEnergy()))
                            .withStyle(ChatFormatting.LIGHT_PURPLE),
                    Component.literal(String.format("Cap: %,d FE", menu.rawEnergyMax()))
                            .withStyle(ChatFormatting.GRAY),
                    Component.literal(String.format("Transfer: %,d FE/tick",
                                    menu.tier().transfer))
                            .withStyle(ChatFormatting.GOLD)
            ), mx, my);
        }
        renderTooltip(g, mx, my);
    }

    private static String formatFE(int v) {
        if (v < 1000) return Integer.toString(v);
        if (v < 1_000_000) return String.format("%.1fk", v / 1000.0);
        if (v < 1_000_000_000) return String.format("%.2fM", v / 1_000_000.0);
        return String.format("%.2fG", v / 1_000_000_000.0);
    }
}
