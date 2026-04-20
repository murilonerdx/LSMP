package br.com.murilo.liberthia.client.renderer;

import br.com.murilo.liberthia.entity.projectile.HemoBoltEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/**
 * Render-less bolt — the projectile is represented entirely by its particle
 * trail (DAMAGE_INDICATOR). Keeps implementation minimal and dist-safe.
 */
public class HemoBoltRenderer extends EntityRenderer<HemoBoltEntity> {
    private static final ResourceLocation TEX =
            new ResourceLocation("textures/misc/white.png");

    public HemoBoltRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public ResourceLocation getTextureLocation(HemoBoltEntity entity) { return TEX; }

    @Override
    public void render(HemoBoltEntity entity, float yaw, float partial,
                       PoseStack pose, MultiBufferSource buf, int light) {
        // particle-only; skip geometry
    }
}
