package br.com.murilo.liberthia.particle;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.particle.engine.ConfigurableParticleOptions;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.util.Mth;

public class ConfigurableSpriteParticle extends TextureSheetParticle {

    private final SpriteSet sprites;
    private final ConfigurableParticleOptions options;

    private final float startSize;
    private final float endSize;
    private final float baseAlpha;
    private final float spinSpeed;

    protected ConfigurableSpriteParticle(
            ClientLevel level,
            double x,
            double y,
            double z,
            double xSpeed,
            double ySpeed,
            double zSpeed,
            ConfigurableParticleOptions options,
            SpriteSet sprites
    ) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed);

        this.sprites = sprites;
        this.options = options;

        this.lifetime = Math.max(1, options.lifetime());

        this.startSize = Math.max(0.01F, options.startSize());
        this.endSize = Math.max(0.01F, options.endSize());
        this.baseAlpha = Mth.clamp(options.alpha(), 0.0F, 1.0F);
        this.spinSpeed = options.spinSpeed();

        this.quadSize = this.startSize;

        /*
         * Nova lógica:
         *
         * Se colorTintEnabled == true:
         *   usa a cor enviada pela config e tinge o sprite.
         *
         * Se colorTintEnabled == false:
         *   usa branco puro, então a textura mantém as cores originais do PNG.
         */
        if (options.colorTintEnabled()) {
            this.rCol = Mth.clamp(options.red(), 0.0F, 1.0F);
            this.gCol = Mth.clamp(options.green(), 0.0F, 1.0F);
            this.bCol = Mth.clamp(options.blue(), 0.0F, 1.0F);
        } else {
            this.rCol = 1.0F;
            this.gCol = 1.0F;
            this.bCol = 1.0F;
        }

        this.alpha = this.baseAlpha;

        this.gravity = options.gravity();
        this.friction = Mth.clamp(options.friction(), 0.0F, 1.0F);
        this.hasPhysics = options.hasPhysics();

        this.xd = xSpeed;
        this.yd = ySpeed;
        this.zd = zSpeed;

        this.roll = this.random.nextFloat() * ((float) Math.PI * 2.0F);
        this.oRoll = this.roll;

        this.pickSprite(sprites);
    }

    @Override
    public void tick() {
        this.oRoll = this.roll;

        super.tick();

        if (!this.isAlive()) {
            return;
        }

        float progress = this.age / (float) this.lifetime;
        progress = Mth.clamp(progress, 0.0F, 1.0F);

        this.setSpriteFromAge(this.sprites);

        this.quadSize = Mth.lerp(progress, this.startSize, this.endSize);

        float fade;

        if (progress < 0.15F) {
            fade = progress / 0.15F;
        } else if (progress > 0.70F) {
            fade = 1.0F - ((progress - 0.70F) / 0.30F);
        } else {
            fade = 1.0F;
        }

        this.alpha = this.baseAlpha * Mth.clamp(fade, 0.0F, 1.0F);

        this.roll += this.spinSpeed * (1.0F - progress);

        if (this.onGround) {
            this.xd *= 0.60D;
            this.zd *= 0.60D;
            this.yd = 0.0D;
        }
    }

    @Override
    public float getQuadSize(float partialTicks) {
        float progress = ((float) this.age + partialTicks) / (float) this.lifetime;
        progress = Mth.clamp(progress, 0.0F, 1.0F);
        return Mth.lerp(progress, this.startSize, this.endSize);
    }

    @Override
    protected int getLightColor(float partialTicks) {
        if (this.options.emissive()) {
            return 15728880;
        }

        return super.getLightColor(partialTicks);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static class Provider implements ParticleProvider<ConfigurableParticleOptions> {

        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(
                ConfigurableParticleOptions options,
                ClientLevel level,
                double x,
                double y,
                double z,
                double xSpeed,
                double ySpeed,
                double zSpeed
        ) {
            try {
                return new ConfigurableSpriteParticle(
                        level,
                        x,
                        y,
                        z,
                        xSpeed,
                        ySpeed,
                        zSpeed,
                        options,
                        this.sprites
                );
            } catch (Exception exception) {
                LiberthiaMod.LOGGER.error(
                        "Failed to create ENGINE_PARTICLE. Check assets/liberthia/particles/engine_particle.json and particle textures.",
                        exception
                );

                return null;
            }
        }
    }
}