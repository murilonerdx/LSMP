package br.com.murilo.liberthia.client.renderer;

import br.com.murilo.liberthia.entity.DarkMatterSporeEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class DarkMatterSporeRenderer extends EntityRenderer<DarkMatterSporeEntity> {
    public DarkMatterSporeRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(DarkMatterSporeEntity entity) {
        return new ResourceLocation("liberthia", "textures/entity/spore.png");
    }

    @Override
    public void render(DarkMatterSporeEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        // Invisible anomaly - heavily relies on internal tick particles for visual representation.
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }
}
