package br.com.murilo.liberthia.observation.particle;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.registry.ModParticles;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * v0.1.22 r63: Bootstrap client-side dos providers das particles customizadas.
 *
 * <p>Pattern AN: usa {@link RegisterParticleProvidersEvent#registerSpriteSet}
 * pra bindar o ParticleType ao SpriteSet-aware provider.
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ObservationParticleClient {

    private ObservationParticleClient() {}

    @SubscribeEvent
    public static void onRegister(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.OBSERVATION_GLOW.get(),
                GlowParticle.Provider::new);
        event.registerSpriteSet(ModParticles.OBSERVATION_LINE.get(),
                LineParticle.Provider::new);
        LiberthiaMod.LOGGER.debug("[ObservationParticle] registered GLOW+LINE providers");
    }
}
