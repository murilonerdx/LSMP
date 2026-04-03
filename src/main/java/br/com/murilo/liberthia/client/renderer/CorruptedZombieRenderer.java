package br.com.murilo.liberthia.client.renderer;

import br.com.murilo.liberthia.entity.CorruptedZombieEntity;
import net.minecraft.client.model.ZombieModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

public class CorruptedZombieRenderer extends HumanoidMobRenderer<CorruptedZombieEntity, ZombieModel<CorruptedZombieEntity>> {

    private static final ResourceLocation TEXTURE = new ResourceLocation("liberthia", "textures/entity/corrupted_zombie.png");

    public CorruptedZombieRenderer(EntityRendererProvider.Context context) {
        super(context, new ZombieModel<>(context.bakeLayer(ModelLayers.ZOMBIE)), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(CorruptedZombieEntity entity) {
        return TEXTURE;
    }
}
