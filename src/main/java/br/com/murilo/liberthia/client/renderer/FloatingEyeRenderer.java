package br.com.murilo.liberthia.client.renderer;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.client.model.FloatingEyeModel;
import br.com.murilo.liberthia.cosmic.IScalableBillboard;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;

/**
 * r187 — renderer 3D do {@link FloatingEyeModel} para os mobs-olho cósmicos.
 * Aplica uma escala base por mob; se a entidade for {@link IScalableBillboard}
 * (cresce ao ser encarada etc.), multiplica pela escala dinâmica dela.
 */
public class FloatingEyeRenderer<T extends Mob> extends MobRenderer<T, FloatingEyeModel<T>> {

    private final ResourceLocation texture;
    private final float baseScale;

    public FloatingEyeRenderer(EntityRendererProvider.Context ctx, String texName, float baseScale) {
        super(ctx, new FloatingEyeModel<>(ctx.bakeLayer(FloatingEyeModel.LAYER)), 0.35F);
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
