package br.com.murilo.liberthia.magic.spell.voidspell;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

/**
 * v0.1.152 r120: <b>VoidInfectionParticle</b> — partícula custom roxa que sobe
 * lentamente, gira, e desbota. Usa spritesheet animado (8 frames).
 */
public class VoidInfectionParticle extends TextureSheetParticle {

    private final SpriteSet sprites;

    protected VoidInfectionParticle(ClientLevel level, double x, double y, double z,
                                     double dx, double dy, double dz, SpriteSet sprites) {
        super(level, x, y, z);
        this.sprites = sprites;
        this.lifetime = 30 + level.random.nextInt(15);
        this.gravity = -0.02F; // sobe ligeiramente
        this.friction = 0.92F;
        this.xd = dx + (level.random.nextDouble() - 0.5) * 0.04;
        this.yd = dy + 0.05 + level.random.nextDouble() * 0.05;
        this.zd = dz + (level.random.nextDouble() - 0.5) * 0.04;
        this.quadSize = 0.18F + level.random.nextFloat() * 0.12F;
        // Cor: purple base com variação
        float pr = 0.6F + level.random.nextFloat() * 0.3F;
        this.rCol = pr;
        this.gCol = 0.05F + level.random.nextFloat() * 0.15F;
        this.bCol = 0.7F + level.random.nextFloat() * 0.3F;
        this.alpha = 1.0F;
        this.hasPhysics = false;
        this.setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        // Fade-out nos últimos 1/3 do lifetime
        if (this.age > this.lifetime * 2 / 3) {
            this.alpha = Math.max(0F, (this.lifetime - this.age) / (float)(this.lifetime / 3));
        }
        this.setSpriteFromAge(sprites);
        // Rotaciona
        this.oRoll = this.roll;
        this.roll += 0.08F;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;
        public Provider(SpriteSet s) { this.sprites = s; }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                        double x, double y, double z,
                                        double dx, double dy, double dz) {
            return new VoidInfectionParticle(level, x, y, z, dx, dy, dz, sprites);
        }
    }
}
