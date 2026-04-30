package br.com.murilo.liberthia.client;


import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.particle.ConfigurableSpriteParticle;
import br.com.murilo.liberthia.particle.DarkBloodParticle;
import br.com.murilo.liberthia.registry.ModParticles;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = LiberthiaMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT
)
public final class ClientParticleEvents {

    @SubscribeEvent
    public static void registerParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(
                ModParticles.ENGINE_PARTICLE.get(),
                ConfigurableSpriteParticle.Provider::new
        );

        event.registerSpriteSet(
                ModParticles.DARK_BLOOD.get(),
                DarkBloodParticle.Provider::new
        );
    }

    private ClientParticleEvents() {
    }
}
