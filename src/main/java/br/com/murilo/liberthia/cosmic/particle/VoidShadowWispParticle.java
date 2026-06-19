package br.com.murilo.liberthia.cosmic.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

/**
 * <b>Void Shadow Wisp</b> — partícula CUSTOM (não-vanilla) de fumaça NEGRA que dá
 * corpo à sombra do Vazio. Sobe devagar, expande (cresce muito de tamanho), gira a
 * própria textura e some com fade longo — formando uma massa escura viva em vez de
 * usar SQUID_INK/LARGE_SMOKE vanilla.
 *
 * <p>Render translúcido normal (escurece o fundo), big quad, lifetime longo.
 */
public class VoidShadowWispParticle extends TextureSheetParticle {

    private final SpriteSet sprites;
    private final float spin;

    public VoidShadowWispParticle(ClientLevel level, double x, double y, double z,
                                  double vx, double vy, double vz, SpriteSet sprites) {
        super(level, x, y, z, 0, 0, 0);
        this.sprites = sprites;
        this.lifetime = 30 + level.random.nextInt(24);
        this.gravity = 0;
        this.hasPhysics = false;
        this.friction = 0.92F;
        // deriva inicial bem suave a partir do vetor de spawn
        this.xd = vx * 0.4; this.yd = Math.abs(vy) * 0.3 + 0.01; this.zd = vz * 0.4;
        this.alpha = 0F;
        this.quadSize = 0.7F + level.random.nextFloat() * 0.6F;
        this.spin = (level.random.nextFloat() - 0.5F) * 0.12F;
        this.roll = (float) (level.random.nextDouble() * Math.PI * 2);
        this.oRoll = this.roll;
        // fumaça é quase preta, leve tom roxo
        this.rCol = 0.06F; this.gCol = 0.02F; this.bCol = 0.10F;
        this.setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        this.xo = this.x; this.yo = this.y; this.zo = this.z;
        this.oRoll = this.roll;
        if (this.age++ >= this.lifetime) { this.remove(); return; }
        this.setSpriteFromAge(sprites);

        this.x += this.xd; this.y += this.yd; this.z += this.zd;
        this.xd *= 0.92; this.zd *= 0.92;
        this.yd += 0.002;       // sobe lento
        this.roll += this.spin; // gira a textura

        float t = (float) this.age / this.lifetime;
        // alpha: aparece rápido, segura, some devagar (sombra densa)
        if (t < 0.2F) this.alpha = (t / 0.2F) * 0.85F;
        else if (t > 0.5F) this.alpha = Math.max(0F, 0.85F * (1F - (t - 0.5F) / 0.5F));
        else this.alpha = 0.85F;
        // expande continuamente
        this.quadSize = (0.7F + t * 1.6F);
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
            return new VoidShadowWispParticle(level, x, y, z, vx, vy, vz, sprites);
        }
    }
}
