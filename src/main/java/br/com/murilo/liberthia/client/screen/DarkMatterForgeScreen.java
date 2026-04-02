package br.com.murilo.liberthia.client.screen;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.menu.DarkMatterForgeMenu;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class DarkMatterForgeScreen extends AbstractContainerScreen<DarkMatterForgeMenu> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(LiberthiaMod.MODID, "textures/gui/dark_matter_forge_gui.png");

    public DarkMatterForgeScreen(DarkMatterForgeMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
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
        RenderSystem.setShaderTexture(0, TEXTURE);

        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        guiGraphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);

        renderFuelIndicator(guiGraphics, x, y);
        renderProgressArrow(guiGraphics, x, y);
    }

    private void renderFuelIndicator(GuiGraphics guiGraphics, int x, int y) {
        if (menu.hasFuel()) {
            int scaled = menu.getScaledFuelProgress();
            // Flame icon drawn from bottom up at fuel slot area
            guiGraphics.blit(TEXTURE, x + 18, y + 35 + 14 - scaled, 176, 14 - scaled, 14, scaled + 1);
        }
    }

    private void renderProgressArrow(GuiGraphics guiGraphics, int x, int y) {
        if (menu.isCrafting()) {
            guiGraphics.blit(TEXTURE, x + 89, y + 33, 176, 14, menu.getScaledProgress(), 17);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
