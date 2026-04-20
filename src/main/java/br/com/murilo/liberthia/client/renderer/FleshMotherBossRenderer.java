package br.com.murilo.liberthia.client.renderer;

import br.com.murilo.liberthia.entity.FleshMotherBossEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

/** Big, grotesque humanoid — player UV scaled up 2.5x with a bloody skin. */
public class FleshMotherBossRenderer
        extends HumanoidMobRenderer<FleshMotherBossEntity, HumanoidModel<FleshMotherBossEntity>> {

    private static final ResourceLocation TEX =
            new ResourceLocation("liberthia", "textures/entity/flesh_mother.png");

    public FleshMotherBossRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER)), 1.2F);
    }

    @Override
    public ResourceLocation getTextureLocation(FleshMotherBossEntity entity) { return TEX; }

    @Override
    protected void scale(FleshMotherBossEntity entity, PoseStack pose, float partial) {
        pose.scale(2.5F, 2.5F, 2.5F);
    }
}
