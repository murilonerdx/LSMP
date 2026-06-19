package br.com.murilo.liberthia.magic.spell.voidspell;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.SilverfishModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.resources.ResourceLocation;

/**
 * v0.1.152 r120: <b>VoidLarvaRenderer</b> — usa modelo de Silverfish vanilla
 * com textura roxa custom + scale ligeiramente maior.
 */
public class VoidLarvaRenderer extends MobRenderer<VoidLarvaEntity, SilverfishModel<VoidLarvaEntity>> {

    private static final ResourceLocation TEXTURE = new ResourceLocation(
            "liberthia", "textures/entity/void_larva/void_larva.png");

    public VoidLarvaRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new SilverfishModel<>(ctx.bakeLayer(ModelLayers.SILVERFISH)), 0.25F);
    }

    @Override
    public ResourceLocation getTextureLocation(VoidLarvaEntity entity) {
        return TEXTURE;
    }

    @Override
    protected void scale(VoidLarvaEntity entity, PoseStack poseStack, float partialTick) {
        poseStack.scale(1.2F, 1.2F, 1.2F);
    }
}
