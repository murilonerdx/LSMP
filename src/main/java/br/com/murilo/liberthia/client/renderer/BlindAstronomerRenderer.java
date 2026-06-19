package br.com.murilo.liberthia.client.renderer;

import br.com.murilo.liberthia.entity.BlindAstronomerBossEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

/** r191 — Astrônomo Cego: humanoide alto de manto escuro com olhos estelares (player UV ×2.3). */
public class BlindAstronomerRenderer
        extends HumanoidMobRenderer<BlindAstronomerBossEntity, HumanoidModel<BlindAstronomerBossEntity>> {
    private static final ResourceLocation TEX =
            new ResourceLocation("liberthia", "textures/entity/blind_astronomer.png");

    public BlindAstronomerRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER)), 1.0F);
    }

    @Override public ResourceLocation getTextureLocation(BlindAstronomerBossEntity e) { return TEX; }

    @Override protected void scale(BlindAstronomerBossEntity e, PoseStack pose, float partial) {
        pose.scale(2.3F, 2.3F, 2.3F);
    }
}
