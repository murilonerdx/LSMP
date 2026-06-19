package br.com.murilo.liberthia.observation.particle;

import com.mojang.serialization.Codec;
import net.minecraft.core.particles.ParticleType;

public class LineParticleType extends ParticleType<LineData> {
    public LineParticleType() { super(false, LineData.DESERIALIZER); }
    @Override public Codec<LineData> codec() { return LineData.CODEC; }
}
