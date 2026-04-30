package br.com.murilo.liberthia.registry;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.particle.engine.ConfigurableParticleType;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModParticles {

    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, LiberthiaMod.MODID);

    public static final RegistryObject<SimpleParticleType> DARK_BLOOD =
            PARTICLE_TYPES.register("dark_blood", () -> new SimpleParticleType(false));


    public static final RegistryObject<ConfigurableParticleType> ENGINE_PARTICLE =
            PARTICLE_TYPES.register("engine_particle", () -> new ConfigurableParticleType(false));

    private ModParticles() {
    }
}