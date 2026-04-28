package br.com.murilo.liberthia.client.renderer;

import br.com.murilo.liberthia.entity.BloodMageEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

public class BloodMageRenderer
        extends HumanoidMobRenderer<BloodMageEntity, HumanoidModel<BloodMageEntity>> {

    private static final ResourceLocation TEX =
            new ResourceLocation("liberthia", "textures/entity/blood_mage.png");

    public BloodMageRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER)), 0.55F);
    }

    @Override
    public ResourceLocation getTextureLocation(BloodMageEntity entity) { return TEX; }

    @Override
    protected void scale(BloodMageEntity entity, com.mojang.blaze3d.vertex.PoseStack pose, float partial) {
        pose.scale(1.05F, 1.05F, 1.05F);
    }
}
