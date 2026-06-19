package br.com.murilo.liberthia.cosmic.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

/**
 * v0.1.22 r41: <b>Collapse Shockwave Particle</b> — anel expansivo que cresce
 * rápido e desaparece, simulando onda de choque pós-implosão.
 *
 * <h2>Motion model</h2>
 * <ul>
 *   <li>Posição = ponto fixo (não orbita — partícula EXPANDE no lugar)</li>
 *   <li>Scale: rápido easeOut — começa em 0, cresce até maxScale em 70%</li>
 *   <li>Alpha: instantânea 1.0 → fade quadrático nos últimos 30%</li>
 *   <li>roll: constante (não rotaciona — não é "shimmer", é "expansão")</li>
 * </ul>
 *
 * <h2>Color</h2>
 * Branco-quente nas bordas, transparente no centro — usado em anel.
 * Cores RGB: 0.9, 0.7, 1.0 (white-violet).
 *
 * <p>Server uso típico: emite ~30 particles em ring formation usando
 * sendParticles em coords ao redor de um center, todas com mesma idade.
 */
public class CollapseShockwaveParticle extends TextureSheetParticle {

    private final float maxScale;

    public CollapseShockwaveParticle(ClientLevel level, double x, double y, double z,
                                      double vx, double vy, double vz, SpriteSet sprites) {
        super(level, x, y, z);
        // vy = max scale (default 3)
        this.maxScale = (float) Math.max(1.0, Math.min(6.0, vy != 0 ? vy : 3.0));

        // Short life — shockwave é rápido
        this.lifetime = 25 + level.random.nextInt(10);

        // White-violet
        this.rCol = 0.95F;
        this.gCol = 0.75F;
        this.bCol = 1.0F;
        this.alpha = 1.0F;
        this.quadSize = 0.1F;
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

        // Ease-out cubic: fast start, slow finish
        // scale(t) = maxScale × (1 - (1-t)³) durante crescimento (0-70%)
        if (t < 0.7F) {
            float n = t / 0.7F;
            float ease = 1.0F - (float) Math.pow(1.0F - n, 3);
            this.quadSize = 0.1F + (maxScale - 0.1F) * ease;
        } else {
            // Hold at max with slight contraction
            this.quadSize = maxScale * (1.0F - (t - 0.7F) * 0.3F);
        }

        // Alpha: full alpha 0-70%, quadratic fade-out 70-100%
        if (t > 0.7F) {
            float fade = (t - 0.7F) / 0.3F;
            this.alpha = (1.0F - fade) * (1.0F - fade);
        } else {
            this.alpha = 1.0F;
        }

        // Color: fade from white-hot to deep violet at end
        float fade = Math.max(0, t - 0.5F) * 2F;
        this.rCol = 0.95F - fade * 0.5F;
        this.gCol = 0.75F - fade * 0.5F;
        this.bCol = 1.0F - fade * 0.3F;
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
            return new CollapseShockwaveParticle(level, x, y, z, vx, vy, vz, sprites);
        }
    }
}
