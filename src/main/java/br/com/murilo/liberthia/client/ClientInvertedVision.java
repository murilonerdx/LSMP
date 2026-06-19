package br.com.murilo.liberthia.client;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.cosmic.CosmicClientState;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Estado client-side da <b>visão invertida</b> do Mapa Invertido. Usa o
 * post-shader vanilla {@code minecraft:shaders/post/invert.json} (cores
 * negativas) pra dar a sensação de estar vendo "o lado de lá".
 *
 * <p>r166 FIX: antes usava reflexão pelos nomes deobf ({@code loadEffect}/
 * {@code postEffect}), o que <b>falha no jar de produção</b> (runtime SRG) — por
 * isso o efeito não aparecia no jogo. Agora chama os métodos públicos
 * {@code GameRenderer.loadEffect(...)} e {@code shutdownEffect()} diretamente
 * (o Forge remapeia chamadas diretas corretamente, ao contrário de reflexão).
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID, value = Dist.CLIENT)
public final class ClientInvertedVision {

    private static final ResourceLocation INVERT_SHADER =
            new ResourceLocation("minecraft", "shaders/post/invert.json");

    private static int remaining = 0;
    private static boolean loaded = false;

    private ClientInvertedVision() {}

    /** Chamado pelo packet (no main thread do cliente). */
    public static void activate(int durationTicks) {
        remaining = Math.max(remaining, durationTicks);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (remaining <= 0) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.gameRenderer == null) {
            unloadShader(mc);
            remaining = 0;
            return;
        }

        remaining--;
        if (remaining <= 0) {
            unloadShader(mc);
            return;
        }

        // Não brigar com o post-chain do cosmic horror — esse tem prioridade.
        boolean cosmicBusy = CosmicClientState.currentDistortion > 0.05F
                || CosmicClientState.currentChromaticAbb > 0.05F;
        if (!loaded && !cosmicBusy) {
            loadShader(mc);
        }
    }

    private static void loadShader(Minecraft mc) {
        try {
            mc.gameRenderer.loadEffect(INVERT_SHADER);
            loaded = true;
        } catch (Throwable t) {
            LiberthiaMod.LOGGER.debug("[InvertedVision] load failed: {}", t.toString());
            loaded = false;
        }
    }

    private static void unloadShader(Minecraft mc) {
        if (!loaded) return;
        loaded = false;
        // Se o cosmic horror assumiu o slot de post-effect, não mata o shader dele.
        boolean cosmicBusy = CosmicClientState.currentDistortion > 0.05F
                || CosmicClientState.currentChromaticAbb > 0.05F;
        if (cosmicBusy) return;
        try {
            if (mc != null && mc.gameRenderer != null) {
                mc.gameRenderer.shutdownEffect();
            }
        } catch (Throwable ignored) {}
    }
}
