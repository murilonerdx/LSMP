package br.com.murilo.liberthia.client.renderer;

import br.com.murilo.liberthia.entity.ThrownVarathaEntity;
import br.com.murilo.liberthia.registry.ModItems;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** r180b — renderiza a {@link ThrownVarathaEntity} como a lança girando no ar. */
public class ThrownVarathaRenderer extends EntityRenderer<ThrownVarathaEntity> {

    private static final ResourceLocation TEX =
            new ResourceLocation("liberthia", "textures/item/varatha_spear.png");
    private final ItemRenderer itemRenderer;
    private final ItemStack stack;

    public ThrownVarathaRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.itemRenderer = ctx.getItemRenderer();
        this.stack = new ItemStack(ModItems.VARATHA_SPEAR.get());
    }

    @Override
    public void render(ThrownVarathaEntity e, float yaw, float partial, PoseStack ps,
                       MultiBufferSource buf, int light) {
        ps.pushPose();
        ps.scale(1.4F, 1.4F, 1.4F);
        ps.mulPose(Axis.YP.rotationDegrees((e.tickCount + partial) * 28F));
        ps.mulPose(Axis.XP.rotationDegrees(45F));
        this.itemRenderer.renderStatic(this.stack, ItemDisplayContext.GROUND, light,
                OverlayTexture.NO_OVERLAY, ps, buf, e.level(), e.getId());
        ps.popPose();
        super.render(e, yaw, partial, ps, buf, light);
    }

    @Override
    public ResourceLocation getTextureLocation(ThrownVarathaEntity e) {
        return TEX;
    }
}
