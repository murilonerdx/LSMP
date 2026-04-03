package br.com.murilo.liberthia.client.renderer;

import br.com.murilo.liberthia.entity.SporeSpitterEntity;
import net.minecraft.client.model.SpiderModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class SporeSpitterRenderer extends MobRenderer<SporeSpitterEntity, SpiderModel<SporeSpitterEntity>> {

    private static final ResourceLocation TEXTURE = new ResourceLocation("liberthia", "textures/entity/spore_spitter.png");

    public SporeSpitterRenderer(EntityRendererProvider.Context context) {
        super(context, new SpiderModel<>(context.bakeLayer(ModelLayers.SPIDER)), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(SporeSpitterEntity entity) {
        return TEXTURE;
    }
}
