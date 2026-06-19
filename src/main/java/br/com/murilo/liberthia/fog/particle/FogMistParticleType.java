package br.com.murilo.liberthia.fog.particle;

import com.mojang.serialization.Codec;
import net.minecraft.core.particles.ParticleType;

/**
 * ParticleType pro {@link FogMistData}. Mesmo pattern do {@code GlowParticleType}.
 */
public class FogMistParticleType extends ParticleType<FogMistData> {

    public FogMistParticleType() {
        super(false, FogMistData.DESERIALIZER);
    }

    @Override
    public Codec<FogMistData> codec() {
        return FogMistData.CODEC;
    }
}
