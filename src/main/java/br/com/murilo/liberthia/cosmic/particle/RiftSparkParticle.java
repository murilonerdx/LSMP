package br.com.murilo.liberthia.cosmic.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

/**
 * v0.1.22 r35: <b>Rift Spark</b> — pequenos sparks que aparecem ao redor do
 * rasgo dimensional. Bright cyan/magenta, vida curta, scale pulse.
 *
 * <p>Usado pelo Sky Rift renderer pra dar "vida" ao rasgo.
 */
public class RiftSparkParticle extends TextureSheetParticle {

    public RiftSparkParticle(ClientLevel level, double x, double y, double z,
                              double vx, double vy, double vz, SpriteSet sprites) {
        super(level, x, y, z, vx, vy, vz);
        this.lifetime = 15 + level.random.nextInt(15);
        // Alternates entre cyan e magenta
        if (level.random.nextBoolean()) {
            this.rCol = 0.2F; this.gCol = 0.9F; this.bCol = 1.0F; // cyan
        } else {
            this.rCol = 1.0F; this.gCol = 0.2F; this.bCol = 0.9F; // magenta
        }
        this.alpha = 1.0F;
        this.quadSize = 0.15F;
        this.gravity = 0.0F;
        this.xd = vx;
        this.yd = vy;
        this.zd = vz;
        this.hasPhysics = false;
        this.setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.age++ >= this.lifetime) { this.remove(); return; }
        // Move linear with velocity decay
        this.xd *= 0.9;
        this.yd *= 0.9;
        this.zd *= 0.9;
        this.x += this.xd;
        this.y += this.yd;
        this.z += this.zd;
        // Pulse scale + fade
        float lifeP = (float) this.age / this.lifetime;
        this.quadSize = 0.15F + (float) Math.sin(lifeP * Math.PI) * 0.3F;
        this.alpha = 1.0F - lifeP;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_LIT;
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;
        public Provider(SpriteSet sprites) { this.sprites = sprites; }
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                        double x, double y, double z,
                                        double vx, double vy, double vz) {
            return new RiftSparkParticle(level, x, y, z, vx, vy, vz, sprites);
        }
    }
}
