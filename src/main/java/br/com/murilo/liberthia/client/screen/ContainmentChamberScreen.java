package br.com.murilo.liberthia.client.screen;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.menu.ContainmentChamberMenu;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class ContainmentChamberScreen extends AbstractContainerScreen<ContainmentChamberMenu> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(LiberthiaMod.MODID, "textures/gui/containment_chamber_gui.png");

    public ContainmentChamberScreen(ContainmentChamberMenu menu, Inventory playerInventory, Component title) {
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

        // Progress arrow
        if (menu.isCrafting()) {
            guiGraphics.blit(TEXTURE, x + 79, y + 35, 176, 0, menu.getScaledProgress(), 17);
        }

        // Stability bar (vertical, green->yellow->red)
        int scaledStability = menu.getScaledStability();
        int stability = menu.getStability();

        // Color based on stability level
        int color;
        if (stability > 60) color = 0xFF00CC00;       // Green
        else if (stability > 30) color = 0xFFCCCC00;   // Yellow
        else color = 0xFFCC0000;                        // Red

        // Draw stability bar from bottom up at position (152, 17) height 42
        int barX = x + 152;
        int barBottom = y + 17 + 42;
        guiGraphics.fill(barX, barBottom - scaledStability, barX + 8, barBottom, color);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);

        // Stability tooltip
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;
        if (mouseX >= x + 152 && mouseX <= x + 160 && mouseY >= y + 17 && mouseY <= y + 59) {
            guiGraphics.renderTooltip(this.font, Component.literal("Stability: " + menu.getStability() + "%"), mouseX, mouseY);
        }
    }
}
