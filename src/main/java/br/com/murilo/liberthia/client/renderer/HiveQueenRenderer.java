package br.com.murilo.liberthia.client.renderer;

import br.com.murilo.liberthia.entity.HiveQueenBossEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.SpiderModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/** r192 — Colmeia Rainha: modelo de aranha (inseto) ampliado, textura fúngica/sculk. */
public class HiveQueenRenderer extends MobRenderer<HiveQueenBossEntity, SpiderModel<HiveQueenBossEntity>> {
    private static final ResourceLocation TEX =
            new ResourceLocation("liberthia", "textures/entity/hive_queen.png");

    public HiveQueenRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new SpiderModel<>(ctx.bakeLayer(ModelLayers.SPIDER)), 1.1F);
    }

    @Override public ResourceLocation getTextureLocation(HiveQueenBossEntity e) { return TEX; }

    @Override protected void scale(HiveQueenBossEntity e, PoseStack pose, float partial) {
        pose.scale(2.4F, 2.4F, 2.4F);
    }
}
