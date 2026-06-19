package br.com.murilo.liberthia.magic.spell.particle;

import com.mojang.serialization.Codec;
import net.minecraft.core.particles.ParticleType;

/**
 * v0.1.145 r113: ParticleType pro SpellTrail. Original — boilerplate Forge.
 */
public class SpellTrailParticleType extends ParticleType<SpellTrailParticleData> {

    public SpellTrailParticleType() {
        super(false, SpellTrailParticleData.DESERIALIZER);
    }

    @Override
    public Codec<SpellTrailParticleData> codec() {
        return SpellTrailParticleData.CODEC;
    }
}
