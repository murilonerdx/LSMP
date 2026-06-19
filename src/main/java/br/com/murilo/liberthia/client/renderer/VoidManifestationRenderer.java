package br.com.murilo.liberthia.client.renderer;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.cosmic.horror.entity.VoidManifestationEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

/**
 * <b>VoidManifestationRenderer</b> — a sombra viva do Alex's Caves recriada com
 * billboards: várias camadas de FUMAÇA PRETA girando em sentidos/velocidades
 * diferentes (parecem uma nuvem caótica), + 3 OLHOS VERMELHOS que orbitam e cuja
 * própria textura gira, + tentáculos finos saindo da massa.
 *
 * <p>Tudo billboard (sempre face pra câmera), translúcido aditivo, escala/alpha
 * controlados pelo envelope {@link VoidManifestationEntity#getProgress()}.
 */
public class VoidManifestationRenderer extends EntityRenderer<VoidManifestationEntity> {

    private static final ResourceLocation SMOKE =
            new ResourceLocation(LiberthiaMod.MODID, "textures/entity/void_manifestation/smoke.png");
    private static final ResourceLocation EYE =
            new ResourceLocation(LiberthiaMod.MODID, "textures/entity/void_manifestation/eye.png");
    private static final ResourceLocation[] TENTACLE = new ResourceLocation[4];
    static {
        for (int i = 0; i < 4; i++)
            TENTACLE[i] = new ResourceLocation(LiberthiaMod.MODID,
                    "textures/entity/void_manifestation/tentacle_" + i + ".png");
    }

    public VoidManifestationRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(VoidManifestationEntity entity, float yaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float prog = entity.getProgress();
        if (prog <= 0.001F) return;

        float age = entity.getAge() + partialTick;
        float pulse = 0.93F + 0.07F * Mth.sin(age * 0.2F);     // pulsar SUTIL (massa estável)
        float baseScale = (1.5F + 1.2F * prog) * pulse;        // tamanho da massa
        int alpha = (int) (255 * Mth.clamp(prog, 0F, 1F));

        // ── 1) TENTÁCULOS PRIMEIRO (atrás da fumaça): saem da base pra fora ──
        // Não billboard puro — usam só o yaw da câmera, ficando "em pé" no mundo,
        // pra parecer que erguem do chão/escuridão.
        poseStack.pushPose();
        poseStack.translate(0, 0.1F, 0);
        poseStack.mulPose(Axis.YP.rotationDegrees(-this.entityRenderDispatcher.camera.getYRot()));
        VertexConsumer tent = buffer.getBuffer(RenderType.entityTranslucent(TENTACLE[((int) (age * 0.25F)) & 3]));
        int tentacles = 5;
        for (int i = 0; i < tentacles; i++) {
            float spread = (i - (tentacles - 1) / 2f) * 22F;   // leque
            float sway = Mth.sin(age * 0.12F + i * 1.3F) * 14F; // balanço
            int ta = Mth.clamp((int) (alpha * 0.9F), 0, 255);
            float len = baseScale * (1.0F + 0.12F * (i % 2));
            drawTentacle(tent, poseStack, spread + sway, len,
                    (i - (tentacles - 1) / 2f) * 0.35F, 0.0F, ta, packedLight);
        }
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0, 1.2F, 0); // centro da massa
        // billboard: vira tudo pra câmera
        poseStack.mulPose(Axis.YP.rotationDegrees(-this.entityRenderDispatcher.camera.getYRot()));
        poseStack.mulPose(Axis.XP.rotationDegrees(this.entityRenderDispatcher.camera.getXRot()));

        // ── 2) NÚCLEO DE FUMAÇA: massa DENSA e estável — várias camadas quase paradas,
        //        giro bem lento, opacas no miolo (parece fumaça sólida, não partícula) ──
        VertexConsumer smoke = buffer.getBuffer(RenderType.entityTranslucent(SMOKE));
        int layers = 6;
        for (int i = 0; i < layers; i++) {
            float dir = (i % 2 == 0) ? 1F : -1F;
            float rot = age * 0.6F * dir + i * 53F;        // giro LENTO + offset fixo por camada
            float sc = baseScale * (0.85F + i * 0.14F);    // camadas concêntricas
            // miolo bem opaco, bordas mais leves → volume de fumaça
            int la = Mth.clamp((int) (alpha * (i < 3 ? 0.95F : 0.55F - (i - 3) * 0.12F)), 0, 255);
            float ox = Mth.cos(i * 1.7F + age * 0.05F) * 0.12F * baseScale; // leve deriva
            float oy = Mth.sin(i * 2.1F + age * 0.04F) * 0.10F * baseScale;
            drawBillboard(smoke, poseStack, rot, sc, ox, oy, 0, 0, la, packedLight);
        }

        // ── 3) OLHOS: 3 olhos quase parados, encarando (textura NÃO gira), piscam ──
        VertexConsumer eye = buffer.getBuffer(RenderType.entityTranslucent(EYE));
        float[][] eyePos = {                               // posições fixas dentro da massa
                {-0.55F, 0.18F}, {0.50F, 0.30F}, {0.10F, -0.35F}
        };
        for (int i = 0; i < eyePos.length; i++) {
            // leve flutuação local (não orbital), pra "vida" sem virar engrenagem
            float fx = eyePos[i][0] * baseScale + Mth.sin(age * 0.07F + i) * 0.05F * baseScale;
            float fy = eyePos[i][1] * baseScale + Mth.cos(age * 0.06F + i * 2F) * 0.05F * baseScale;
            // blink: encolhe a altura periodicamente (escala Y momentânea)
            float blinkPhase = Mth.sin(age * 0.08F + i * 2.5F);
            float blink = blinkPhase > 0.93F ? 0.15F : 1.0F; // pisca rápido às vezes
            float eyeScale = baseScale * 0.34F;
            drawEye(eye, poseStack, fx, fy, 0.06F, eyeScale, eyeScale * blink, alpha, packedLight);
        }

        poseStack.popPose();
        super.render(entity, yaw, partialTick, poseStack, buffer, packedLight);
    }

    /** Desenha um quad billboard (já em espaço de câmera), com rotação própria, escala e offset. */
    private void drawBillboard(VertexConsumer vb, PoseStack poseStack, float rotDeg, float scale,
                               float offX, float offY, float offZ, int unused, int alpha, int light) {
        poseStack.pushPose();
        poseStack.translate(offX, offY, offZ);
        poseStack.mulPose(Axis.ZP.rotationDegrees(rotDeg));
        float h = scale;
        Matrix4f m = poseStack.last().pose();
        quad(vb, m, h, alpha, light);
        poseStack.popPose();
    }

    /**
     * Olho: quad com largura e altura INDEPENDENTES (pro blink encolher só a altura),
     * SEM rotação de textura — o olho fica "em pé" encarando. halfW/halfH em blocos.
     */
    private void drawEye(VertexConsumer vb, PoseStack poseStack, float offX, float offY, float offZ,
                         float halfW, float halfH, int alpha, int light) {
        poseStack.pushPose();
        poseStack.translate(offX, offY, offZ);
        Matrix4f m = poseStack.last().pose();
        vb.vertex(m, -halfW, halfH, 0).color(255, 255, 255, alpha).uv(0, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(0, 0, 1).endVertex();
        vb.vertex(m, halfW, halfH, 0).color(255, 255, 255, alpha).uv(1, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(0, 0, 1).endVertex();
        vb.vertex(m, halfW, -halfH, 0).color(255, 255, 255, alpha).uv(1, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(0, 0, 1).endVertex();
        vb.vertex(m, -halfW, -halfH, 0).color(255, 255, 255, alpha).uv(0, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(0, 0, 1).endVertex();
        poseStack.popPose();
    }

    /**
     * Tentáculo: quad estreito e alto com a RAIZ ancorada embaixo (y=0) crescendo pra
     * cima — gira ao redor da base (rotDeg) pra parecer que ergue da escuridão pra fora.
     * Sprite tem o tip no topo (uv v=0) e a raiz embaixo (uv v=1).
     */
    private void drawTentacle(VertexConsumer vb, PoseStack poseStack, float rotDeg, float len,
                              float offX, float offY, int alpha, int light) {
        poseStack.pushPose();
        poseStack.translate(offX, offY, -0.02F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(rotDeg)); // rotaciona em torno da base
        Matrix4f m = poseStack.last().pose();
        float w = 0.16F * len, h = 2.2F * len; // raiz em y=0, topo em y=h
        vb.vertex(m, -w, h, 0).color(255, 255, 255, alpha).uv(0, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(0, 0, 1).endVertex();
        vb.vertex(m, w, h, 0).color(255, 255, 255, alpha).uv(1, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(0, 0, 1).endVertex();
        vb.vertex(m, w, 0, 0).color(255, 255, 255, alpha).uv(1, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(0, 0, 1).endVertex();
        vb.vertex(m, -w, 0, 0).color(255, 255, 255, alpha).uv(0, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(0, 0, 1).endVertex();
        poseStack.popPose();
    }

    /** Quad centrado de lado 2*h, branco com alpha (textura define a cor). */
    private static void quad(VertexConsumer vb, Matrix4f m, float h, int alpha, int light) {
        vb.vertex(m, -h, h, 0).color(255, 255, 255, alpha).uv(0, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(0, 0, 1).endVertex();
        vb.vertex(m, h, h, 0).color(255, 255, 255, alpha).uv(1, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(0, 0, 1).endVertex();
        vb.vertex(m, h, -h, 0).color(255, 255, 255, alpha).uv(1, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(0, 0, 1).endVertex();
        vb.vertex(m, -h, -h, 0).color(255, 255, 255, alpha).uv(0, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(0, 0, 1).endVertex();
    }

    @Override
    public ResourceLocation getTextureLocation(VoidManifestationEntity entity) {
        return SMOKE;
    }
}
