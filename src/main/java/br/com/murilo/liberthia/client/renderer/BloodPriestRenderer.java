package br.com.murilo.liberthia.client.renderer;

import br.com.murilo.liberthia.entity.BloodPriestEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

public class BloodPriestRenderer
        extends HumanoidMobRenderer<BloodPriestEntity, HumanoidModel<BloodPriestEntity>> {

    private static final ResourceLocation TEX =
            new ResourceLocation("liberthia", "textures/entity/blood_priest.png");

    public BloodPriestRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER)), 0.6F);
    }

    @Override
    public ResourceLocation getTextureLocation(BloodPriestEntity entity) { return TEX; }

    @Override
    protected void scale(BloodPriestEntity entity, com.mojang.blaze3d.vertex.PoseStack pose, float partial) {
        pose.scale(1.1F, 1.1F, 1.1F);
    }
}
