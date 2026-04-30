package br.com.murilo.liberthia.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

public class DarkBloodParticle extends TextureSheetParticle {

    private final SpriteSet sprites;

    private final float startSize;
    private final float spinSpeed;
    private final float baseRed;
    private final float baseGreen;
    private final float baseBlue;

    private boolean touchedGround;

    protected DarkBloodParticle(
            ClientLevel level,
            double x,
            double y,
            double z,
            double xSpeed,
            double ySpeed,
            double zSpeed,
            SpriteSet sprites
    ) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed);

        this.sprites = sprites;

        this.lifetime = 22 + this.random.nextInt(16);

        this.startSize = 0.18F + this.random.nextFloat() * 0.22F;
        this.quadSize = this.startSize;

        this.xd = xSpeed + (this.random.nextDouble() - 0.5D) * 0.015D;
        this.yd = ySpeed + this.random.nextDouble() * 0.035D;
        this.zd = zSpeed + (this.random.nextDouble() - 0.5D) * 0.015D;

        this.gravity = 0.16F;
        this.friction = 0.86F;
        this.hasPhysics = true;

        this.baseRed = 0.35F + this.random.nextFloat() * 0.18F;
        this.baseGreen = 0.0F;
        this.baseBlue = 0.025F + this.random.nextFloat() * 0.05F;

        this.rCol = this.baseRed;
        this.gCol = this.baseGreen;
        this.bCol = this.baseBlue;

        this.alpha = 0.95F;

        this.roll = this.random.nextFloat() * ((float) Math.PI * 2.0F);
        this.oRoll = this.roll;

        this.spinSpeed = ((this.random.nextBoolean() ? 1.0F : -1.0F)
                * (0.08F + this.random.nextFloat() * 0.13F));

        this.touchedGround = false;

        this.pickSprite(sprites);
    }

    @Override
    public void tick() {
        this.oRoll = this.roll;

        super.tick();

        if (!this.isAlive()) {
            return;
        }

        float life = this.age / (float) this.lifetime;
        float clampedLife = Mth.clamp(life, 0.0F, 1.0F);

        this.setSpriteFromAge(this.sprites);

        this.roll += this.spinSpeed * (1.0F - clampedLife);

        float fadeStart = 0.55F;
        if (clampedLife > fadeStart) {
            float fade = (clampedLife - fadeStart) / (1.0F - fadeStart);
            this.alpha = 0.95F * (1.0F - fade);
        } else {
            this.alpha = 0.95F;
        }

        float pulse = Mth.sin(clampedLife * (float) Math.PI);
        this.quadSize = this.startSize * (0.75F + pulse * 0.85F);

        if (this.onGround) {
            if (!this.touchedGround) {
                this.touchedGround = true;

                this.yd = 0.0D;
                this.xd *= 0.28D;
                this.zd *= 0.28D;

                this.gravity = 0.0F;
                this.friction = 0.72F;

                this.quadSize *= 1.35F;
                this.alpha = Math.min(this.alpha, 0.75F);
            } else {
                this.xd *= 0.68D;
                this.zd *= 0.68D;
                this.yd = 0.0D;

                this.quadSize *= 1.018F;
            }
        }

        float darkness = 1.0F - clampedLife * 0.45F;

        this.rCol = this.baseRed * darkness;
        this.gCol = this.baseGreen;
        this.bCol = this.baseBlue * darkness;
    }

    @Override
    public float getQuadSize(float partialTicks) {
        float life = ((float) this.age + partialTicks) / (float) this.lifetime;
        life = Mth.clamp(life, 0.0F, 1.0F);

        float grow = Mth.sin(life * (float) Math.PI);
        return this.quadSize * (0.85F + grow * 0.35F);
    }

    @Override
    protected int getLightColor(float partialTicks) {
        return 15728880;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {

        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(
                SimpleParticleType type,
                ClientLevel level,
                double x,
                double y,
                double z,
                double xSpeed,
                double ySpeed,
                double zSpeed
        ) {
            return new DarkBloodParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, this.sprites);
        }
    }
}
