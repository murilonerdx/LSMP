package br.com.murilo.liberthia.client.renderer;

import br.com.murilo.liberthia.entity.DisarmerEntity;
import br.com.murilo.liberthia.entity.WeavingShadeEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

/** Big, grotesque humanoid — player UV scaled up 2.5x with a bloody skin. */
public class DisarmerRenderer
        extends HumanoidMobRenderer<DisarmerEntity, HumanoidModel<DisarmerEntity>> {

    private static final ResourceLocation TEX =
            new ResourceLocation("liberthia", "textures/entity/disarmer.png");

    public DisarmerRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER)), 1.2F);
    }

    @Override
    public ResourceLocation getTextureLocation(DisarmerEntity entity) { return TEX; }

    @Override
    protected void scale(DisarmerEntity entity, PoseStack pose, float partial) {
        pose.scale(0.55F, 0.55F, 0.55F);
    }
}
