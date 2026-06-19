package br.com.murilo.liberthia.cosmic.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

/**
 * v0.1.22 r41: <b>Dimensional Crack Particle</b> — rachadura preta no ar,
 * cresce ao longo do lifetime e tem chromatic shimmer nas bordas.
 *
 * <h2>Crack growth</h2>
 * <ul>
 *   <li>Stays at SAME WORLD POSITION (não orbita)</li>
 *   <li>Scale curve: 0 → 1.4 (growth 60% of life) → 1.4 (hold 30%) → 0 (collapse 10%)</li>
 *   <li>Quasi-random rotation: roll = age × 0.02 + initialRoll</li>
 *   <li>Alpha: full alpha durante growth, perpetual chromatic flicker</li>
 * </ul>
 *
 * <h2>Chromatic shimmer</h2>
 * Cor RGB oscila numa fase senoidal — r/g/b dessincronizados pra parecer
 * que tem aberração cromática. Simula reality tear visualmente.
 *
 * <h2>Render</h2>
 * TRANSLUCENT pra mesclar com fundo escuro (preto). Em luz alta fica menos
 * visível (intencional — cracks "preferem" escuridão).
 */
public class DimensionalCrackParticle extends TextureSheetParticle {

    private final float initialRoll;
    private final float chromaSpeed;
    private final float maxScale;

    public DimensionalCrackParticle(ClientLevel level, double x, double y, double z,
                                     double vx, double vy, double vz, SpriteSet sprites) {
        super(level, x, y, z);
        this.initialRoll = (float) (level.random.nextDouble() * Math.PI * 2);
        this.chromaSpeed = 0.15F + level.random.nextFloat() * 0.1F;
        // vy = max scale (default 2.0)
        this.maxScale = (float) Math.max(1.0, Math.min(4.0, vy != 0 ? vy : 2.0));

        // Lifetime longer — cracks linger
        this.lifetime = 60 + level.random.nextInt(40);

        // Color: deep void with cyan/magenta chromatic shifts
        this.rCol = 0.05F;
        this.gCol = 0.02F;
        this.bCol = 0.10F;
        this.alpha = 0.0F;
        this.quadSize = 0.01F;
        this.hasPhysics = false;
        this.roll = initialRoll;
        this.oRoll = initialRoll;
        this.setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        this.oRoll = this.roll;
        if (this.age++ >= this.lifetime) {
            this.remove();
            return;
        }

        float t = (float) this.age / this.lifetime;

        // Crack stays at same position (no orbit)
        // Slow rotation — gives a subtle "shifting" look
        this.roll = initialRoll + (float)(this.age * 0.015);

        // Scale curve: 0 → maxScale (60%) → hold (30%) → 0 (10%)
        if (t < 0.6F) {
            this.quadSize = maxScale * (t / 0.6F);
        } else if (t < 0.9F) {
            this.quadSize = maxScale;
        } else {
            this.quadSize = maxScale * (1.0F - (t - 0.9F) / 0.1F);
        }

        // Alpha: ease-in 5%, hold, ease-out last 10%
        if (t < 0.05F) {
            this.alpha = t / 0.05F;
        } else if (t > 0.9F) {
            this.alpha = (1.0F - t) / 0.1F;
        } else {
            // Constant alpha but chromatic flicker subtracts random amount
            this.alpha = 0.85F + 0.15F * (float) Math.sin(this.age * 0.4);
        }

        // Chromatic shimmer — RGB dessincronizado
        float phase = this.age * chromaSpeed;
        this.rCol = 0.05F + 0.20F * (float) Math.abs(Math.sin(phase));
        this.gCol = 0.02F + 0.05F * (float) Math.abs(Math.sin(phase + 2.1));
        this.bCol = 0.10F + 0.40F * (float) Math.abs(Math.sin(phase + 4.2));
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;
        public Provider(SpriteSet sprites) { this.sprites = sprites; }
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                        double x, double y, double z,
                                        double vx, double vy, double vz) {
            return new DimensionalCrackParticle(level, x, y, z, vx, vy, vz, sprites);
        }
    }
}
