package br.com.murilo.liberthia.cosmic.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

/**
 * v0.1.22 r41: <b>Orbital Debris Particle</b> — debris elongado orbitando em
 * múltiplas frequências, simulando rotação binária e tilt 3D.
 *
 * <h2>Spline motion</h2>
 * <ul>
 *   <li><b>Primary orbit:</b> elipse com semi-major axis a, semi-minor b</li>
 *   <li><b>Secondary frequency:</b> wobble vertical 2× a velocidade orbital</li>
 *   <li><b>Tilt:</b> matriz de rotação aplicada ao plano da órbita</li>
 *   <li><b>Trajectory:</b> spline composta de 3 ondas senoidais perpendiculares</li>
 * </ul>
 *
 * <p>Diferente de {@link GravitySingularityParticle}, este particle NÃO
 * espirala pra dentro — ele orbita estavelmente até desaparecer.
 *
 * <h2>Sound-reactive scaling</h2>
 * Scale = base × (1 + 0.15 × sin(age × 0.3)) — pulsa sutilmente como se
 * estivesse vibrando com som cósmico ambiente.
 *
 * <h2>Velocity input</h2>
 * <ul>
 *   <li>vx = semi-major (default 4)</li>
 *   <li>vy = semi-minor (default 2.5)</li>
 *   <li>vz = tilt strength (-1..1)</li>
 * </ul>
 */
public class OrbitalDebrisParticle extends TextureSheetParticle {

    private final double centerX;
    private final double centerY;
    private final double centerZ;
    private final float semiMajor;
    private final float semiMinor;
    private final float tilt;
    private final float startAngle;
    private final float angularSpeed;
    private final float verticalAmp;
    private final float wobblePhase;

    public OrbitalDebrisParticle(ClientLevel level, double x, double y, double z,
                                  double vx, double vy, double vz, SpriteSet sprites) {
        super(level, x, y, z);
        this.centerX = x;
        this.centerY = y;
        this.centerZ = z;
        this.semiMajor = (float) Math.max(2.0, Math.min(8.0, vx != 0 ? vx : 4.0));
        this.semiMinor = (float) Math.max(1.0, Math.min(5.0, vy != 0 ? vy : 2.5));
        this.tilt = (float) Math.max(-1, Math.min(1, vz));
        this.startAngle = (float) (level.random.nextDouble() * Math.PI * 2);
        this.angularSpeed = 0.04F + level.random.nextFloat() * 0.04F;
        this.verticalAmp = 0.5F + level.random.nextFloat() * 1.5F;
        this.wobblePhase = (float) (level.random.nextDouble() * Math.PI * 2);

        this.lifetime = 80 + level.random.nextInt(40);

        // Cor azul-violeta cosmica
        this.rCol = 0.20F + level.random.nextFloat() * 0.15F;
        this.gCol = 0.15F;
        this.bCol = 0.85F + level.random.nextFloat() * 0.15F;
        this.alpha = 0.0F;
        this.quadSize = 0.6F + level.random.nextFloat() * 0.4F;
        this.hasPhysics = false;
        this.setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.age++ >= this.lifetime) {
            this.remove();
            return;
        }

        float t = (float) this.age / this.lifetime;
        float angle = startAngle + (this.age * angularSpeed);

        // Elliptical orbit: x = a×cos(θ), z = b×sin(θ)
        double ex = Math.cos(angle) * semiMajor;
        double ez = Math.sin(angle) * semiMinor;

        // Apply tilt: rotate around Z axis (so y-component appears)
        double tiltedY = ex * tilt * 0.5;

        // Vertical wobble — 2× orbital freq
        double wobble = Math.sin(angle * 2 + wobblePhase) * verticalAmp;

        this.x = centerX + ex;
        this.y = centerY + tiltedY + wobble;
        this.z = centerZ + ez;

        // Alpha fade-in/fade-out
        if (t < 0.15F) {
            this.alpha = t / 0.15F;
        } else if (t > 0.85F) {
            this.alpha = (1.0F - t) / 0.15F;
        } else {
            this.alpha = 1.0F;
        }

        // Sound-reactive scaling — pulsa sutilmente
        this.quadSize = (0.6F + 0.2F * (float) Math.sin(this.age * 0.3))
                * (0.9F + 0.3F * (1.0F - Math.abs(t - 0.5F) * 2));

        // Color modulation — shifts hue slightly over time
        float hueShift = (float) Math.sin(this.age * 0.05) * 0.15F;
        this.rCol = 0.25F + hueShift;
        this.bCol = 0.85F - hueShift * 0.3F;
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
            return new OrbitalDebrisParticle(level, x, y, z, vx, vy, vz, sprites);
        }
    }
}
