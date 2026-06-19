package br.com.murilo.liberthia.observation.particle;

import com.mojang.serialization.Codec;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;

/**
 * v0.1.22 r63: <b>ParticleType</b> pro GlowData. Pattern de AN's GlowParticleType.
 *
 * <p>Em 1.20.1: ParticleType abstract com getDeserializer() + codec().
 * (1.20.5+ usa streamCodec() em vez de getDeserializer().)
 */
public class GlowParticleType extends ParticleType<GlowData> {

    public GlowParticleType() {
        super(false, GlowData.DESERIALIZER);
    }

    @Override
    public Codec<GlowData> codec() {
        return GlowData.CODEC;
    }
}
