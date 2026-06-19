package br.com.murilo.liberthia.magic.spell.voidspell;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/**
 * v0.1.152 r120: <b>MiniBlackHoleRenderer</b> — desenha disco escuro 3D billboard
 * com halo roxo + rotação. Sempre face ao viewer.
 */
public class MiniBlackHoleRenderer extends EntityRenderer<MiniBlackHoleEntity> {

    private static final ResourceLocation TEXTURE = new ResourceLocation(
            "liberthia", "textures/entity/mini_black_hole.png");

    public MiniBlackHoleRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.shadowRadius = 1.0F;
    }

    @Override
    public ResourceLocation getTextureLocation(MiniBlackHoleEntity e) {
        return TEXTURE;
    }

    @Override
    public void render(MiniBlackHoleEntity entity, float entityYaw, float partialTick,
                        PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);

        float scale = entity.getScale();
        int age = entity.getAge();
        float rotation = age * 8F + partialTick * 8F;

        poseStack.pushPose();
        // Billboard: face camera
        Camera cam = Minecraft.getInstance().gameRenderer.getMainCamera();
        poseStack.mulPose(Axis.YP.rotationDegrees(-cam.getYRot()));
        poseStack.mulPose(Axis.XP.rotationDegrees(cam.getXRot()));
        poseStack.mulPose(Axis.ZP.rotationDegrees(rotation));
        poseStack.scale(scale, scale, scale);

        VertexConsumer vc = buffer.getBuffer(RenderType.entityTranslucent(TEXTURE));
        // Quad com texture full
        var pose = poseStack.last().pose();
        var normal = poseStack.last().normal();
        float half = 1.0F;
        // bottom-left
        vc.vertex(pose, -half, -half, 0).color(255, 255, 255, 220)
                .uv(0, 1).overlayCoords(0).uv2(packedLight).normal(normal, 0, 0, 1).endVertex();
        // bottom-right
        vc.vertex(pose, half, -half, 0).color(255, 255, 255, 220)
                .uv(1, 1).overlayCoords(0).uv2(packedLight).normal(normal, 0, 0, 1).endVertex();
        // top-right
        vc.vertex(pose, half, half, 0).color(255, 255, 255, 220)
                .uv(1, 0).overlayCoords(0).uv2(packedLight).normal(normal, 0, 0, 1).endVertex();
        // top-left
        vc.vertex(pose, -half, half, 0).color(255, 255, 255, 220)
                .uv(0, 0).overlayCoords(0).uv2(packedLight).normal(normal, 0, 0, 1).endVertex();

        poseStack.popPose();
    }
}
