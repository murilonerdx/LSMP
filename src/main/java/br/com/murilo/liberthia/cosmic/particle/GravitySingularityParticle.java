package br.com.murilo.liberthia.cosmic.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

/**
 * v0.1.22 r41: <b>Gravity Singularity Particle</b> — partícula que orbita
 * em torno de um ponto E ESPIRALA pra dentro, simulando lensing gravitacional.
 *
 * <h2>Motion model</h2>
 * <ul>
 *   <li><b>Polar coords:</b> r(t) = r0 × (1 - t)<sup>0.8</sup> — encolhe não-linear</li>
 *   <li><b>Angular velocity:</b> ω(t) = ω0 / r(t) — acelera ao aproximar (Kepler)</li>
 *   <li><b>Vertical drift:</b> y(t) = y0 + sin(ω×t × 3) × radius — wobble</li>
 *   <li><b>Tilt plane:</b> órbita não-axial — eixo aleatório por partícula</li>
 * </ul>
 *
 * <h2>Visual</h2>
 * <ul>
 *   <li><b>Color shift:</b> partícula aproxima → vai do roxo escuro pro
 *       branco-quente (energy compression)</li>
 *   <li><b>Scale curve:</b> 1.0 → 1.6 (estiramento por lensing) → 0.0 (consumed)</li>
 *   <li><b>Alpha:</b> 0 → 1 (fade-in 10%) → 1 (90%) → 0 (consumed flash)</li>
 *   <li><b>Render:</b> additive (PARTICLE_SHEET_LIT pra emissão)</li>
 * </ul>
 *
 * <p>Spawn velocity é IGNORADA — usa-se {@code vy} como flag pra raio inicial
 * e {@code vx, vz} como tilt do plano orbital. Server passa params via
 * sendParticles(x, y, z, count, vx=tiltX, vy=radius, vz=tiltZ, speed=0).
 */
public class GravitySingularityParticle extends TextureSheetParticle {

    private final double centerX;
    private final double centerY;
    private final double centerZ;
    private final float initialRadius;
    private final float startAngle;
    private final float baseAngularSpeed;
    private final float tiltAxisX;
    private final float tiltAxisZ;
    private final float wobblePhase;

    public GravitySingularityParticle(ClientLevel level, double x, double y, double z,
                                       double vx, double vy, double vz, SpriteSet sprites) {
        super(level, x, y, z);
        this.centerX = x;
        this.centerY = y;
        this.centerZ = z;
        // Parse params: vy = initial radius (1.5-6 range)
        this.initialRadius = (float) Math.max(1.5, Math.min(6, vy != 0 ? vy : 3.0));
        // vx, vz = tilt axis (range -1..1)
        this.tiltAxisX = (float) Math.max(-1, Math.min(1, vx));
        this.tiltAxisZ = (float) Math.max(-1, Math.min(1, vz));

        this.startAngle = (float) (level.random.nextDouble() * Math.PI * 2);
        this.baseAngularSpeed = 0.10F + level.random.nextFloat() * 0.06F;
        this.wobblePhase = (float) (level.random.nextDouble() * Math.PI * 2);

        // Longer life than vanilla — full collapse takes ~3s
        this.lifetime = 70 + level.random.nextInt(20);

        // Color: start cool purple, transition to white-hot near center
        this.rCol = 0.30F;
        this.gCol = 0.10F;
        this.bCol = 0.65F;
        this.alpha = 0.0F;
        this.quadSize = 0.45F;
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

        // Radius collapses non-linearly: r(t) = r0 × (1 - t)^0.8
        float radius = initialRadius * (float) Math.pow(1.0 - t, 0.8);
        if (radius < 0.05F) radius = 0.05F;

        // Angular velocity accelerates inversely with radius (Kepler-like)
        // Cumulative angle = integral of ω(t) dt — approximated stepwise
        float currentAngle = startAngle + (this.age * baseAngularSpeed) * (initialRadius / Math.max(0.5F, radius));

        // 3D orbit position with tilt
        double ox = Math.cos(currentAngle) * radius;
        double oz = Math.sin(currentAngle) * radius;
        // Apply tilt rotation around X (tiltAxisZ) and Z (tiltAxisX) axes
        double oy = ox * tiltAxisZ - oz * tiltAxisX;
        // Add wobble in Y
        oy += Math.sin(currentAngle * 3 + wobblePhase) * radius * 0.25;

        this.x = centerX + ox;
        this.y = centerY + oy;
        this.z = centerZ + oz;

        // Color shift: as radius shrinks, particle "compresses" → goes white-hot
        float heat = 1.0F - (radius / initialRadius);
        this.rCol = 0.30F + heat * 0.70F;       // purple → white
        this.gCol = 0.10F + heat * 0.60F;
        this.bCol = 0.65F + heat * 0.35F;

        // Alpha curve: fade-in 10%, full 80%, fade-out + bright flash 10%
        if (t < 0.1F) {
            this.alpha = t / 0.1F;
        } else if (t > 0.9F) {
            // Bright flash before disappear (consumed by singularity)
            this.alpha = ((1.0F - t) / 0.1F);
        } else {
            this.alpha = 1.0F;
        }

        // Scale curve: starts at 0.45 → stretches 1.6 (lensing) → 0.0 (consumed)
        if (t < 0.85F) {
            // Stretch as it falls in
            this.quadSize = 0.45F + (heat * 1.15F);
        } else {
            // Compressed/consumed
            float consumeT = (t - 0.85F) / 0.15F;
            this.quadSize = 1.6F * (1.0F - consumeT);
        }
    }

    @Override
    public ParticleRenderType getRenderType() {
        // Additive-ish translucent — TRANSLUCENT works visually for the effect
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;
        public Provider(SpriteSet sprites) { this.sprites = sprites; }
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                        double x, double y, double z,
                                        double vx, double vy, double vz) {
            return new GravitySingularityParticle(level, x, y, z, vx, vy, vz, sprites);
        }
    }
}
