package br.com.murilo.liberthia.loom.client;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.registry.ModEffects;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Random;

/**
 * v0.1.22 r33: <b>GLITCH OVERLAY</b> — efeito visual de "tela quebrada" pra
 * players com Obsession ou Madness.
 *
 * <p>Renderiza barras coloridas aleatórias horizontais + chromatic aberration
 * pseudo via cores transparentes em layers, simulando glitch real.
 *
 * <p>Activated when player has {@link ModEffects#OBSESSION} OR
 * {@link ModEffects#MADNESS} OR {@link ModEffects#DIMENSIONAL_INFECTION}.
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID, value = Dist.CLIENT)
public final class GlitchOverlayHandler {

    private static final Random RANDOM = new Random();
    private static long lastFrameNanos = 0;

    private GlitchOverlayHandler() {}

    @SubscribeEvent
    public static void onRenderGui(RenderGuiOverlayEvent.Post event) {
        if (!event.getOverlay().id().toString().contains("hotbar")) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (mc.options.hideGui) return;

        boolean obs = mc.player.hasEffect(ModEffects.OBSESSION.get());
        boolean mad = mc.player.hasEffect(ModEffects.MADNESS.get());
        boolean dim = mc.player.hasEffect(ModEffects.DIMENSIONAL_INFECTION.get());
        if (!obs && !mad && !dim) return;

        int intensity = (obs ? 1 : 0) + (mad ? 2 : 0) + (dim ? 1 : 0);
        renderGlitch(event.getGuiGraphics(), intensity);
    }

    private static void renderGlitch(GuiGraphics g, int intensity) {
        int screenW = g.guiWidth();
        int screenH = g.guiHeight();
        // Use system time pra animar — não depende de game time
        long now = System.nanoTime() / 1_000_000;
        if (now - lastFrameNanos < 16) return; // 60fps
        lastFrameNanos = now;

        RANDOM.setSeed(now / 100); // muda padrão a cada 100ms

        // Horizontal bars aleatórias
        int barCount = 2 + intensity * 3;
        for (int i = 0; i < barCount; i++) {
            int y = RANDOM.nextInt(screenH);
            int h = 1 + RANDOM.nextInt(4 + intensity * 2);
            int colorChannel = RANDOM.nextInt(3);
            int alpha = 80 + RANDOM.nextInt(120);
            int color;
            switch (colorChannel) {
                case 0  -> color = (alpha << 24) | 0xFF0000; // red
                case 1  -> color = (alpha << 24) | 0x00FF00; // green
                default -> color = (alpha << 24) | 0x0099FF; // cyan-blue
            }
            // Bar shift horizontally by random px
            int shiftX = RANDOM.nextInt(10) - 5;
            g.fill(shiftX, y, screenW + shiftX, y + h, color);
        }

        // Chromatic aberration nas bordas (intensity 2+)
        if (intensity >= 2) {
            int alpha = 50;
            g.fill(0, 0, 4, screenH, (alpha << 24) | 0xFF0000);
            g.fill(screenW - 4, 0, screenW, screenH, (alpha << 24) | 0x00FFFF);
        }

        // Static noise dots (random pixels) — apenas em intensity 3+
        if (intensity >= 3) {
            for (int i = 0; i < 200; i++) {
                int x = RANDOM.nextInt(screenW);
                int y = RANDOM.nextInt(screenH);
                int gray = RANDOM.nextInt(256);
                int alpha = 60 + RANDOM.nextInt(100);
                g.fill(x, y, x + 1, y + 1, (alpha << 24) | (gray << 16) | (gray << 8) | gray);
            }
        }
    }
}
