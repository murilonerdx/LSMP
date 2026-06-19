package br.com.murilo.liberthia.magic.familiar;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/**
 * v0.1.160 r135: <b>DrygmyRenderer</b> — renderer mínimo que desenha 1 quad
 * billboard verde com a textura.
 *
 * <p>Approach simples (evita criar EntityModel custom). Visualmente é uma
 * spritezinha 3D fofa.
 */
public class DrygmyRenderer extends EntityRenderer<DrygmyEntity> {

    private static final ResourceLocation TEXTURE = new ResourceLocation(
            "liberthia", "textures/entity/drygmy.png");

    public DrygmyRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.shadowRadius = 0.4F;
    }

    @Override
    public ResourceLocation getTextureLocation(DrygmyEntity entity) {
        return TEXTURE;
    }

    @Override
    public void render(DrygmyEntity entity, float entityYaw, float partialTicks,
                        PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);

        poseStack.pushPose();
        // Billboard face camera
        var cam = Minecraft.getInstance().gameRenderer.getMainCamera();
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-cam.getYRot()));
        poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(cam.getXRot()));
        poseStack.translate(0, 0.6, 0);
        // Bob up/down
        float bob = (float) Math.sin(entity.tickCount * 0.15) * 0.05F;
        poseStack.translate(0, bob, 0);
        poseStack.scale(0.6F, 0.6F, 0.6F);

        VertexConsumer vc = buffer.getBuffer(RenderType.entityCutout(TEXTURE));
        var pose = poseStack.last().pose();
        var normal = poseStack.last().normal();
        float half = 0.5F;
        // Quad (front face)
        vc.vertex(pose, -half, -half, 0).color(255, 255, 255, 255)
                .uv(0, 1).overlayCoords(0).uv2(packedLight).normal(normal, 0, 0, 1).endVertex();
        vc.vertex(pose, half, -half, 0).color(255, 255, 255, 255)
                .uv(1, 1).overlayCoords(0).uv2(packedLight).normal(normal, 0, 0, 1).endVertex();
        vc.vertex(pose, half, half, 0).color(255, 255, 255, 255)
                .uv(1, 0).overlayCoords(0).uv2(packedLight).normal(normal, 0, 0, 1).endVertex();
        vc.vertex(pose, -half, half, 0).color(255, 255, 255, 255)
                .uv(0, 0).overlayCoords(0).uv2(packedLight).normal(normal, 0, 0, 1).endVertex();

        poseStack.popPose();
    }
}
