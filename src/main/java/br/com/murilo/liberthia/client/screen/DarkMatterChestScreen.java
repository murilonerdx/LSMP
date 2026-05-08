package br.com.murilo.liberthia.client.screen;

import br.com.murilo.liberthia.menu.DarkMatterChestMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** GUI procedural roxa pro Dark Matter Chest (9×6 + inventário). */
public class DarkMatterChestScreen extends AbstractContainerScreen<DarkMatterChestMenu> {

    public DarkMatterChestScreen(DarkMatterChestMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 240;
        this.titleLabelY = 6;
        this.inventoryLabelY = 130;
    }

    @Override
    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        int x = (this.width - imageWidth) / 2;
        int y = (this.height - imageHeight) / 2;

        // Painel principal
        g.fill(x, y, x + imageWidth, y + imageHeight, 0xFF0E0212);
        g.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, 0xFF1B0830);
        g.fill(x + 1, y + 1, x + imageWidth - 1, y + 2, 0xFF3B1A5C);
        g.fill(x + 1, y + 1, x + 2, y + imageHeight - 1, 0xFF3B1A5C);

        // Área dos 54 slots
        g.fill(x + 6, y + 16, x + imageWidth - 6, y + 16 + 110, 0xFF120420);
        for (int row = 0; row < 6; row++)
            for (int col = 0; col < 9; col++)
                slot(g, x + 8 + col * 18 - 1, y + 18 + row * 18 - 1);

        // Painel inferior do inventário
        g.fill(x + 6, y + 138, x + imageWidth - 6, y + imageHeight - 6, 0xFF120420);
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                slot(g, x + 8 + col * 18 - 1, y + 140 + row * 18 - 1);
        for (int col = 0; col < 9; col++)
            slot(g, x + 8 + col * 18 - 1, y + 198 - 1);
    }

    private void slot(GuiGraphics g, int x, int y) {
        g.fill(x, y, x + 18, y + 1, 0xFF2A0D44);
        g.fill(x, y, x + 1, y + 18, 0xFF2A0D44);
        g.fill(x, y + 17, x + 18, y + 18, 0xFF55208A);
        g.fill(x + 17, y, x + 18, y + 18, 0xFF55208A);
        g.fill(x + 1, y + 1, x + 17, y + 17, 0xFF1B0830);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mx, int my) {
        g.drawString(this.font,
                Component.translatable("container.liberthia.dark_matter_chest")
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
