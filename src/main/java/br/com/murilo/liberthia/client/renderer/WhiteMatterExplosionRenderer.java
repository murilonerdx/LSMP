package br.com.murilo.liberthia.client.renderer;

import br.com.murilo.liberthia.entity.WhiteMatterExplosionEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class WhiteMatterExplosionRenderer extends EntityRenderer<WhiteMatterExplosionEntity> {

    public WhiteMatterExplosionRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(WhiteMatterExplosionEntity entity) {
        return new ResourceLocation("liberthia", "textures/entity/white_matter_explosion.png");
    }

    @Override
    public void render(WhiteMatterExplosionEntity entity, float entityYaw, float partialTicks,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }
}
