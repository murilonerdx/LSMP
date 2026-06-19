package br.com.murilo.liberthia.client.renderer;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.dimension.DimensionalRiftEntity;
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
 * r179: renderiza a {@link DimensionalRiftEntity} como um <b>rasgo billboard</b>
 * (sempre de frente pra câmera): a textura do rasgo, translúcida e auto-iluminada
 * (glow), abrindo de uma fenda fina (escala X cresce com {@code getOpen()}) +
 * leve shimmer vertical. Parece um corte no ar mostrando outra dimensão.
 */
public class DimensionalRiftRenderer extends EntityRenderer<DimensionalRiftEntity> {

    private static final ResourceLocation TEX =
            new ResourceLocation(LiberthiaMod.MODID, "textures/entity/dimensional_rift.png");

    private static final float W = 2.1F;
    private static final float H = 2.5F;

    public DimensionalRiftRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.shadowRadius = 0.0F;
    }

    @Override
    public ResourceLocation getTextureLocation(DimensionalRiftEntity e) {
        return TEX;
    }

    @Override
    public void render(DimensionalRiftEntity entity, float yaw, float partial,
                       PoseStack ps, MultiBufferSource buffer, int packedLight) {
        float open = entity.getOpen();
        if (open <= 0.001F) return;

        float age = entity.getAge() + partial;
        float shimmer = 0.96F + 0.04F * Mth.sin(age * 0.25F);
        float sx = open;                          // abre lateralmente (de uma fresta)
        float sy = Math.min(1.0F, open * 1.4F) * shimmer;
        int alpha = (int) (255 * Mth.clamp(open, 0F, 1F));

        ps.pushPose();
        ps.translate(0, H * 0.5F, 0);             // centro na altura
        ps.mulPose(this.entityRenderDispatcher.cameraOrientation()); // billboard
        ps.scale(W * sx, H * sy, 1.0F);

        Matrix4f m = ps.last().pose();
        Matrix3f n = ps.last().normal();
        VertexConsumer vc = buffer.getBuffer(RenderType.entityTranslucentEmissive(TEX));
        int fullLight = 0x00F000F0; // 15728880 — auto-iluminado (glow)

        quad(vc, m, n, fullLight, alpha);

        ps.popPose();
        super.render(entity, yaw, partial, ps, buffer, packedLight);
    }

    /** Quad billboard centrado (-0.5..0.5) com a textura completa. */
    private static void quad(VertexConsumer vc, Matrix4f m, Matrix3f n, int light, int a) {
        vert(vc, m, n, -0.5F, -0.5F, 0F, 1F, a, light);
        vert(vc, m, n,  0.5F, -0.5F, 1F, 1F, a, light);
        vert(vc, m, n,  0.5F,  0.5F, 1F, 0F, a, light);
        vert(vc, m, n, -0.5F,  0.5F, 0F, 0F, a, light);
    }

    private static void vert(VertexConsumer vc, Matrix4f m, Matrix3f n,
                             float x, float y, float u, float v, int a, int light) {
        vc.vertex(m, x, y, 0F)
                .color(255, 255, 255, a)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(n, 0F, 0F, 1F)
                .endVertex();
    }
}
