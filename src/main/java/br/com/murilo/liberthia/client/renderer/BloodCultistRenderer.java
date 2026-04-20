package br.com.murilo.liberthia.client.renderer;

import br.com.murilo.liberthia.entity.BloodCultistEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

public class BloodCultistRenderer
        extends HumanoidMobRenderer<BloodCultistEntity, HumanoidModel<BloodCultistEntity>> {

    private static final ResourceLocation TEX =
            new ResourceLocation("liberthia", "textures/entity/blood_cultist.png");

    public BloodCultistRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER)), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(BloodCultistEntity entity) { return TEX; }
}
