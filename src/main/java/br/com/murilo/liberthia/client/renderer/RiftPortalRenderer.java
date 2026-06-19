package br.com.murilo.liberthia.client.renderer;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.entity.RiftPortalEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * r185 — renderiza o {@link RiftPortalEntity} como um rasgo billboard VERDE (mesma técnica da
 * fenda dimensional, mas tingido), auto-iluminado e translúcido, abrindo de uma fresta.
 */
public class RiftPortalRenderer extends EntityRenderer<RiftPortalEntity> {
    private static final ResourceLocation TEX =
            new ResourceLocation(LiberthiaMod.MODID, "textures/entity/dimensional_rift.png");
    private static final float W = 1.9F, H = 2.2F;

    public RiftPortalRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.shadowRadius = 0.0F;
    }

    @Override public ResourceLocation getTextureLocation(RiftPortalEntity e) { return TEX; }

    @Override
    public void render(RiftPortalEntity entity, float yaw, float partial, PoseStack ps,
                       MultiBufferSource buffer, int packedLight) {
        float open = entity.getOpen();
        if (open <= 0.001F) return;
        float age = entity.getAge() + partial;
        float shimmer = 0.96F + 0.04F * Mth.sin(age * 0.25F);
        float sx = open;
        float sy = Math.min(1.0F, open * 1.4F) * shimmer;
        int alpha = (int) (255 * Mth.clamp(open, 0F, 1F));

        ps.pushPose();
        ps.translate(0, H * 0.5F, 0);
        ps.mulPose(this.entityRenderDispatcher.cameraOrientation());
        ps.scale(W * sx, H * sy, 1.0F);
        Matrix4f m = ps.last().pose();
        Matrix3f n = ps.last().normal();
        VertexConsumer vc = buffer.getBuffer(RenderType.entityTranslucentEmissive(TEX));
        int fullLight = 0x00F000F0;
        quad(vc, m, n, fullLight, alpha);
        ps.popPose();
        super.render(entity, yaw, partial, ps, buffer, packedLight);
    }

    private static void quad(VertexConsumer vc, Matrix4f m, Matrix3f n, int light, int a) {
        vert(vc, m, n, -0.5F, -0.5F, 0F, 1F, a, light);
        vert(vc, m, n,  0.5F, -0.5F, 1F, 1F, a, light);
        vert(vc, m, n,  0.5F,  0.5F, 1F, 0F, a, light);
        vert(vc, m, n, -0.5F,  0.5F, 0F, 0F, a, light);
    }

    private static void vert(VertexConsumer vc, Matrix4f m, Matrix3f n,
                             float x, float y, float u, float v, int a, int light) {
        vc.vertex(m, x, y, 0F)
                .color(70, 255, 140, a) // tinta verde
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(n, 0F, 0F, 1F)
                .endVertex();
    }
}
