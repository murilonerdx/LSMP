package br.com.murilo.liberthia.client.screen;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.menu.DarkMatterAlchemizerMenu;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * Screen for the Dark Matter Alchemizer. Reuses the Matter Transmuter GUI
 * background — same slot layout (input @ 48,17, catalyst @ 48,53, output @ 116,35,
 * progress arrow @ 73,35).
 *
 * Adds a 60% / 40% odds banner so the player knows it's a gamble.
 */
public class DarkMatterAlchemizerScreen extends AbstractContainerScreen<DarkMatterAlchemizerMenu> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(LiberthiaMod.MODID, "textures/gui/matter_transmuter_gui.png");

    public DarkMatterAlchemizerScreen(DarkMatterAlchemizerMenu menu, Inventory playerInventory, Component title) {
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

        // Odds banner above the title — small but obvious.
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;
        Component odds = Component.literal("60% raro · 40% explosão")
                .withStyle(ChatFormatting.GOLD);
        guiGraphics.drawString(this.font, odds,
                x + (imageWidth - this.font.width(odds)) / 2, y - 12, 0xFFD700, false);

        renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
