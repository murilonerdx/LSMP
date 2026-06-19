package br.com.murilo.liberthia.magic.spell.voidspell;

import br.com.murilo.liberthia.LiberthiaMod;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

/**
 * r165: <b>VoidTentacleRenderer</b> — billboard sprite renderer com flipbook
 * animation, baseado no pattern do Iron's Spellbooks AcidOrbRenderer.
 *
 * <p>Carrega 8 PNGs como frames de animação, cicla baseado em
 * {@code entity.tickCount / 2 % FRAMES.length}.
 *
 * <p>Render: quad billboard (sempre face pra câmera) com transparent texture
 * (entityTranslucent). Sprite é 32×64 (proporção 1:2 vertical pra tentáculo alto).
 */
public class VoidTentacleRenderer extends EntityRenderer<VoidTentacleEntity> {

    private static final ResourceLocation[] FRAMES = new ResourceLocation[8];
    static {
        for (int i = 0; i < 8; i++) {
            FRAMES[i] = new ResourceLocation(LiberthiaMod.MODID,
                    "textures/entity/void_tentacle/tentacle_" + i + ".png");
        }
    }

    public VoidTentacleRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.shadowRadius = 0.4F;
    }

    @Override
    public void render(VoidTentacleEntity entity, float yaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        ResourceLocation tex = pickFrame(entity);

        poseStack.pushPose();
        // Billboard — face the camera
        poseStack.mulPose(Axis.YP.rotationDegrees(-this.entityRenderDispatcher.camera.getYRot()));
        poseStack.mulPose(Axis.XP.rotationDegrees(this.entityRenderDispatcher.camera.getXRot()));

        // Scale 0..1 based on growth phase (0..15 ticks: scaling up, 65-80: scaling down)
        float scaleY = computeYScale(entity);

        // 32×64 sprite — render as 2 blocks tall by 1 block wide (centered)
        float halfW = 0.6F;
        float halfH = 1.6F * scaleY;
        float yBase = -0.1F;  // small offset so feet root is at entity Y

        VertexConsumer vb = buffer.getBuffer(RenderType.entityTranslucent(tex));
        Matrix4f m = poseStack.last().pose();

        // Render quad (two triangles via 4 verts)
        // UVs: full texture (0..1)
        vb.vertex(m, -halfW, yBase + halfH * 2, 0).color(255, 255, 255, 255)
                .uv(0, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight)
                .normal(0, 0, 1).endVertex();
        vb.vertex(m, halfW, yBase + halfH * 2, 0).color(255, 255, 255, 255)
                .uv(1, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight)
                .normal(0, 0, 1).endVertex();
        vb.vertex(m, halfW, yBase, 0).color(255, 255, 255, 255)
                .uv(1, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight)
                .normal(0, 0, 1).endVertex();
        vb.vertex(m, -halfW, yBase, 0).color(255, 255, 255, 255)
                .uv(0, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight)
                .normal(0, 0, 1).endVertex();

        poseStack.popPose();
        super.render(entity, yaw, partialTick, poseStack, buffer, packedLight);
    }

    /** Frame selection — cycle through 8 frames at 2-tick per frame during active phase. */
    private static ResourceLocation pickFrame(VoidTentacleEntity entity) {
        int age = VoidTentacleEntity.TOTAL_LIFE - entity.getLifeRemaining();
        // Windup (0-15): play frames 0→3 once
        if (age < 15) {
            int idx = Math.min(3, age / 4);
            return FRAMES[idx];
        }
        // Retract (65-80): play frames 7→3 in reverse
        if (age > 65) {
            int back = age - 65;
            int idx = Math.max(3, 7 - back / 3);
            return FRAMES[idx];
        }
        // Active loop 15..65: cycle 3→7→3 at ~2-tick rate
        int cycleFrame = (age / 2) % 5;  // 0..4
        int idx = 3 + cycleFrame;        // 3..7
        return FRAMES[Math.min(7, idx)];
    }

    /** Y-scale animation: 0 → 1 during windup, 1.0 active, 1 → 0 retraction. */
    private static float computeYScale(VoidTentacleEntity entity) {
        int age = VoidTentacleEntity.TOTAL_LIFE - entity.getLifeRemaining();
        if (age < 15) return age / 15F;
        if (age > 65) return Math.max(0, 1F - (age - 65) / 15F);
        return 1F;
    }

    @Override
    public ResourceLocation getTextureLocation(VoidTentacleEntity entity) {
        return pickFrame(entity);
    }
}
