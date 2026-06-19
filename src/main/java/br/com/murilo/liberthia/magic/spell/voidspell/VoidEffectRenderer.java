package br.com.murilo.liberthia.magic.spell.voidspell;

import br.com.murilo.liberthia.LiberthiaMod;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

/**
 * r165: <b>VoidEffectRenderer</b> — generic billboard renderer cycling through
 * sprite frames specific to each {@link VoidEffectEntity} type.
 *
 * <p>Per-type sprite arrays + per-type aspect ratio + per-type orientation:
 * <ul>
 *   <li>MIND_SPIKE — vertical billboard (16×32), 5 frames, plays once</li>
 *   <li>SOUL_TEAR — billboard square (32×32), 6 frames, loops</li>
 *   <li>MADNESS_WAVE — horizontal flat (64×16) lying on ground, 8 frames once</li>
 *   <li>COSMIC_VOID — large billboard square (64×64), 10 frames, loops + grows</li>
 *   <li>ELDRITCH_BLAST — small burst (32×32), 4 frames once</li>
 * </ul>
 */
public class VoidEffectRenderer extends EntityRenderer<VoidEffectEntity> {

    // ── Sprite arrays per type ────────────────────────────────────────────────
    private static final ResourceLocation[] MIND_SPIKE_FRAMES = build("mind_spike/spike_", 5);
    private static final ResourceLocation[] SOUL_TEAR_FRAMES  = build("soul_tear/tear_", 6);
    private static final ResourceLocation[] MADNESS_WAVE_FRAMES = build("madness_wave/wave_", 8);
    private static final ResourceLocation[] COSMIC_VOID_FRAMES  = build("cosmic_void/vortex_", 10);
    private static final ResourceLocation[] ELDRITCH_BLAST_FRAMES = build("eldritch_blast/blast_", 4);

    private static ResourceLocation[] build(String prefix, int count) {
        ResourceLocation[] r = new ResourceLocation[count];
        for (int i = 0; i < count; i++) {
            r[i] = new ResourceLocation(LiberthiaMod.MODID,
                    "textures/entity/" + prefix + i + ".png");
        }
        return r;
    }

    public VoidEffectRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.shadowRadius = 0F;
    }

    @Override
    public void render(VoidEffectEntity entity, float yaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        int type = entity.getEffectType();
        ResourceLocation[] frames = framesForType(type);
        ResourceLocation tex = pickFrame(entity, frames);
        float scale = entity.getScale();

        poseStack.pushPose();

        // Per-type render setup
        switch (type) {
            case VoidEffectEntity.TYPE_MIND_SPIKE -> {
                billboardFace(poseStack);
                renderQuad(poseStack, buffer, tex, packedLight,
                        0.4F * scale, 0.8F * scale, 0F);
            }
            case VoidEffectEntity.TYPE_SOUL_TEAR -> {
                billboardFace(poseStack);
                float spinScale = 1.0F + entity.getProgress() * 0.3F;
                renderQuad(poseStack, buffer, tex, packedLight,
                        1.0F * scale * spinScale, 1.0F * scale * spinScale, 0F);
            }
            case VoidEffectEntity.TYPE_MADNESS_WAVE -> {
                // Flat on ground — rotate to lay horizontally
                poseStack.mulPose(Axis.XP.rotationDegrees(90));
                float expand = entity.getProgress() * 8.0F;  // grows outward
                float w = (2.0F + expand) * scale;
                renderQuad(poseStack, buffer, tex, packedLight, w, w * 0.4F, 0.01F);
            }
            case VoidEffectEntity.TYPE_COSMIC_VOID -> {
                billboardFace(poseStack);
                float ringScale = 1.0F + entity.getProgress() * 0.5F;
                renderQuad(poseStack, buffer, tex, packedLight,
                        2.5F * scale * ringScale, 2.5F * scale * ringScale, 0F);
            }
            case VoidEffectEntity.TYPE_ELDRITCH_BLAST -> {
                billboardFace(poseStack);
                float burstScale = 0.5F + entity.getProgress() * 1.0F;
                renderQuad(poseStack, buffer, tex, packedLight,
                        1.5F * scale * burstScale, 1.5F * scale * burstScale, 0F);
            }
        }

        poseStack.popPose();
        super.render(entity, yaw, partialTick, poseStack, buffer, packedLight);
    }

    private void billboardFace(PoseStack ps) {
        ps.mulPose(Axis.YP.rotationDegrees(-this.entityRenderDispatcher.camera.getYRot()));
        ps.mulPose(Axis.XP.rotationDegrees(this.entityRenderDispatcher.camera.getXRot()));
    }

    /** Renders a single textured quad facing forward (Z+) with given half-extents. */
    private void renderQuad(PoseStack ps, MultiBufferSource buf, ResourceLocation tex,
                            int light, float halfW, float halfH, float yBase) {
        VertexConsumer vb = buf.getBuffer(RenderType.entityTranslucent(tex));
        Matrix4f m = ps.last().pose();
        // CCW from top-left when looking at Z+ face
        vb.vertex(m, -halfW, yBase + halfH * 2, 0).color(255, 255, 255, 255)
                .uv(0, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light)
                .normal(0, 0, 1).endVertex();
        vb.vertex(m, halfW, yBase + halfH * 2, 0).color(255, 255, 255, 255)
                .uv(1, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light)
                .normal(0, 0, 1).endVertex();
        vb.vertex(m, halfW, yBase, 0).color(255, 255, 255, 255)
                .uv(1, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light)
                .normal(0, 0, 1).endVertex();
        vb.vertex(m, -halfW, yBase, 0).color(255, 255, 255, 255)
                .uv(0, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light)
                .normal(0, 0, 1).endVertex();
    }

    private static ResourceLocation[] framesForType(int type) {
        return switch (type) {
            case VoidEffectEntity.TYPE_MIND_SPIKE     -> MIND_SPIKE_FRAMES;
            case VoidEffectEntity.TYPE_SOUL_TEAR      -> SOUL_TEAR_FRAMES;
            case VoidEffectEntity.TYPE_MADNESS_WAVE   -> MADNESS_WAVE_FRAMES;
            case VoidEffectEntity.TYPE_COSMIC_VOID    -> COSMIC_VOID_FRAMES;
            case VoidEffectEntity.TYPE_ELDRITCH_BLAST -> ELDRITCH_BLAST_FRAMES;
            default -> MIND_SPIKE_FRAMES;
        };
    }

    /** Cycle through frames based on entity age + type-specific play mode. */
    private static ResourceLocation pickFrame(VoidEffectEntity entity, ResourceLocation[] frames) {
        int total = entity.getTotalLifetime();
        int life = entity.getLifeRemaining();
        int age = total - life;
        int type = entity.getEffectType();

        return switch (type) {
            // Play once over lifetime
            case VoidEffectEntity.TYPE_MIND_SPIKE,
                 VoidEffectEntity.TYPE_MADNESS_WAVE,
                 VoidEffectEntity.TYPE_ELDRITCH_BLAST -> {
                int idx = Math.min(frames.length - 1, (age * frames.length) / Math.max(1, total));
                yield frames[idx];
            }
            // Loop continuously
            case VoidEffectEntity.TYPE_SOUL_TEAR,
                 VoidEffectEntity.TYPE_COSMIC_VOID -> frames[(age / 2) % frames.length];
            default -> frames[0];
        };
    }

    @Override
    public ResourceLocation getTextureLocation(VoidEffectEntity entity) {
        return pickFrame(entity, framesForType(entity.getEffectType()));
    }
}
