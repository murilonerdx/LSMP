package br.com.murilo.liberthia.cosmic.client;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.cosmic.CosmicClientState;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Random;

/**
 * v0.1.22 r35: <b>Screen Overlay client</b> — chromatic aberration via
 * triple-pass RGB shift + vignette escuro + scanlines pulsating.
 *
 * <p>Implementação 100% via {@link GuiGraphics#fill} mas com cores
 * cuidadosamente blendadas pra simular shader real. Não usa fragment shader
 * direto (ver {@link CosmicPostChainHook} pra integração PostChain).
 *
 * <h2>Componentes do overlay</h2>
 * <ol>
 *   <li><b>Vignette escuro</b>: bordas radiais escuras (intensity-driven)</li>
 *   <li><b>Chromatic aberration</b>: 3 layers RGB shifted</li>
 *   <li><b>Scanlines roxas</b>: linhas horizontais pulsantes</li>
 *   <li><b>Static noise</b>: random pixels nas phases altas</li>
 *   <li><b>Edge glow</b>: bordas pulsando cor da phase</li>
 * </ol>
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID, value = Dist.CLIENT)
public final class CosmicScreenOverlay {

    private static final Random RNG = new Random();
    private static long animFrame = 0;

    private CosmicScreenOverlay() {}

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        // Atualiza interp client-side
        CosmicClientState.tickInterpolation();
        animFrame++;
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onRenderOverlay(RenderGuiOverlayEvent.Post event) {
        // Render once por frame (apenas no hotbar overlay pra evitar duplicação)
        if (!event.getOverlay().id().toString().contains("hotbar")) return;
        if (!CosmicClientState.isActive()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;

        GuiGraphics g = event.getGuiGraphics();
        int w = g.guiWidth();
        int h = g.guiHeight();
        float chroma = CosmicClientState.currentChromaticAbb;
        float distort = CosmicClientState.currentDistortion;
        float skyAlpha = CosmicClientState.currentSkyRiftAlpha;
        float intensity = Math.max(chroma, distort);

        // 1. VIGNETTE escuro nas bordas
        renderVignette(g, w, h, intensity);

        // 2. CHROMATIC ABERRATION (RGB split nas bordas)
        if (chroma > 0.01F) {
            renderChromaticAberration(g, w, h, chroma);
        }

        // 3. SCANLINES roxas pulsantes
        if (intensity > 0.1F) {
            renderScanlines(g, w, h, intensity);
        }

        // 4. STATIC NOISE em phases altas
        if (distort > 0.3F) {
            renderStaticNoise(g, w, h, distort);
        }

        // 5. EDGE GLOW pulsando (sky rift visible at edges)
        if (skyAlpha > 0.2F) {
            renderEdgeGlow(g, w, h, skyAlpha);
        }
    }

    private static void renderVignette(GuiGraphics g, int w, int h, float intensity) {
        // 4 quads degradê preto nas bordas
        int alphaMax = (int) (intensity * 180);
        int steps = 20;
        int stepW = w / 2 / steps;
        int stepH = h / 2 / steps;
        for (int i = 0; i < steps; i++) {
            int a = alphaMax * (steps - i) / steps;
            int color = (a << 24);
            // top
            g.fill(0, i * stepH / 2, w, (i + 1) * stepH / 2, color);
            // bottom
            g.fill(0, h - (i + 1) * stepH / 2, w, h - i * stepH / 2, color);
            // left
            g.fill(i * stepW / 2, 0, (i + 1) * stepW / 2, h, color);
            // right
            g.fill(w - (i + 1) * stepW / 2, 0, w - i * stepW / 2, h, color);
        }
    }

    private static void renderChromaticAberration(GuiGraphics g, int w, int h, float strength) {
        int shift = (int) (3 + strength * 8);
        // Red shifted RIGHT, Blue shifted LEFT — simulates RGB lens dispersion
        int alphaR = (int) (40 * strength);
        int alphaB = (int) (40 * strength);
        // Vertical bars on each side
        g.fill(shift, 0, shift + 4, h, (alphaR << 24) | 0xFF0000);
        g.fill(w - shift - 4, 0, w - shift, h, (alphaB << 24) | 0x00FFFF);
        // Horizontal too
        g.fill(0, shift, w, shift + 2, (alphaR << 24) | 0xFF3300);
        g.fill(0, h - shift - 2, w, h - shift, (alphaB << 24) | 0x0033FF);
    }

    private static void renderScanlines(GuiGraphics g, int w, int h, float intensity) {
        int alpha = (int) (30 + intensity * 60);
        // Pulsing line position (varies with animFrame)
        for (int y = 0; y < h; y += 3) {
            // Subtle line a cada 3px
            int lineAlpha = alpha / 2;
            g.fill(0, y, w, y + 1, (lineAlpha << 24) | 0x441080);
        }
        // Plus uma SCAN LINE bem visível que se move
        long lineY = (animFrame * 2) % h;
        int color = (alpha << 24) | 0x9933FF;
        g.fill(0, (int) lineY, w, (int) lineY + 3, color);
    }

    private static void renderStaticNoise(GuiGraphics g, int w, int h, float intensity) {
        RNG.setSeed(animFrame / 2); // changes every 100ms
        int count = (int) (intensity * 400);
        for (int i = 0; i < count; i++) {
            int x = RNG.nextInt(w);
            int y = RNG.nextInt(h);
            int grayLevel = RNG.nextInt(255);
            int alpha = 80 + RNG.nextInt(120);
            int color = (alpha << 24) | (grayLevel << 16) | (grayLevel << 8) | grayLevel;
            g.fill(x, y, x + 1, y + 1, color);
        }
    }

    private static void renderEdgeGlow(GuiGraphics g, int w, int h, float alpha) {
        // Pulse purple glow on edges sync with anim
        float pulse = (float) (0.5 + Math.sin(animFrame * 0.1) * 0.5);
        int a = (int) (alpha * pulse * 90);
        int color = (a << 24) | 0x6E2DA0;
        // Top + bottom thick band
        g.fill(0, 0, w, 8, color);
        g.fill(0, h - 8, w, h, color);
        g.fill(0, 0, 8, h, color);
        g.fill(w - 8, 0, w, h, color);
    }
}
