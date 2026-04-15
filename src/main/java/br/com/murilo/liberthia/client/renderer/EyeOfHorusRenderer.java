package br.com.murilo.liberthia.client.renderer;

import br.com.murilo.liberthia.entity.EyeOfHorusEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * Renderer for the Eye of Horus — renders as a glowing billboard sprite,
 * similar to the ender dragon's death effect or the end crystal beam.
 * A floating purple eye that always faces the player.
 */
public class EyeOfHorusRenderer extends EntityRenderer<EyeOfHorusEntity> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation("liberthia", "textures/entity/eye_of_horus.png");

    public EyeOfHorusRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(EyeOfHorusEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();

        // Bob up and down
        float bob = (float) Math.sin((entity.tickCount + partialTick) * 0.08) * 0.15F;
        poseStack.translate(0, bob + 0.5, 0);

        // Face the camera (billboard)
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));

        // Scale
        float scale = 1.5F;
        poseStack.scale(scale, scale, scale);

        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.entityTranslucent(TEXTURE));
        PoseStack.Pose pose = poseStack.last();
        Matrix4f matrix = pose.pose();
        Matrix3f normal = pose.normal();

        float alpha = 0.7F + (float) Math.sin((entity.tickCount + partialTick) * 0.15) * 0.2F;

        // Render a simple quad
        vertex(vertexConsumer, matrix, normal, -0.5F, -0.5F, 0, 0, 1, packedLight, alpha);
        vertex(vertexConsumer, matrix, normal,  0.5F, -0.5F, 0, 1, 1, packedLight, alpha);
        vertex(vertexConsumer, matrix, normal,  0.5F,  0.5F, 0, 1, 0, packedLight, alpha);
        vertex(vertexConsumer, matrix, normal, -0.5F,  0.5F, 0, 0, 0, packedLight, alpha);

        poseStack.popPose();

        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    private static void vertex(VertexConsumer consumer, Matrix4f matrix, Matrix3f normal,
                               float x, float y, float z, float u, float v, int light, float alpha) {
        consumer.vertex(matrix, x, y, z)
                .color(1.0F, 1.0F, 1.0F, alpha)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(0xF000F0) // Full brightness
                .normal(normal, 0.0F, 1.0F, 0.0F)
                .endVertex();
    }

    @Override
    public ResourceLocation getTextureLocation(EyeOfHorusEntity entity) {
        return TEXTURE;
    }
}
