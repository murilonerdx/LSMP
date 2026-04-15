package br.com.murilo.liberthia.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

public class MatterEnergyHudOverlay implements IGuiOverlay {
    public static final MatterEnergyHudOverlay INSTANCE = new MatterEnergyHudOverlay();

    private static final int BAR_WIDTH = 50;
    private static final int BAR_HEIGHT = 5;
    private static final int MAX_ENERGY = 1000;

    @Override
    public void render(ForgeGui gui, GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight) {
        if (br.com.murilo.liberthia.config.DevMode.ACTIVE) return;
        int dark = ClientMatterEnergyState.getDarkEnergy();
        int clear = ClientMatterEnergyState.getClearEnergy();
        int yellow = ClientMatterEnergyState.getYellowEnergy();

        // Only show if player has any energy
        if (dark == 0 && clear == 0 && yellow == 0) return;

        RenderSystem.enableBlend();

        int baseX = 10;
        int baseY = 70;
        int spacing = 8;

        // Dark Energy bar (purple)
        if (dark > 0) {
            renderEnergyBar(guiGraphics, baseX, baseY, dark, 0xFF5A1A78, 0xFF2A0A38, "D");
        }

        // Clear Energy bar (cyan)
        if (clear > 0) {
            renderEnergyBar(guiGraphics, baseX, baseY + spacing, clear, 0xFF00CED1, 0xFF005555, "C");
        }

        // Yellow Energy bar (gold)
        if (yellow > 0) {
            renderEnergyBar(guiGraphics, baseX, baseY + spacing * 2, yellow, 0xFFFFD700, 0xFF665500, "Y");
        }

        // Stabilized indicator
        if (ClientMatterEnergyState.isStabilized()) {
            guiGraphics.drawString(gui.getFont(), "\u2726", baseX + BAR_WIDTH + 14, baseY + spacing - 2, 0xFF00FF00, true);
        }

        RenderSystem.disableBlend();
    }

    private void renderEnergyBar(GuiGraphics guiGraphics, int x, int y, int energy, int fillColor, int bgColor, String label) {
        // Background
        guiGraphics.fill(x + 8, y, x + 8 + BAR_WIDTH, y + BAR_HEIGHT, bgColor);

        // Fill
        int fillWidth = (int) ((float) energy / MAX_ENERGY * BAR_WIDTH);
        guiGraphics.fill(x + 8, y, x + 8 + fillWidth, y + BAR_HEIGHT, fillColor);

        // Border
        guiGraphics.fill(x + 8, y - 1, x + 8 + BAR_WIDTH, y, 0x80FFFFFF);
        guiGraphics.fill(x + 8, y + BAR_HEIGHT, x + 8 + BAR_WIDTH, y + BAR_HEIGHT + 1, 0x80FFFFFF);
    }
}
