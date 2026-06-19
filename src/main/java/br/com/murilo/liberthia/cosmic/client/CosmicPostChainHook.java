package br.com.murilo.liberthia.cosmic.client;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.cosmic.CosmicClientState;
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.EffectInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * v0.1.22 r35: <b>PostChain Hook</b> — carrega/ativa o post-process shader
 * {@code cosmic_horror.json} via reflection no GameRenderer.
 *
 * <h2>Como funciona</h2>
 * Minecraft tem campos privados {@code effect} e {@code effectActive} no
 * GameRenderer. Setamos via reflexão pra ativar nossa PostChain custom quando
 * a phase >= DIMENSIONAL_CORRUPTION, e desativamos quando dormant.
 *
 * <p>Também update uniforms dinâmicos (Aberration, Strength, Time) a cada
 * frame baseado em {@link CosmicClientState}.
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID, value = Dist.CLIENT)
public final class CosmicPostChainHook {

    private static final ResourceLocation SHADER_ID =
            new ResourceLocation(LiberthiaMod.MODID, "shaders/post/cosmic_horror.json");

    private static PostChain loadedChain = null;
    private static boolean wantActive = false;
    private static boolean lastActiveState = false;

    private CosmicPostChainHook() {}

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.gameRenderer == null) return;

        wantActive = CosmicClientState.currentDistortion > 0.05F
                || CosmicClientState.currentChromaticAbb > 0.05F;

        try {
            if (wantActive && !lastActiveState) {
                activatePostChain(mc);
                lastActiveState = true;
            } else if (!wantActive && lastActiveState) {
                deactivatePostChain(mc);
                lastActiveState = false;
            }
            // Update uniforms se ativo
            if (lastActiveState && loadedChain != null) {
                updateUniforms();
            }
        } catch (Throwable t) {
            // Falha gentil — não trava o jogo se o shader não carregar
            LiberthiaMod.LOGGER.debug("[CosmicPostChain] {}", t.toString());
        }
    }

    private static void activatePostChain(Minecraft mc) {
        try {
            // Load via reflection (loadEffect é protected)
            var loadEffect = GameRenderer.class.getDeclaredMethod("loadEffect",
                    ResourceLocation.class);
            loadEffect.setAccessible(true);
            loadEffect.invoke(mc.gameRenderer, new ResourceLocation(
                    LiberthiaMod.MODID, "shaders/post/cosmic_horror.json"));
            // Get effect field
            var effectField = GameRenderer.class.getDeclaredField("postEffect");
            effectField.setAccessible(true);
            loadedChain = (PostChain) effectField.get(mc.gameRenderer);
            LiberthiaMod.LOGGER.info("[CosmicPostChain] activated");
        } catch (Throwable t) {
            LiberthiaMod.LOGGER.warn("[CosmicPostChain] activation failed: {}", t.toString());
        }
    }

    private static void deactivatePostChain(Minecraft mc) {
        try {
            var effectField = GameRenderer.class.getDeclaredField("postEffect");
            effectField.setAccessible(true);
            PostChain current = (PostChain) effectField.get(mc.gameRenderer);
            if (current != null) {
                current.close();
            }
            effectField.set(mc.gameRenderer, null);
            loadedChain = null;
            LiberthiaMod.LOGGER.info("[CosmicPostChain] deactivated");
        } catch (Throwable ignored) {}
    }

    /** Update uniforms dinâmicos das passes a cada tick.
     * Acessa o campo {@code passes} via reflexão (é private em PostChain). */
    private static void updateUniforms() {
        try {
            var passesField = PostChain.class.getDeclaredField("passes");
            passesField.setAccessible(true);
            @SuppressWarnings("unchecked")
            java.util.List<net.minecraft.client.renderer.PostPass> passes =
                    (java.util.List<net.minecraft.client.renderer.PostPass>)
                            passesField.get(loadedChain);
            if (passes == null) return;
            for (var pass : passes) {
                EffectInstance effect = pass.getEffect();
                if (effect == null) continue;
                var aberration = effect.safeGetUniform("Aberration");
                if (aberration != null) aberration.set(CosmicClientState.currentChromaticAbb);
                var strength = effect.safeGetUniform("Strength");
                if (strength != null) strength.set(CosmicClientState.currentDistortion);
                var time = effect.safeGetUniform("Time");
                if (time != null) time.set(CosmicClientState.animTime);
            }
        } catch (Throwable ignored) {}
    }
}
