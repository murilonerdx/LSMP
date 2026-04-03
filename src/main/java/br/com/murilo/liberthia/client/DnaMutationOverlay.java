package br.com.murilo.liberthia.client;

import br.com.murilo.liberthia.config.LiberthiaConfig;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

public class DnaMutationOverlay implements IGuiOverlay {
    public static final DnaMutationOverlay INSTANCE = new DnaMutationOverlay();

    private static final float SCALE = 0.75f;

    @Override
    public void render(ForgeGui gui, GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight) {
        int dark = ClientMatterEnergyState.getDarkEnergy();
        int clear = ClientMatterEnergyState.getClearEnergy();
        int yellow = ClientMatterEnergyState.getYellowEnergy();

        if (dark <= 0 && clear <= 0 && yellow <= 0) {
            return;
        }

        int x = LiberthiaConfig.CLIENT.dnaX.get();
        int y = LiberthiaConfig.CLIENT.dnaY.get();
        int width = 105;
        int rowH = 10;

        RenderSystem.enableBlend();

        // Compact background panel
        guiGraphics.fill(x - 3, y - 4, x + width + 3, y + (rowH * 3) + 6, 0xAA0A0A0A);

        // Title at reduced scale
        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(SCALE, SCALE, 1.0f);
        int sx = (int) (x / SCALE);
        int sy = (int) ((y - 2) / SCALE);
        guiGraphics.drawString(gui.getFont(), "Exposure", sx, sy, 0xFFE0E0E0, false);
        guiGraphics.pose().popPose();

        // Compact energy bars
        drawCompactBar(guiGraphics, gui, x, y + rowH, "Esc", dark, 0xFF7B2CBF);
        drawCompactBar(guiGraphics, gui, x, y + (rowH * 2), "Cla", clear, 0xFF4CC9F0);
        drawCompactBar(guiGraphics, gui, x, y + (rowH * 3), "Ama", yellow, 0xFFF4B400);

        RenderSystem.disableBlend();
    }

    private void drawCompactBar(GuiGraphics g, ForgeGui gui, int x, int y, String label, int value, int color) {
        int pct = Math.max(0, Math.min(100, (int) Math.round((value / 1000.0D) * 100.0D)));
        int barX1 = x + 24;
        int barX2 = x + 100;
        int barHeight = 8;

        // Background bar
        g.fill(barX1, y, barX2, y + barHeight, 0xFF111111);

        // Filled portion
        int fillWidth = Math.max(1, (int) ((barX2 - barX1) * (pct / 100.0F)));
        g.fill(barX1, y, barX1 + fillWidth, y + barHeight, color);

        // Label and percentage at reduced scale
        guiGraphics_drawScaled(g, gui, label, x, y + 1);
        guiGraphics_drawScaled(g, gui, pct + "%", barX1 + 2, y + 1);
    }

    private void guiGraphics_drawScaled(GuiGraphics g, ForgeGui gui, String text, int x, int y) {
        g.pose().pushPose();
        g.pose().scale(SCALE, SCALE, 1.0f);
        g.drawString(gui.getFont(), text, (int) (x / SCALE), (int) (y / SCALE), 0xFFFFFFFF, false);
        g.pose().popPose();
    }
}
