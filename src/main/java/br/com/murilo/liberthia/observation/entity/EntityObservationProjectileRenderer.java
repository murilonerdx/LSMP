package br.com.murilo.liberthia.observation.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * v0.1.22 r64: Renderer pra {@link EntityObservationProjectile}.
 *
 * <p>Renderer mínimo — todo o visual vem das particles spawned em tick().
 * Igual ao AN's {@code RenderSpell} — render() vazio, particles fazem o trabalho.
 */
@OnlyIn(Dist.CLIENT)
public class EntityObservationProjectileRenderer extends EntityRenderer<EntityObservationProjectile> {

    public EntityObservationProjectileRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(EntityObservationProjectile entity, float entityYaw, float partialTicks,
                        PoseStack pose, MultiBufferSource buffer, int packedLight) {
        // Empty — trail particles spawn in entity.tick() handle the visual.
        // Pattern AN's RenderSpell.
    }

    @Override
    public ResourceLocation getTextureLocation(EntityObservationProjectile entity) {
        return new ResourceLocation("minecraft", "textures/misc/white.png");
    }
}
