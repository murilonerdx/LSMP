package br.com.murilo.liberthia.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

/**
 * F4: Distorção de tela quando em área de infecção alta (density > 0.5).
 * Efeito: faixas roxas semi-transparentes com offset sin-based.
 */
public class InfectionDistortionOverlay implements IGuiOverlay {

    public static final InfectionDistortionOverlay INSTANCE = new InfectionDistortionOverlay();

    private InfectionDistortionOverlay() {}

    @Override
    public void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int screenWidth, int screenHeight) {
        if (br.com.murilo.liberthia.config.DevMode.ACTIVE) return;

        float density = ClientInfectionState.getChunkDensity();
        if (density <= 0.5f) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        float intensity = (density - 0.5f) * 2.0f; // 0.0 at 0.5, 1.0 at 1.0
        float time = (mc.player.tickCount + partialTick) * 0.05f;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        // Horizontal distortion bands
        int bands = 4 + (int)(intensity * 4);
        int bandHeight = screenHeight / bands;

        for (int i = 0; i < bands; i++) {
            float phase = time + i * 1.3f;
            float offset = (float)Math.sin(phase) * intensity * 8.0f;
            int alpha = (int)(15 + intensity * 30 * (0.5f + 0.5f * (float)Math.sin(phase * 0.7f)));
            alpha = Math.min(alpha, 60);

            int y = i * bandHeight + (int)offset;
            int color = (alpha << 24) | 0x2A0033; // Dark purple with dynamic alpha

            graphics.fill(0, y, screenWidth, y + bandHeight / 2, color);
        }

        // Vignette effect at high intensity
        if (intensity > 0.3f) {
            int vigAlpha = (int)(intensity * 80);
            int vigColor = (vigAlpha << 24) | 0x0D0008;

            // Top/bottom vignette
            int vigHeight = (int)(screenHeight * 0.15f * intensity);
            graphics.fill(0, 0, screenWidth, vigHeight, vigColor);
            graphics.fill(0, screenHeight - vigHeight, screenWidth, screenHeight, vigColor);

            // Left/right vignette
            int vigWidth = (int)(screenWidth * 0.1f * intensity);
            graphics.fill(0, 0, vigWidth, screenHeight, vigColor);
            graphics.fill(screenWidth - vigWidth, 0, screenWidth, screenHeight, vigColor);
        }

        RenderSystem.disableBlend();
    }
}
