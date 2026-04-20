package br.com.murilo.liberthia.logic;

import net.minecraft.core.particles.DustParticleOptions;
import org.joml.Vector3f;

/**
 * Shared particle presets for blood-themed blocks.
 * Using red dust instead of DAMAGE_INDICATOR avoids the "heart icon"
 * look that was coming out of the ground.
 */
public final class BloodParticles {
    private BloodParticles() {}

    /** Deep arterial red dust — the default blood droplet. */
    public static final DustParticleOptions BLOOD =
            new DustParticleOptions(new Vector3f(0.55F, 0.02F, 0.02F), 1.2F);

    /** Brighter red dust — for impact/spit highlights. */
    public static final DustParticleOptions BLOOD_BRIGHT =
            new DustParticleOptions(new Vector3f(0.80F, 0.05F, 0.05F), 1.0F);

    /** Darker near-black red — for veins/shadows. */
    public static final DustParticleOptions BLOOD_DARK =
            new DustParticleOptions(new Vector3f(0.28F, 0.01F, 0.01F), 1.3F);
}
