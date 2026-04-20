package br.com.murilo.liberthia.client.renderer;

import br.com.murilo.liberthia.client.model.BloodWormModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.Silverfish;

/**
 * Renderer for worm-type entities using our custom segmented 3D model.
 * Skin path is dynamic so BloodWorm / FleshCrawler / GoreWorm can share
 * the renderer class but have different textures.
 */
public class BloodWormRenderer extends MobRenderer<Silverfish, BloodWormModel<Silverfish>> {
    private final ResourceLocation texture;

    public BloodWormRenderer(EntityRendererProvider.Context ctx, String textureName) {
        super(ctx, new BloodWormModel<>(ctx.bakeLayer(BloodWormModel.LAYER)), 0.3F);
        this.texture = new ResourceLocation("liberthia", "textures/entity/" + textureName + ".png");
    }

    @Override
    public ResourceLocation getTextureLocation(Silverfish entity) {
        return texture;
    }
}
