package br.com.murilo.liberthia.client.renderer;

import br.com.murilo.liberthia.entity.DarkConsciousnessEntity;
import net.minecraft.client.model.EndermanModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * Renderer for the Dark Consciousness entity.
 * Uses the Enderman model as base — tall, dark, otherworldly silhouette.
 */
public class DarkConsciousnessRenderer extends MobRenderer<DarkConsciousnessEntity, EndermanModel<DarkConsciousnessEntity>> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation("liberthia", "textures/entity/dark_consciousness.png");

    public DarkConsciousnessRenderer(EntityRendererProvider.Context context) {
        super(context, new EndermanModel<>(context.bakeLayer(ModelLayers.ENDERMAN)), 0.6F);
    }

    @Override
    public ResourceLocation getTextureLocation(DarkConsciousnessEntity entity) {
        return TEXTURE;
    }
}
