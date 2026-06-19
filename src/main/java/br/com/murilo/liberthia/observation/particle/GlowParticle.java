package br.com.murilo.liberthia.observation.particle;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL11;

/**
 * v0.1.22 r67: <b>GlowParticle</b> — partícula com cor RGB, alpha animado,
 * scale fade, rotação contínua. Additive blending.
 *
 * <h2>Pattern AN's ParticleGlow (lido direto do source)</h2>
 * <ul>
 *   <li><b>1/6 age throttle</b> — {@code if (level.random.nextInt(6) == 0) age++} —
 *       particles sobrevivem 6× mais tempo, criando trails densos.</li>
 *   <li><b>Velocity boost no spawn</b> — {@code xd *= 2.0f} (igual AN).</li>
 *   <li><b>getLightColor() = 255</b> — sempre full bright (independe da luz do mundo).</li>
 *   <li><b>Additive blending</b> — SRC_ALPHA / ONE no render type.</li>
 *   <li><b>tick() override total</b> — skip super.tick() pra controlar age e physics manualmente.</li>
 * </ul>
 */
@OnlyIn(Dist.CLIENT)
public class GlowParticle extends TextureSheetParticle {

    private final float initScale;
    private final float initAlpha;

    public GlowParticle(ClientLevel world, double x, double y, double z,
                         double vx, double vy, double vz,
                         GlowData data, SpriteSet spriteSet) {
        super(world, x, y, z, vx, vy, vz);

        // Cores via TextureSheetParticle fields
        this.rCol = data.r;
        this.gCol = data.g;
        this.bCol = data.b;
        this.alpha = data.alpha;

        this.initAlpha = data.alpha;
        this.initScale = data.size;
        this.quadSize = data.size;
        this.lifetime = Math.max(1, data.age);

        // Motion — AN boost: 2× velocity inicial pra movimento mais dinâmico
        this.xd = vx * 2.0;
        this.yd = vy * 2.0;
        this.zd = vz * 2.0;
        this.hasPhysics = false;
        this.friction = 0.96F;
        this.gravity = 0.0F;

        // Random sprite from set
        this.setSpriteFromAge(spriteSet);

        // Random initial roll
        this.roll = (float)(Math.random() * Math.PI * 2);
        this.oRoll = this.roll;
    }

    /**
     * Always full-bright — AN pattern. Glow particles ignoram luz ambiente.
     * Resolve o problema de particles "morrerem" visualmente em escuro.
     */
    @Override
    public int getLightColor(float pTicks) {
        return 255;
    }

    @Override
    public void tick() {
        // Position carry-over (replica super.tick() top sem o age++)
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        // <<< AN ParticleGlow.java: age só avança a 1/6 da taxa >>>
        // Faz partículas sobreviverem 6× mais → trail visual denso e contínuo.
        if (this.level.random.nextInt(6) == 0) {
            this.age++;
        }

        if (this.age >= this.lifetime) {
            this.remove();
            return;
        }

        // Physics manual (hasPhysics=false, sem collision check)
        this.x += this.xd;
        this.y += this.yd;
        this.z += this.zd;
        this.xd *= this.friction;
        this.yd *= this.friction;
        this.zd *= this.friction;

        // Animations baseadas em age proporcional
        float lifeCoeff = (float) this.age / (float) this.lifetime;
        // Shrink (AN: initScale - initScale * lifeCoeff)
        this.quadSize = initScale - initScale * lifeCoeff;
        // Fade linear
        this.alpha = initAlpha * (1.0F - lifeCoeff);
        // Rotate (AN: roll += 1.0f, mas mais suave aqui)
        this.oRoll = this.roll;
        this.roll += 0.3F;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ADDITIVE_GLOW;
    }

    /**
     * Custom ParticleRenderType — ADDITIVE blending (SRC_ALPHA / ONE),
     * disableDepthMask, particle atlas texture.
     *
     * <p>Pattern de AN's EMBER_RENDER.
     */
    public static final ParticleRenderType ADDITIVE_GLOW = new ParticleRenderType() {
        @Override
        public void begin(BufferBuilder buffer, net.minecraft.client.renderer.texture.TextureManager tm) {
            RenderSystem.depthMask(false);
            RenderSystem.enableBlend();
            RenderSystem.blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE,
                                            GL11.GL_ONE, GL11.GL_ONE);
            RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_PARTICLES);
            RenderSystem.setShader(GameRenderer::getParticleShader);
            buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
        }

        @Override
        public void end(Tesselator tesselator) {
            tesselator.end();
            RenderSystem.depthMask(true);
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableBlend();
        }

        @Override
        public String toString() {
            return "liberthia:additive_glow";
        }
    };

    /** Provider — chamado pelo Forge quando precisa criar a particle. */
    @OnlyIn(Dist.CLIENT)
    public static class Provider implements ParticleProvider<GlowData> {
        private final SpriteSet spriteSet;

        public Provider(SpriteSet spriteSet) {
            this.spriteSet = spriteSet;
        }

        @Nullable
        @Override
        public Particle createParticle(GlowData data, ClientLevel world,
                                        double x, double y, double z,
                                        double vx, double vy, double vz) {
            return new GlowParticle(world, x, y, z, vx, vy, vz, data, spriteSet);
        }
    }
}
