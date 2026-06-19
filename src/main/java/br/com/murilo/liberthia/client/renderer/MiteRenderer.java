package br.com.murilo.liberthia.client.renderer;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.client.model.MiteModel;
import br.com.murilo.liberthia.cosmic.IScalableBillboard;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;

/**
 * r187 — renderer 3D do {@link MiteModel} para os pequenos mobs-inseto (e a
 * mariposa, com {@code hasWings=true}).
 */
public class MiteRenderer<T extends Mob> extends MobRenderer<T, MiteModel<T>> {

    private final ResourceLocation texture;
    private final float baseScale;

    public MiteRenderer(EntityRendererProvider.Context ctx, String texName, float baseScale, boolean hasWings) {
        super(ctx, new MiteModel<>(ctx.bakeLayer(MiteModel.LAYER), hasWings), 0.3F);
        this.texture = new ResourceLocation(LiberthiaMod.MODID, "textures/entity/" + texName + ".png");
        this.baseScale = baseScale;
    }

    @Override
    public ResourceLocation getTextureLocation(T e) {
        return texture;
    }

    @Override
    protected void scale(T e, PoseStack ps, float pt) {
        float s = baseScale;
        if (e instanceof IScalableBillboard b) {
            s *= Math.max(0.2F, b.billboardScale());
        }
        ps.scale(s, s, s);
    }
}
