package br.com.murilo.liberthia.client.screen;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.menu.MatterTransmuterMenu;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class MatterTransmuterScreen extends AbstractContainerScreen<MatterTransmuterMenu> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(LiberthiaMod.MODID, "textures/gui/matter_transmuter_gui.png");

    public MatterTransmuterScreen(MatterTransmuterMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void init() {
        super.init();
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);
        if (menu.isCrafting()) {
            guiGraphics.blit(TEXTURE, x + 73, y + 35, 176, 0, menu.getScaledProgress(), 17);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
