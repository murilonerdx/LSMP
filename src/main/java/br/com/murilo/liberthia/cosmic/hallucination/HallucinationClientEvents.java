package br.com.murilo.liberthia.cosmic.hallucination;

import br.com.murilo.liberthia.LiberthiaMod;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * v0.1.22 r40: Hooks client-side pra hallucinations.
 *
 * <ul>
 *   <li>{@code ClientTickEvent}: tick do {@link HallucinationClientHandler} (decay alpha, remove expired)</li>
 *   <li>{@code RenderGuiOverlayEvent.Post}: desenha overlays globais (glitch burst, death flash)</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID, value = Dist.CLIENT)
public final class HallucinationClientEvents {

    private HallucinationClientEvents() {}

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        try {
            HallucinationClientHandler.clientTick();
        } catch (Throwable t) {
            LiberthiaMod.LOGGER.warn("[Hallucination] client tick error: {}", t.toString());
        }
    }

    /**
     * Overlay POS-render — desenhamos por cima de TUDO (após hotbar/chat).
     * Glitch burst e death flash são fullscreen alpha overlays.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRenderOverlay(RenderGuiOverlayEvent.Post event) {
        if (!event.getOverlay().id().toString().contains("hotbar")) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;

        GuiGraphics g = event.getGuiGraphics();
        int w = g.guiWidth();
        int h = g.guiHeight();

        // 1. Death flash (vermelho fullscreen)
        float deathAlpha = HallucinationClientHandler.deathFlashAlpha;
        if (deathAlpha > 0.01F) {
            int alpha = (int)(deathAlpha * 200);
            int color = (alpha << 24) | 0x8B0000;
            g.fill(0, 0, w, h, color);
            // Texto "YOU DIED" no centro (visual de mock)
            String dt = "§4§l§oYOU DIED";
            int dw = mc.font.width(dt);
            g.drawString(mc.font, dt, (w - dw) / 2, h / 2 - 8,
                    (alpha << 24) | 0xFFFFFF, true);
        }

        // 2. Glitch burst — chromatic + scanlines violentas
        float glitchAlpha = HallucinationClientHandler.glitchBurstAlpha;
        if (glitchAlpha > 0.01F) {
            int alpha = (int)(glitchAlpha * 150);
            // RGB shift bars
            int redA = alpha;
            int blueA = alpha;
            int shift = (int)(8 + glitchAlpha * 16);
            g.fill(shift, 0, shift + 6, h, (redA << 24) | 0xFF0000);
            g.fill(w - shift - 6, 0, w - shift, h, (blueA << 24) | 0x00FFFF);
            // Scan flicker
            int sa = alpha / 2;
            for (int y = 0; y < h; y += 4) {
                g.fill(0, y, w, y + 1, (sa << 24) | 0x550066);
            }
            // Random noise pixels
            java.util.Random rng = new java.util.Random(System.nanoTime());
            int count = (int)(glitchAlpha * 600);
            for (int i = 0; i < count; i++) {
                int x = rng.nextInt(w);
                int y = rng.nextInt(h);
                int gray = rng.nextInt(255);
                int a = 80 + rng.nextInt(120);
                int color = (a << 24) | (gray << 16) | (gray << 8) | gray;
                g.fill(x, y, x + 1, y + 1, color);
            }
        }

        // 3. Reality shake é tratado em CosmicCameraShake (já existente)
        //    HallucinationClientHandler.shakeIntensity é lido lá.
    }
}
