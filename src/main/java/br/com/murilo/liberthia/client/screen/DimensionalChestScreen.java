package br.com.murilo.liberthia.client.screen;

import br.com.murilo.liberthia.menu.DimensionalChestMenu;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * Tela do Baú Dimensional — usa a textura vanilla do generic_54 (double chest).
 */
public class DimensionalChestScreen extends AbstractContainerScreen<DimensionalChestMenu> {

    private static final ResourceLocation TEX =
            new ResourceLocation("textures/gui/container/generic_54.png");

    public DimensionalChestScreen(DimensionalChestMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageHeight = 222;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        g.blit(TEX, x, y, 0, 0, this.imageWidth, 6 * 18 + 17);
        g.blit(TEX, x, y + 6 * 18 + 17, 0, 126, this.imageWidth, 96);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mx, int my) {
        g.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0x404040, false);
        // Channel readout topo direito
        String chan = "§9⌬ " + this.menu.getChannel();
        int w = this.font.width(chan);
        g.drawString(this.font, chan, this.imageWidth - w - 8, this.titleLabelY, 0x4040AA, false);
        g.drawString(this.font, this.playerInventoryTitle,
                this.inventoryLabelX, this.inventoryLabelY, 0x404040, false);
        g.drawString(this.font, "§7Sneak+click no bloco pra trocar canal",
                8, this.imageHeight - 10, 0x707070, false);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        this.renderBackground(g);
        super.render(g, mx, my, pt);
        this.renderTooltip(g, mx, my);
    }
}
