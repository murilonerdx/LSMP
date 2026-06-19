package br.com.murilo.liberthia.magic.custom;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

/**
 * v0.1.22 r42: <b>Animated Spell Particle</b> — partícula que cicla pelos
 * 4 frames do sprite sheet ao longo da sua vida.
 *
 * <h2>Animation</h2>
 * <ul>
 *   <li>4 frames distribuídos uniformemente no lifetime</li>
 *   <li>{@link #setSpriteFromAge(SpriteSet)} é chamado cada tick — escolhe
 *       o sprite baseado na idade</li>
 *   <li>Lifetime 20-30 ticks (1-1.5s) — partículas SOMEM rápido pra deixar
 *       a trail dinâmica</li>
 *   <li>Alpha curve: fade in 20%, full 60%, fade out 20%</li>
 *   <li>Scale curve: 0.8 → 1.3 (meio) → 0.6 (final)</li>
 *   <li>Rotação procedural: roll = age × 0.1 — pode ser desabilitado por sprite</li>
 * </ul>
 *
 * <h2>Render type</h2>
 * TRANSLUCENT — usa alpha channel das texturas; permite emissive look se
 * a textura é brilhante.
 *
 * <h2>Sound-reactive scaling (futuro)</h2>
 * Pode interpolar scale com {@code CosmicClientState.currentScreenShake}
 * pra reagir ao horror — desabilitado por padrão.
 */
public class AnimatedSpellParticle extends TextureSheetParticle {

    private final SpriteSet sprites;
    private final boolean rotates;
    /** Initial random rotation offset. */
    private final float rotOffset;

    public AnimatedSpellParticle(ClientLevel level, double x, double y, double z,
                                  double vx, double vy, double vz,
                                  SpriteSet sprites, boolean rotates) {
        super(level, x, y, z, vx * 0.3, vy * 0.3, vz * 0.3);
        this.sprites = sprites;
        this.rotates = rotates;
        this.rotOffset = (float)(level.random.nextDouble() * Math.PI * 2);

        this.lifetime = 20 + level.random.nextInt(10);
        this.gravity = 0;
        this.friction = 0.95F;
        this.alpha = 0;
        this.quadSize = 0.8F + level.random.nextFloat() * 0.3F;
        this.hasPhysics = false;

        // Apply initial velocity scaling — particles drift but not too far
        this.xd = vx * 0.2;
        this.yd = vy * 0.2;
        this.zd = vz * 0.2;

        if (rotates) {
            this.roll = rotOffset;
            this.oRoll = rotOffset;
        }
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

        // Spritesheet animation — escolhe frame pela idade
        this.setSpriteFromAge(sprites);

        // Procedural motion: slight drift
        this.x += this.xd;
        this.y += this.yd;
        this.z += this.zd;
        this.yd += 0.005; // very slight rise

        float t = (float) this.age / this.lifetime;

        // Alpha: fade-in (0-20%) → full (20-80%) → fade-out (80-100%)
        if (t < 0.2F) this.alpha = t / 0.2F;
        else if (t > 0.8F) this.alpha = (1.0F - t) / 0.2F;
        else this.alpha = 1.0F;

        // Scale curve: 0.8 → 1.3 (mid) → 0.6
        if (t < 0.5F) this.quadSize = 0.8F + t * 1.0F;       // 0.8 → 1.3
        else this.quadSize = 1.3F - (t - 0.5F) * 1.4F;       // 1.3 → 0.6

        // Roll
        if (rotates) {
            this.roll = rotOffset + this.age * 0.1F;
        }
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;
        private final boolean rotates;
        public Provider(SpriteSet sprites) { this(sprites, true); }
        public Provider(SpriteSet sprites, boolean rotates) {
            this.sprites = sprites;
            this.rotates = rotates;
        }
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                        double x, double y, double z,
                                        double vx, double vy, double vz) {
            return new AnimatedSpellParticle(level, x, y, z, vx, vy, vz, sprites, rotates);
        }
    }
}
