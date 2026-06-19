package br.com.murilo.liberthia.observation.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

/**
 * v0.1.22 r64: <b>LineParticle</b> — particle que interpola de (initX,Y,Z)
 * → (destX,Y,Z) durante seu lifetime. Pattern AN's {@code ParticleLine}.
 *
 * <h2>Mecânica</h2>
 * <pre>
 * t = age / lifetime
 * x = (1-t) * initX + t * destX     // lerp
 * quadSize = initScale * (1 - t)    // shrink
 * alpha = 1.0 - t                    // fade
 * </pre>
 *
 * <p>Render type: {@link GlowParticle#ADDITIVE_GLOW} — reuse o additive blend
 * que já temos. Ambas usam o mesmo atlas particle texture.
 */
@OnlyIn(Dist.CLIENT)
public class LineParticle extends TextureSheetParticle {

    private final float initScale;
    private final float initX, initY, initZ;
    private final float destX, destY, destZ;

    public LineParticle(ClientLevel world, double x, double y, double z,
                         LineData data, SpriteSet spriteSet) {
        super(world, x, y, z, 0, 0, 0);

        this.rCol = data.r;
        this.gCol = data.g;
        this.bCol = data.b;
        this.alpha = 1.0F;

        this.initScale = data.scale;
        this.quadSize = data.scale;
        this.lifetime = Math.max(1, data.age);

        this.initX = (float) x;
        this.initY = (float) y;
        this.initZ = (float) z;
        this.destX = data.destX;
        this.destY = data.destY;
        this.destZ = data.destZ;

        this.hasPhysics = false;
        this.gravity = 0F;
        this.friction = 1F;

        this.setSpriteFromAge(spriteSet);
    }

    @Override
    public void tick() {
        if (this.age >= this.lifetime) {
            this.remove();
            return;
        }
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        float t = (float) this.age / (float) this.lifetime;
        // LERP de init → dest (igual AN's ParticleLine)
        this.x = (1.0F - t) * initX + t * destX;
        this.y = (1.0F - t) * initY + t * destY;
        this.z = (1.0F - t) * initZ + t * destZ;
        // Scale fade
        this.quadSize = initScale * (1.0F - t);
        // Alpha fade
        this.alpha = 1.0F - t;
        this.age++;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return GlowParticle.ADDITIVE_GLOW;
    }

    @OnlyIn(Dist.CLIENT)
    public static class Provider implements ParticleProvider<LineData> {
        private final SpriteSet spriteSet;
        public Provider(SpriteSet spriteSet) { this.spriteSet = spriteSet; }

        @Nullable
        @Override
        public Particle createParticle(LineData data, ClientLevel world,
                                        double x, double y, double z,
                                        double vx, double vy, double vz) {
            return new LineParticle(world, x, y, z, data, spriteSet);
        }
    }
}
