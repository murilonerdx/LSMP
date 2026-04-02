package br.com.murilo.liberthia.client.renderer;

import br.com.murilo.liberthia.entity.BlackHoleEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class BlackHoleRenderer extends EntityRenderer<BlackHoleEntity> {
    public BlackHoleRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(BlackHoleEntity entity) {
        return new ResourceLocation("liberthia", "textures/entity/black_hole.png");
    }

    @Override
    public void render(BlackHoleEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        // Simple invisible/particle based renderer for now since it's a black hole
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }
}
