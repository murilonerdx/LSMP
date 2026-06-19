package br.com.murilo.liberthia.magic.spell.particle;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.magic.school.SpellSchool;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * v0.1.145 r113: <b>SpellTrailParticle</b> — partícula client-side animada
 * via {@link TextureSheetParticle}. Lê o spritesheet correto (`spell_trail_{school}.png`)
 * baseado na escola do {@link SpellTrailParticleData}.
 *
 * <p>Comportamento:
 * <ul>
 *   <li>Animação cycles via {@code .png.mcmeta} (2t/frame, 8 frames = 16t loop)</li>
 *   <li>Scale animado: cresce no início, encolhe no final (ease-in-out)</li>
 *   <li>Alpha fade-out nos últimos 30% da vida</li>
 *   <li>Rotação leve aleatória pra variedade</li>
 *   <li>Movimento: friction 0.92 + leve drift pra cima (0.005/tick)</li>
 *   <li>RenderType: PARTICLE_SHEET_TRANSLUCENT (não emissive — o spritesheet já
 *       tem cores brilhantes; emissive faria saturar demais)</li>
 * </ul>
 *
 * <p>Código original — apenas usa a API pública {@code TextureSheetParticle}
 * vanilla. Não derivado de outros mods.
 */
@OnlyIn(Dist.CLIENT)
public class SpellTrailParticle extends TextureSheetParticle {

    /** Cache de TextureAtlasSprite por escola (carregado on demand). */
    private static final TextureAtlasSprite[] SPRITE_CACHE = new TextureAtlasSprite[SpellSchool.values().length];

    private final float initialScale;
    private final int maxLife;

    protected SpellTrailParticle(ClientLevel level, double x, double y, double z,
                                 double dx, double dy, double dz,
                                 SpellTrailParticleData data, SpriteSet spriteSet) {
        super(level, x, y, z, dx, dy, dz);
        this.initialScale = data.scale;
        this.maxLife = data.lifeTicks;
        this.lifetime = data.lifeTicks;
        this.gravity = 0F;
        this.friction = 0.92F;
        this.quadSize = data.scale * 0.5F; // starting size half (grows)
        this.hasPhysics = false;

        // Random initial rotation (radians)
        this.roll = (float) (Math.random() * Math.PI * 2);
        this.oRoll = this.roll;

        // Pick sprite from atlas for this school. Forge's SpriteSet doesn't expose
        // per-school selection, so we pull the sprite directly from the texture
        // manager once and cache it.
        TextureAtlasSprite sprite = getSchoolSprite(data.schoolIndex);
        if (sprite != null) {
            this.setSprite(sprite);
        } else {
            // Fallback to first sprite in the spriteSet if loading failed
            this.pickSprite(spriteSet);
        }

        // Initial motion = passed dx,dy,dz scaled down + small upward drift
        this.xd = dx * 0.4 + (Math.random() - 0.5) * 0.02;
        this.yd = dy * 0.4 + 0.005;
        this.zd = dz * 0.4 + (Math.random() - 0.5) * 0.02;
    }

    private static TextureAtlasSprite getSchoolSprite(int idx) {
        if (idx < 0 || idx >= SPRITE_CACHE.length) idx = 0;
        if (SPRITE_CACHE[idx] != null) return SPRITE_CACHE[idx];
        try {
            String schoolName = SpellSchool.values()[idx].name().toLowerCase();
            // r157 FIX: sprite key NÃO inclui "particle/" prefix — atlas resolve
            // automaticamente para textures/particle/<name>.png.
            ResourceLocation tex = new ResourceLocation(LiberthiaMod.MODID,
                    "spell_trail_" + schoolName);
            SPRITE_CACHE[idx] = Minecraft.getInstance()
                    .getTextureAtlas(TextureAtlas.LOCATION_PARTICLES)
                    .apply(tex);
            return SPRITE_CACHE[idx];
        } catch (Throwable t) {
            // r157: log uma vez por school para não floodar console
            if (!WARNED[idx]) {
                LiberthiaMod.LOGGER.warn("[SpellTrailParticle] failed loading sprite for school {}: {}", idx, t.getMessage());
                WARNED[idx] = true;
            }
            return null;
        }
    }
    private static final boolean[] WARNED = new boolean[SpellSchool.values().length];

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public void tick() {
        super.tick();
        // Scale animation: ease-in then ease-out around midpoint
        float lifeProgress = (float) this.age / Math.max(1, this.maxLife);
        // Bell curve: 4*x*(1-x) peaks at 0.5 = 1.0
        float curve = 4F * lifeProgress * (1F - lifeProgress);
        this.quadSize = initialScale * (0.4F + curve * 0.8F);
        // Alpha fade-out in last 30%
        if (lifeProgress > 0.7F) {
            this.alpha = Math.max(0F, 1F - (lifeProgress - 0.7F) / 0.3F);
        } else {
            this.alpha = 1F;
        }
        // Slow spin
        this.oRoll = this.roll;
        this.roll += 0.05F;
    }

    @OnlyIn(Dist.CLIENT)
    public static class Provider implements ParticleProvider<SpellTrailParticleData> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SpellTrailParticleData data, ClientLevel level,
                                       double x, double y, double z,
                                       double dx, double dy, double dz) {
            return new SpellTrailParticle(level, x, y, z, dx, dy, dz, data, sprites);
        }
    }
}
