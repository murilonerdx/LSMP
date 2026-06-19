package br.com.murilo.liberthia.cosmic.lurker;

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
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * r173: Renderer do Lurker — billboard de um rosto de terror que sempre encara
 * o jogador, brilho total (visível no escuro), some em 2s.
 */
public class LurkerRenderer extends EntityRenderer<LurkerEntity> {

    public LurkerRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.shadowRadius = 0.0F;
    }

    private static ResourceLocation faceTex(int face) {
        int idx = Math.floorMod(face, 6) + 1;
        return new ResourceLocation(LiberthiaMod.MODID, "textures/gui/scare/face_" + idx + ".png");
    }

    @Override
    public void render(LurkerEntity entity, float yaw, float partialTick,
                       PoseStack ps, MultiBufferSource buffer, int packedLight) {
        ps.pushPose();
        ps.translate(0, 1.2, 0);
        ps.mulPose(this.entityRenderDispatcher.cameraOrientation());
        ps.mulPose(Axis.YP.rotationDegrees(180.0F));
        float scale = 2.2F;
        ps.scale(scale, scale, scale);

        VertexConsumer vc = buffer.getBuffer(RenderType.entityTranslucent(faceTex(entity.getFace())));
        PoseStack.Pose pose = ps.last();
        Matrix4f m = pose.pose();
        Matrix3f n = pose.normal();
        // fade-out nos últimos 8 ticks
        int remaining = LurkerEntity.MAX_LIFE - entity.getLife();
        float alpha = remaining < 8 ? Math.max(0F, remaining / 8F) : 1.0F;

        vertex(vc, m, n, -0.5F, -0.5F, 0, 0, 1, alpha);
        vertex(vc, m, n,  0.5F, -0.5F, 0, 1, 1, alpha);
        vertex(vc, m, n,  0.5F,  0.5F, 0, 1, 0, alpha);
        vertex(vc, m, n, -0.5F,  0.5F, 0, 0, 0, alpha);

        ps.popPose();
        super.render(entity, yaw, partialTick, ps, buffer, packedLight);
    }

    private static void vertex(VertexConsumer c, Matrix4f m, Matrix3f n,
                               float x, float y, float z, float u, float v, float alpha) {
        c.vertex(m, x, y, z).color(1F, 1F, 1F, alpha).uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(0xF000F0)
                .normal(n, 0F, 1F, 0F).endVertex();
    }

    @Override
    public ResourceLocation getTextureLocation(LurkerEntity entity) {
        return faceTex(entity.getFace());
    }
}
