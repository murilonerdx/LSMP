package br.com.murilo.liberthia.client.renderer;

import br.com.murilo.liberthia.entity.FleshMotherBossEntity;
import br.com.murilo.liberthia.entity.WeavingShadeEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

/** Big, grotesque humanoid — player UV scaled up 2.5x with a bloody skin. */
public class WeavingShadeRenderer
        extends HumanoidMobRenderer<WeavingShadeEntity, HumanoidModel<WeavingShadeEntity>> {

    private static final ResourceLocation TEX =
            new ResourceLocation("liberthia", "textures/entity/weaving_shade.png");

    public WeavingShadeRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER)), 1.2F);
    }

    @Override
    public ResourceLocation getTextureLocation(WeavingShadeEntity entity) { return TEX; }

    @Override
    protected void scale(WeavingShadeEntity entity, PoseStack pose, float partial) {
        pose.scale(1.5F, 1.5F, 1.5F);
    }
}
