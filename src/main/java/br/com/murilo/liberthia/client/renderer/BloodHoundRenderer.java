package br.com.murilo.liberthia.client.renderer;

import br.com.murilo.liberthia.entity.BloodHoundEntity;
import net.minecraft.client.model.WolfModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/**
 * Reuses vanilla WolfModel with a custom red-soaked texture.
 */
public class BloodHoundRenderer
        extends MobRenderer<BloodHoundEntity, WolfModel<BloodHoundEntity>> {

    private static final ResourceLocation TEX =
            new ResourceLocation("liberthia", "textures/entity/blood_hound.png");

    public BloodHoundRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new WolfModel<>(ctx.bakeLayer(ModelLayers.WOLF)), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(BloodHoundEntity entity) { return TEX; }
}
