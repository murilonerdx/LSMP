package br.com.murilo.liberthia.cosmic.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

/**
 * v0.1.22 r35: <b>Void Leak Particle</b> — partícula que cai do céu como gota
 * de "tinta dimensional". Movimento turbulento via noise simulado.
 *
 * <ul>
 *   <li>Spawn: posição alta, velocity DOWN</li>
 *   <li>Movimento: turbulência via seno/cosseno fora-de-fase</li>
 *   <li>Cor: muda gradualmente do PRETO pro VERMELHO ESCURO ao longo da vida</li>
 *   <li>Scale aumenta no decay (gota fica grande quando "splash")</li>
 * </ul>
 */
public class VoidLeakParticle extends TextureSheetParticle {

    private final float turbulenceSeed;
    private final float fallSpeed;

    public VoidLeakParticle(ClientLevel level, double x, double y, double z,
                             double vx, double vy, double vz, SpriteSet sprites) {
        super(level, x, y, z, 0, 0, 0);
        this.turbulenceSeed = level.random.nextFloat() * 100;
        this.fallSpeed = 0.08F + level.random.nextFloat() * 0.05F;
        this.lifetime = 80 + level.random.nextInt(40);
        // Inicia preto
        this.rCol = 0.05F;
        this.gCol = 0.0F;
        this.bCol = 0.1F;
        this.alpha = 0.7F;
        this.quadSize = 0.4F + level.random.nextFloat() * 0.3F;
        this.hasPhysics = true;
        this.gravity = 0.0F; // controlado manualmente
        this.setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.age++ >= this.lifetime || this.onGround) {
            // Splash final
            if (this.onGround && this.age < this.lifetime - 5) {
                this.quadSize *= 1.5F;
                this.alpha *= 0.7F;
            }
            if (this.age >= this.lifetime) {
                this.remove();
                return;
            }
        }
        // Movimento: fall + turbulência
        float t = (this.age + turbulenceSeed) * 0.15F;
        this.xd = Math.sin(t * 1.3) * 0.04;
        this.yd = -this.fallSpeed;
        this.zd = Math.cos(t * 1.7) * 0.04;
        this.x += this.xd;
        this.y += this.yd;
        this.z += this.zd;

        // Cor: preto → vermelho escuro
        float lifeP = (float) this.age / this.lifetime;
        this.rCol = 0.05F + lifeP * 0.5F;
        this.gCol = lifeP * 0.05F;
        this.bCol = 0.1F + lifeP * 0.1F;
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
            return new VoidLeakParticle(level, x, y, z, vx, vy, vz, sprites);
        }
    }
}
