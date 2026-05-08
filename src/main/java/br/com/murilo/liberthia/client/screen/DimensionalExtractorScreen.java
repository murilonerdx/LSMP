package br.com.murilo.liberthia.client.screen;

import br.com.murilo.liberthia.menu.DimensionalExtractorMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

public class DimensionalExtractorScreen extends AbstractContainerScreen<DimensionalExtractorMenu> {

    public DimensionalExtractorScreen(DimensionalExtractorMenu m, Inventory inv, Component title) {
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

        g.fill(x + 6, y + 16, x + imageWidth - 6, y + 76, 0xFF120420);

        // Slots
        slot(g, x + 56 - 1, y + 35 - 1, 0xFF55208A);
        slot(g, x + 116 - 1, y + 35 - 1, 0xFFC060FF);

        // Progress arrow do bucket → output
        int ax = x + 80, ay = y + 38, aw = 30, ah = 12;
        g.fill(ax, ay, ax + aw, ay + ah, 0xFF1A0830);
        float pf = menu.progressFrac();
        int filled = (int) (aw * pf);
        for (int i = 0; i < filled; i++) {
            float t = i / (float) Math.max(1, aw);
            int r = (int) (60 + 180 * t);
            int b = (int) (140 + 110 * t);
            g.fill(ax + i, ay + 2, ax + i + 1, ay + ah - 2, 0xFF000000 | (r << 16) | (40 << 8) | b);
        }
        for (int i = 0; i < 4; i++)
            g.fill(ax + aw + i, ay + 4 - i, ax + aw + i + 1, ay + ah - 4 + i, 0xFF8B40D8);

        // player inv
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
        g.drawString(this.font,
                Component.translatable("container.liberthia.dimensional_extractor")
                        .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD),
                titleLabelX, titleLabelY, 0xFFFFFF, true);
        g.drawString(this.font, this.playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x9966CC, false);

        // Status: fontes próximas + rift
        String src = "Fontes: " + menu.nearbyCount();
        String rift = menu.riftDistance() > 0
                ? "Rift: " + menu.riftDistance() + "m"
                : "Sem rift próximo";
        g.drawString(this.font, Component.literal(src).withStyle(ChatFormatting.LIGHT_PURPLE),
                8, 60, 0xFFFFFF, false);
        g.drawString(this.font, Component.literal(rift).withStyle(
                        menu.riftDistance() > 0 && menu.riftDistance() <= 32
                                ? ChatFormatting.GOLD : ChatFormatting.GRAY),
                72, 60, 0xFFFFFF, false);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        super.render(g, mx, my, pt);
        int x = (this.width - imageWidth) / 2;
        int y = (this.height - imageHeight) / 2;
        if (mx >= x + 80 && mx < x + 110 && my >= y + 38 && my < y + 50) {
            int pct = (int) (menu.progressFrac() * 100);
            g.renderComponentTooltip(this.font, List.of(
                    Component.literal("Extraindo " + pct + "%").withStyle(ChatFormatting.LIGHT_PURPLE),
                    Component.literal(menu.progress() + " / " + menu.progressMax())
                            .withStyle(ChatFormatting.GRAY)
            ), mx, my);
        }
        renderTooltip(g, mx, my);
    }
}
