package br.com.murilo.liberthia.particle.engine;

import com.mojang.serialization.Codec;
import net.minecraft.core.particles.ParticleType;
import com.mojang.serialization.Codec;
import net.minecraft.core.particles.ParticleType;

public class ConfigurableParticleType extends ParticleType<ConfigurableParticleOptions> {

    private final Codec<ConfigurableParticleOptions> codec;

    public ConfigurableParticleType(boolean overrideLimiter) {
        super(overrideLimiter, ConfigurableParticleOptions.DESERIALIZER);
        this.codec = ConfigurableParticleOptions.codec(this);
    }

    @Override
    public Codec<ConfigurableParticleOptions> codec() {
        return this.codec;
    }
}
