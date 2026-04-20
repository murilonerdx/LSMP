package br.com.murilo.liberthia.client.renderer;

import br.com.murilo.liberthia.entity.OrderPaladinEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

public class OrderPaladinRenderer
        extends HumanoidMobRenderer<OrderPaladinEntity, HumanoidModel<OrderPaladinEntity>> {

    private static final ResourceLocation TEX =
            new ResourceLocation("liberthia", "textures/entity/order_paladin.png");

    public OrderPaladinRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER)), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(OrderPaladinEntity entity) { return TEX; }
}
