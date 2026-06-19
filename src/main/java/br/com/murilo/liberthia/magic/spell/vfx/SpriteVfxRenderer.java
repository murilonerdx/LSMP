package br.com.murilo.liberthia.magic.spell.vfx;

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
 * r166: <b>SpriteVfxRenderer</b> — billboard renderer for {@link SpriteVfxEntity}.
 *
 * <p>For each entity, reads its {@link SpriteVfxRegistry.Type}, computes the
 * current frame from entity age, picks the correct PNG out of the per-effect
 * frame array, and renders it as a single quad.
 *
 * <p>Three orientation modes (per type's {@code orientation}):
 * <ul>
 *   <li><b>BILLBOARD</b> — quad always faces the camera</li>
 *   <li><b>GROUND_FLAT</b> — quad lays flat on the ground (rotated 90° on X)</li>
 *   <li><b>MOTION_AXIS</b> — quad aligned to entity yaw (useful for flame lashes)</li>
 * </ul>
 *
 * <p>Frame size scales with both the type's base {@code scale} and the entity's
 * per-instance {@code scaleMul}, so different spells reuse the same VFX type at
 * different sizes (e.g. cosmic_void at 2.5×, magic_spell at 1.0×).
 */
public class SpriteVfxRenderer extends EntityRenderer<SpriteVfxEntity> {

    public SpriteVfxRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.shadowRadius = 0F;
    }

    @Override
    public void render(SpriteVfxEntity entity, float yaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        // r167: dispatch pack-1 vs pack-2
        if (entity.getPack() == SpriteVfxEntity.PACK_2) {
            renderPack2(entity, poseStack, buffer, packedLight);
        } else {
            renderPack1(entity, poseStack, buffer, packedLight);
        }
        super.render(entity, yaw, partialTick, poseStack, buffer, packedLight);
    }

    private void renderPack1(SpriteVfxEntity entity, PoseStack poseStack,
                              MultiBufferSource buffer, int packedLight) {
        SpriteVfxRegistry.Type type = entity.getVfxType();
        SpriteVfxRegistry.Effect effect = type.effect;
        int frameIdx = effect.frameAt(entity.getAge());
        ResourceLocation tex = effect.frame(frameIdx);
        float totalScale = effect.scale * entity.getScaleMul();

        poseStack.pushPose();
        switch (effect.orientation) {
            case BILLBOARD -> billboardFace(poseStack);
            case GROUND_FLAT -> poseStack.mulPose(Axis.XP.rotationDegrees(90));
            case MOTION_AXIS -> poseStack.mulPose(Axis.YP.rotationDegrees(-entity.getYRot()));
        }
        float halfW = totalScale, halfH = totalScale;
        float yBase = effect.orientation == SpriteVfxRegistry.Orientation.GROUND_FLAT ? 0.01F : 0F;
        renderQuad(poseStack, buffer, tex, packedLight, halfW, halfH, yBase);
        poseStack.popPose();
    }

    /** r167: Pack-2 renderer — always billboard, uses {@link Pack2VfxCatalog}. */
    private void renderPack2(SpriteVfxEntity entity, PoseStack poseStack,
                              MultiBufferSource buffer, int packedLight) {
        Pack2VfxCatalog.Type type = entity.getPack2Type();
        int frameIdx = type.frameAt(entity.getAge());
        ResourceLocation tex = type.frame(frameIdx, entity.getColorIdx());
        float totalScale = type.scale * entity.getScaleMul();

        poseStack.pushPose();
        billboardFace(poseStack);
        renderQuad(poseStack, buffer, tex, packedLight, totalScale, totalScale, 0F);
        poseStack.popPose();
    }

    private void billboardFace(PoseStack ps) {
        ps.mulPose(Axis.YP.rotationDegrees(-this.entityRenderDispatcher.camera.getYRot()));
        ps.mulPose(Axis.XP.rotationDegrees(this.entityRenderDispatcher.camera.getXRot()));
    }

    private void renderQuad(PoseStack ps, MultiBufferSource buf, ResourceLocation tex,
                            int light, float halfW, float halfH, float yBase) {
        VertexConsumer vb = buf.getBuffer(RenderType.entityTranslucent(tex));
        Matrix4f m = ps.last().pose();
        // Quad centered horizontally, base at yBase, going up by 2*halfH
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

    @Override
    public ResourceLocation getTextureLocation(SpriteVfxEntity entity) {
        if (entity.getPack() == SpriteVfxEntity.PACK_2) {
            Pack2VfxCatalog.Type t = entity.getPack2Type();
            return t.frame(t.frameAt(entity.getAge()), entity.getColorIdx());
        }
        SpriteVfxRegistry.Effect e = entity.getVfxType().effect;
        return e.frame(e.frameAt(entity.getAge()));
    }
}
