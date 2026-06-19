package br.com.murilo.liberthia.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * r180: renderer billboard genérico — desenha uma textura como quad sempre de frente
 * pra câmera (útil pra entidades "sprite" como o Amalgamado Cego e o Olho Parasita).
 */
public class BillboardEntityRenderer<T extends Entity> extends EntityRenderer<T> {

    private final ResourceLocation tex;
    private final float halfW;
    private final float halfH;
    private final float yOffset;

    public BillboardEntityRenderer(EntityRendererProvider.Context ctx, ResourceLocation tex,
                                   float width, float height, float yOffset) {
        super(ctx);
        this.tex = tex;
        this.halfW = width * 0.5F;
        this.halfH = height * 0.5F;
        this.yOffset = yOffset;
        this.shadowRadius = 0.0F;
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) { return tex; }

    @Override
    public void render(T entity, float yaw, float partial, PoseStack ps, MultiBufferSource buffer, int light) {
        float s = (entity instanceof br.com.murilo.liberthia.cosmic.IScalableBillboard b) ? Math.max(0.1F, b.billboardScale()) : 1.0F;
        float halfW = this.halfW * s, halfH = this.halfH * s;
        ps.pushPose();
        ps.translate(0, halfH + yOffset, 0);
        ps.mulPose(this.entityRenderDispatcher.cameraOrientation());
        Matrix4f m = ps.last().pose();
        Matrix3f n = ps.last().normal();
        VertexConsumer vc = buffer.getBuffer(RenderType.entityCutoutNoCull(tex));
        vert(vc, m, n, -halfW, -halfH, 0F, 1F, light);
        vert(vc, m, n,  halfW, -halfH, 1F, 1F, light);
        vert(vc, m, n,  halfW,  halfH, 1F, 0F, light);
        vert(vc, m, n, -halfW,  halfH, 0F, 0F, light);
        ps.popPose();
        super.render(entity, yaw, partial, ps, buffer, light);
    }

    private static void vert(VertexConsumer vc, Matrix4f m, Matrix3f n, float x, float y, float u, float v, int light) {
        vc.vertex(m, x, y, 0F).color(255, 255, 255, 255).uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(n, 0F, 0F, 1F).endVertex();
    }
}
