package br.com.murilo.liberthia.magic.spell.voidspell;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

/**
 * v0.1.152 r120: <b>MiniBlackHoleParticle</b> — partícula custom da explosão
 * do mini buraco negro. Spritesheet de 6 frames mostrando colapso.
 */
public class MiniBlackHoleParticle extends TextureSheetParticle {

    private final SpriteSet sprites;

    protected MiniBlackHoleParticle(ClientLevel level, double x, double y, double z,
                                     double dx, double dy, double dz, SpriteSet sprites) {
        super(level, x, y, z);
        this.sprites = sprites;
        this.lifetime = 18;
        this.gravity = 0.0F;
        this.friction = 0.85F;
        this.xd = dx;
        this.yd = dy;
        this.zd = dz;
        this.quadSize = 0.6F + level.random.nextFloat() * 0.3F;
        this.rCol = 0.15F;
        this.gCol = 0.0F;
        this.bCol = 0.25F;
        this.alpha = 1.0F;
        this.hasPhysics = false;
        this.setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        this.setSpriteFromAge(sprites);
        // Encolhe ao longo da vida
        float t = this.age / (float) this.lifetime;
        this.quadSize = (0.6F + this.random.nextFloat() * 0.3F) * (1.0F - t * 0.7F);
        // Fade-out
        this.alpha = 1.0F - t * 0.8F;
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
            return new MiniBlackHoleParticle(level, x, y, z, dx, dy, dz, sprites);
        }
    }
}
