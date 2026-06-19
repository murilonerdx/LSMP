package br.com.murilo.liberthia.cosmic;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.cosmic.particle.CollapseShockwaveParticle;
import br.com.murilo.liberthia.cosmic.particle.CosmicOrbitParticle;
import br.com.murilo.liberthia.cosmic.particle.DimensionalCrackParticle;
import br.com.murilo.liberthia.cosmic.particle.GravitySingularityParticle;
import br.com.murilo.liberthia.cosmic.particle.OrbitalDebrisParticle;
import br.com.murilo.liberthia.cosmic.particle.RiftSparkParticle;
import br.com.murilo.liberthia.cosmic.particle.VoidLeakParticle;
import br.com.murilo.liberthia.registry.ModParticles;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * v0.1.22 r35: registra providers client-side dos 3 custom particles cosmicos.
 *
 * <p>Particle TYPES (server+client) registrados em {@link ModParticles}.
 * Aqui só vinculamos providers client.
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID, value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.MOD)
public final class CosmicParticleRegistry {

    private CosmicParticleRegistry() {}

    @SubscribeEvent
    public static void onRegisterParticles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.COSMIC_ORBIT.get(),
                CosmicOrbitParticle.Provider::new);
        event.registerSpriteSet(ModParticles.VOID_LEAK.get(),
                VoidLeakParticle.Provider::new);
        event.registerSpriteSet(ModParticles.RIFT_SPARK.get(),
                RiftSparkParticle.Provider::new);

        // r41: Cosmic Collapse AAA particles
        event.registerSpriteSet(ModParticles.GRAVITY_SINGULARITY.get(),
                GravitySingularityParticle.Provider::new);
        event.registerSpriteSet(ModParticles.ORBITAL_DEBRIS.get(),
                OrbitalDebrisParticle.Provider::new);
        event.registerSpriteSet(ModParticles.DIMENSIONAL_CRACK.get(),
                DimensionalCrackParticle.Provider::new);
        event.registerSpriteSet(ModParticles.COLLAPSE_SHOCKWAVE.get(),
                CollapseShockwaveParticle.Provider::new);

        // r42: Custom Spell Sprites — 10 animated particles
        event.registerSpriteSet(ModParticles.SPELL_FIRE_BLAST.get(),
                br.com.murilo.liberthia.magic.custom.AnimatedSpellParticle.Provider::new);
        event.registerSpriteSet(ModParticles.SPELL_ICE_LANCE.get(),
                br.com.murilo.liberthia.magic.custom.AnimatedSpellParticle.Provider::new);
        event.registerSpriteSet(ModParticles.SPELL_VOID_ORB.get(),
                br.com.murilo.liberthia.magic.custom.AnimatedSpellParticle.Provider::new);
        event.registerSpriteSet(ModParticles.SPELL_LIGHT_RAY.get(),
                br.com.murilo.liberthia.magic.custom.AnimatedSpellParticle.Provider::new);
        event.registerSpriteSet(ModParticles.SPELL_BLOOD_SHOT.get(),
                br.com.murilo.liberthia.magic.custom.AnimatedSpellParticle.Provider::new);
        event.registerSpriteSet(ModParticles.SPELL_ARCANE_MISSILE.get(),
                br.com.murilo.liberthia.magic.custom.AnimatedSpellParticle.Provider::new);
        event.registerSpriteSet(ModParticles.SPELL_EARTH_SPIKE.get(),
                br.com.murilo.liberthia.magic.custom.AnimatedSpellParticle.Provider::new);
        event.registerSpriteSet(ModParticles.SPELL_LIGHTNING_BOLT.get(),
                br.com.murilo.liberthia.magic.custom.AnimatedSpellParticle.Provider::new);
        event.registerSpriteSet(ModParticles.SPELL_SHADOW_DART.get(),
                br.com.murilo.liberthia.magic.custom.AnimatedSpellParticle.Provider::new);
        event.registerSpriteSet(ModParticles.SPELL_NATURE_THORN.get(),
                br.com.murilo.liberthia.magic.custom.AnimatedSpellParticle.Provider::new);

        // r45: Cosmic Horror v2 — 6 partículas animadas
        event.registerSpriteSet(ModParticles.TENTACLE_WRITHE.get(),
                br.com.murilo.liberthia.magic.custom.AnimatedSpellParticle.Provider::new);
        event.registerSpriteSet(ModParticles.VULTO_SHADOW.get(),
                br.com.murilo.liberthia.magic.custom.AnimatedSpellParticle.Provider::new);
        event.registerSpriteSet(ModParticles.GLARING_EYE_PULSE.get(),
                br.com.murilo.liberthia.magic.custom.AnimatedSpellParticle.Provider::new);
        event.registerSpriteSet(ModParticles.CURSED_PULSE.get(),
                br.com.murilo.liberthia.magic.custom.AnimatedSpellParticle.Provider::new);
        event.registerSpriteSet(ModParticles.WHISPER_WISP.get(),
                br.com.murilo.liberthia.magic.custom.AnimatedSpellParticle.Provider::new);
        event.registerSpriteSet(ModParticles.DEMON_GLYPH.get(),
                br.com.murilo.liberthia.magic.custom.AnimatedSpellParticle.Provider::new);

        // r178: Manifestação do Vazio — olho voador + fumaça-sombra (movimento custom)
        event.registerSpriteSet(ModParticles.VOID_FLYING_EYE.get(),
                br.com.murilo.liberthia.cosmic.particle.VoidFlyingEyeParticle.Provider::new);
        event.registerSpriteSet(ModParticles.VOID_SHADOW_WISP.get(),
                br.com.murilo.liberthia.cosmic.particle.VoidShadowWispParticle.Provider::new);
    }
}
