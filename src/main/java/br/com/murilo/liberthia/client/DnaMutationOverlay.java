package br.com.murilo.liberthia.client;

import br.com.murilo.liberthia.config.LiberthiaConfig;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

public class DnaMutationOverlay implements IGuiOverlay {
    public static final DnaMutationOverlay INSTANCE = new DnaMutationOverlay();

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
        int width = 140;
        int rowH = 14;

        RenderSystem.enableBlend();
        guiGraphics.fill(x - 4, y - 6, x + width + 6, y + (rowH * 4) + 8, 0xAA0A0A0A);
        guiGraphics.drawString(gui.getFont(), "DNA Mutation", x, y - 2, 0xFFE0E0E0, false);

        drawInputLike(guiGraphics, gui, x, y + rowH, "Escura", dark, 0xFF7B2CBF);
        drawInputLike(guiGraphics, gui, x, y + (rowH * 2), "Clara", clear, 0xFF4CC9F0);
        drawInputLike(guiGraphics, gui, x, y + (rowH * 3), "Amarela", yellow, 0xFFF4B400);
        RenderSystem.disableBlend();
    }

    private void drawInputLike(GuiGraphics g, ForgeGui gui, int x, int y, String label, int value, int color) {
        int pct = Math.max(0, Math.min(100, (int) Math.round((value / 1000.0D) * 100.0D)));
        String text = label + ": " + pct + "%";
        int boxX1 = x + 44;
        int boxX2 = x + 130;

        g.drawString(gui.getFont(), label, x, y + 3, 0xFFE8E8E8, false);
        g.fill(boxX1, y, boxX2, y + 11, 0xFF111111);
        g.fill(boxX1, y, boxX1 + Math.max(1, (int) ((boxX2 - boxX1) * (pct / 100.0F))), y + 11, color);
        g.drawString(gui.getFont(), text, boxX1 + 3, y + 2, 0xFFFFFFFF, false);
    }
}
