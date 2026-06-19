package br.com.murilo.liberthia.client.screen;

import br.com.murilo.liberthia.menu.RiftForgeMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * r185 — tela da Forja de Fendas (procedural, tema roxo/void). 6×2 entradas, seta de progresso,
 * slot de saída dourado.
 */
public class RiftForgeScreen extends AbstractContainerScreen<RiftForgeMenu> {

    public RiftForgeScreen(RiftForgeMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 220;
        this.imageHeight = 200;
        this.inventoryLabelY = this.imageHeight - 92;
        this.titleLabelX = 8;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos, y = this.topPos;
        // fundo
        g.fill(x, y, x + imageWidth, y + imageHeight, 0xFF120A1E);
        g.fill(x, y, x + imageWidth, y + 1, 0xFFAA00FF);
        g.fill(x, y + imageHeight - 1, x + imageWidth, y + imageHeight, 0xFF5A1090);

        // slots de entrada 6×2 (alinhados com os slots reais do menu)
        for (int row = 0; row < 2; row++)
            for (int col = 0; col < 6; col++)
                slot(g, x + 17 + col * 18, y + 21 + row * 18, 0xFF2A1840);
        // saída (dourado)
        slot(g, x + 189, y + 30, 0xFFB8860B);

        // seta de progresso
        int pmax = menu.getMaxProgress();
        int prog = pmax > 0 ? menu.getProgress() * 22 / pmax : 0;
        g.fill(x + 150, y + 32, x + 150 + 22, y + 36, 0xFF3A2A55);
        g.fill(x + 150, y + 32, x + 150 + prog, y + 36, 0xFFC060FF);

        // fundo do inventário (alinhado com os slots reais)
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                slot(g, x + 30 + col * 18, y + 120 + row * 18, 0xFF2A1840);
        for (int col = 0; col < 9; col++)
            slot(g, x + 30 + col * 18, y + 178, 0xFF2A1840);
    }

    private void slot(GuiGraphics g, int x, int y, int color) {
        g.fill(x - 1, y - 1, x + 17, y + 17, 0xFF000000);
        g.fill(x, y, x + 16, y + 16, color);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(this.font, "§5Forja de Fendas", this.titleLabelX, 6, 0xFFFFFF, false);
        g.drawString(this.font, this.playerInventoryTitle, 30, this.inventoryLabelY, 0xFFAAAAAA, false);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);
        super.render(g, mouseX, mouseY, partialTick);
        this.renderTooltip(g, mouseX, mouseY);
    }
}
