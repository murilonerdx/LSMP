package br.com.murilo.liberthia.client;

import br.com.murilo.liberthia.LiberthiaMod;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * r187 — tinge o céu/névoa em direção à cor de Astaron conforme a radiação se aproxima de 100.
 * Roda em PRIORIDADE LOW (depois do CosmicFogController) p/ tingir por cima.
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class AstaronSkyTintHandler {
    private AstaronSkyTintHandler() {}

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onFogColor(ViewportEvent.ComputeFogColor e) {
        float t = AstaronClientState.radiationFraction();
        if (t < 0.01f) return;
        float blend = Math.max(0f, (t - 0.3f) / 0.7f); // visível só acima de 30% radiação
        if (blend <= 0f) return;
        int packed = AstaronClientState.skyColor;
        float tr = ((packed >> 16) & 0xFF) / 255.0f, tg = ((packed >> 8) & 0xFF) / 255.0f, tb = (packed & 0xFF) / 255.0f;
        e.setRed(e.getRed() * (1f - blend) + tr * blend);
        e.setGreen(e.getGreen() * (1f - blend) + tg * blend);
        e.setBlue(e.getBlue() * (1f - blend) + tb * blend);
    }
}
