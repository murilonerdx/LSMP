package br.com.murilo.liberthia.client.renderer;

import br.com.murilo.liberthia.entity.WoundedPilgrimEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class WoundedPilgrimRenderer extends MobRenderer<WoundedPilgrimEntity, HumanoidModel<WoundedPilgrimEntity>> {
    private static final ResourceLocation TEX =
            new ResourceLocation("liberthia", "textures/entity/wounded_pilgrim.png");

    public WoundedPilgrimRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER)), 0.4F);
    }

    @Override
    public ResourceLocation getTextureLocation(WoundedPilgrimEntity entity) { return TEX; }

    @Override
    protected void scale(WoundedPilgrimEntity entity, com.mojang.blaze3d.vertex.PoseStack pose, float partial) {
        pose.scale(0.95F, 0.95F, 0.95F);
    }
}
