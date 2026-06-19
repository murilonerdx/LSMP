package br.com.murilo.liberthia.fog.particle;

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
 * <b>FogMistParticle</b> — puff de fumaça/névoa SUAVE e TRANSLÚCIDO.
 *
 * <h2>Diferença pro GlowParticle</h2>
 * <ul>
 *   <li><b>Blending normal</b> ({@link ParticleRenderType#PARTICLE_SHEET_TRANSLUCENT})
 *       em vez de additive — cor escura vira véu escuro (não some).</li>
 *   <li><b>Fade-in + fade-out</b> suave (entra e sai sem "pop").</li>
 *   <li><b>Cresce devagar</b> ao longo da vida (fumaça expandindo).</li>
 *   <li><b>Drift lento</b> com leve subida — névoa flutuando parada.</li>
 *   <li>Sem gravidade, sem colisão.</li>
 * </ul>
 */
@OnlyIn(Dist.CLIENT)
public class FogMistParticle extends TextureSheetParticle {

    private final float maxAlpha;
    private final float baseSize;
    private final SpriteSet sprites;

    protected FogMistParticle(ClientLevel level, double x, double y, double z,
                              double vx, double vy, double vz,
                              FogMistData data, SpriteSet sprites) {
        super(level, x, y, z, 0.0, 0.0, 0.0);
        this.sprites = sprites;

        // cor da zona
        this.rCol = data.r;
        this.gCol = data.g;
        this.bCol = data.b;

        this.maxAlpha = data.alpha;
        this.alpha = 0.0F; // começa invisível e faz fade-in
        this.baseSize = data.size;
        this.quadSize = data.size;
        this.lifetime = Math.max(1, data.age);

        // drift lento e HORIZONTAL — quase não sobe (não flutua pro céu).
        // Sobrescreve o random velocity grande que o ctor base do Particle aplica.
        this.xd = vx + (level.random.nextDouble() - 0.5) * 0.006;
        this.yd = vy + (level.random.nextDouble() - 0.5) * 0.0012;
        this.zd = vz + (level.random.nextDouble() - 0.5) * 0.006;

        this.hasPhysics = false;
        this.friction = 0.985F;
        this.gravity = 0.0F;

        this.setSpriteFromAge(sprites);

        // rotação inicial aleatória pra cada puff parecer diferente
        this.roll = (float) (Math.random() * Math.PI * 2.0);
        this.oRoll = this.roll;
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
        this.setSpriteFromAge(this.sprites);

        // movimento suave
        this.x += this.xd;
        this.y += this.yd;
        this.z += this.zd;
        this.xd *= this.friction;
        this.yd *= this.friction;
        this.zd *= this.friction;

        float life = (float) this.age / (float) this.lifetime;

        // expande pouco (fumaça crescendo de leve, sem virar bola)
        this.quadSize = baseSize * (0.90F + 0.18F * life);

        // fade-in nos primeiros 25%, segura, fade-out nos últimos 40%
        float a;
        if (life < 0.25F) {
            a = maxAlpha * (life / 0.25F);
        } else if (life > 0.60F) {
            a = maxAlpha * (1.0F - (life - 0.60F) / 0.40F);
        } else {
            a = maxAlpha;
        }
        this.alpha = Math.max(0.0F, a);

        // rotação lenta pra névoa "respirar"
        this.oRoll = this.roll;
        this.roll += 0.015F;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @OnlyIn(Dist.CLIENT)
    public static class Provider implements ParticleProvider<FogMistData> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Nullable
        @Override
        public Particle createParticle(FogMistData data, ClientLevel level,
                                       double x, double y, double z,
                                       double vx, double vy, double vz) {
            return new FogMistParticle(level, x, y, z, vx, vy, vz, data, sprites);
        }
    }
}
