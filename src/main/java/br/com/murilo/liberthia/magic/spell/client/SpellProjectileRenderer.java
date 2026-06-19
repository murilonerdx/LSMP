package br.com.murilo.liberthia.magic.spell.client;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.magic.school.SpellSchool;
import br.com.murilo.liberthia.magic.spell.SpellProjectileEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

/**
 * v0.1.145 r113: <b>SpellProjectileRenderer</b> — desenha o projétil como
 * 2 quads cruzados billboarded (efeito de "orb" 3D barato mas convincente)
 * usando a textura branca tinted pela cor da escola.
 *
 * <p>Render type: {@link RenderType#eyes(ResourceLocation)} — emissive, full-bright,
 * additive blending. Combinado com o trail de partículas spritesheet, o
 * projétil parece um orb fulgurante voando.
 *
 * <p>Original code, escrito do zero usando API pública {@link EntityRenderer}.
 */
public class SpellProjectileRenderer extends EntityRenderer<SpellProjectileEntity> {

    /** Textura branca de glow — vamos tintando pela cor da escola via vertex color. */
    private static final ResourceLocation GLOW_TEX =
            new ResourceLocation(LiberthiaMod.MODID, "textures/entity/spell_orb.png");

    public SpellProjectileRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.shadowRadius = 0F; // sem shadow
    }

    @Override
    public ResourceLocation getTextureLocation(SpellProjectileEntity entity) {
        return GLOW_TEX;
    }

    @Override
    public boolean shouldRender(SpellProjectileEntity entity, net.minecraft.client.renderer.culling.Frustum f,
                                double camX, double camY, double camZ) {
        return true;
    }

    @Override
    public void render(SpellProjectileEntity entity, float entityYaw, float partialTicks,
                       PoseStack pose, MultiBufferSource buffer, int packedLight) {
        super.render(entity, entityYaw, partialTicks, pose, buffer, packedLight);

        SpellSchool school = entity.getSchool();
        int hex = school.colorHex();
        float r = ((hex >> 16) & 0xFF) / 255F;
        float g = ((hex >> 8) & 0xFF) / 255F;
        float b = (hex & 0xFF) / 255F;

        // Tamanho pulsa entre 0.35 e 0.5 (sin wave de 12-tick period)
        float time = entity.tickCount + partialTicks;
        float pulse = 0.5F + 0.5F * Mth.sin(time * 0.5F);
        float baseSize = 0.4F + pulse * 0.15F;

        pose.pushPose();

        // Spin o orb sobre seu eixo Y (visual de energia girando)
        float spin = (time * 12F) % 360F;
        pose.mulPose(Axis.YP.rotationDegrees(spin));

        // Camada 1: outer aura (maior, alpha baixo)
        renderBillboard(pose, buffer, baseSize * 2.0F, r, g, b, 0.25F);
        // Camada 2: inner core (menor, brilhante)
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(45F)); // offset visual
        renderBillboard(pose, buffer, baseSize * 1.2F, r, g, b, 0.6F);
        pose.popPose();
        // Camada 3: hot center (branco)
        renderBillboard(pose, buffer, baseSize * 0.5F, 1.0F, 1.0F, 1.0F, 0.95F);

        pose.popPose();
    }

    /** Desenha um quad billboarded (sempre virado pra câmera) tinted RGBA. */
    private void renderBillboard(PoseStack pose, MultiBufferSource buffer,
                                  float size, float r, float g, float b, float a) {
        VertexConsumer vc = buffer.getBuffer(RenderType.entityTranslucentEmissive(GLOW_TEX));

        // Pega quaternion da câmera pra orientar o quad
        Camera cam = net.minecraft.client.Minecraft.getInstance().gameRenderer.getMainCamera();
        Quaternionf camRot = cam.rotation();

        pose.pushPose();
        pose.mulPose(camRot);

        Matrix4f m = pose.last().pose();
        int color = ((int)(a * 255) << 24) | ((int)(r * 255) << 16) | ((int)(g * 255) << 8) | (int)(b * 255);

        float s = size;
        // 4 vertices do quad — face única, billboarded
        vc.vertex(m, -s, -s, 0).color(color).uv(0, 1).overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(15728880).normal(0, 0, 1).endVertex();
        vc.vertex(m,  s, -s, 0).color(color).uv(1, 1).overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(15728880).normal(0, 0, 1).endVertex();
        vc.vertex(m,  s,  s, 0).color(color).uv(1, 0).overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(15728880).normal(0, 0, 1).endVertex();
        vc.vertex(m, -s,  s, 0).color(color).uv(0, 0).overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(15728880).normal(0, 0, 1).endVertex();

        pose.popPose();
    }
}
