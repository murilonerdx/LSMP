package br.com.murilo.liberthia.loom.client;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.registry.ModEntities;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.SilverfishModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.SilverfishRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterDimensionSpecialEffectsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * v0.1.22 r33: registry client-side da dimensão Loom — renderers + dim effects.
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID, value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.MOD)
public final class LoomClientRegistry {

    private static final ResourceLocation TEX_WATCHER =
            new ResourceLocation(LiberthiaMod.MODID, "textures/entity/loom_watcher.png");
    private static final ResourceLocation TEX_PERIPHERAL =
            new ResourceLocation(LiberthiaMod.MODID, "textures/entity/loom_peripheral.png");
    private static final ResourceLocation TEX_SCREAMER =
            new ResourceLocation(LiberthiaMod.MODID, "textures/entity/loom_screamer.png");
    private static final ResourceLocation TEX_WORM =
            new ResourceLocation(LiberthiaMod.MODID, "textures/entity/loom_worm.png");

    private LoomClientRegistry() {}

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        // Watcher
        event.registerEntityRenderer(ModEntities.LOOM_WATCHER.get(),
                ctx -> new HumanoidMobRenderer<br.com.murilo.liberthia.loom.entity.WatcherStalkerEntity,
                        HumanoidModel<br.com.murilo.liberthia.loom.entity.WatcherStalkerEntity>>(
                                ctx, new HumanoidModel<>(ctx.bakeLayer(ModelLayers.ZOMBIE)), 0.5F) {
                    @Override
                    public ResourceLocation getTextureLocation(
                            br.com.murilo.liberthia.loom.entity.WatcherStalkerEntity e) {
                        return TEX_WATCHER;
                    }
                    // r173: estica o modelo pra ~2.9b — figura alta e magra (Slender-like),
                    // alinhando o PEITO renderizado com o hitbox (agora 0.8×2.9).
                    @Override
                    protected void scale(br.com.murilo.liberthia.loom.entity.WatcherStalkerEntity e,
                                         com.mojang.blaze3d.vertex.PoseStack ps, float partial) {
                        ps.scale(1.05F, 1.55F, 1.05F);
                    }
                });
        // Peripheral
        event.registerEntityRenderer(ModEntities.LOOM_PERIPHERAL.get(),
                ctx -> new HumanoidMobRenderer<br.com.murilo.liberthia.loom.entity.PeripheralObserverEntity,
                        HumanoidModel<br.com.murilo.liberthia.loom.entity.PeripheralObserverEntity>>(
                                ctx, new HumanoidModel<>(ctx.bakeLayer(ModelLayers.ZOMBIE)), 0.5F) {
                    @Override
                    public ResourceLocation getTextureLocation(
                            br.com.murilo.liberthia.loom.entity.PeripheralObserverEntity e) {
                        return TEX_PERIPHERAL;
                    }
                });
        // r174: Window Watcher — reusa o visual do Watcher (figura parada lá fora)
        event.registerEntityRenderer(ModEntities.LOOM_WINDOW_WATCHER.get(),
                ctx -> new HumanoidMobRenderer<br.com.murilo.liberthia.loom.entity.WindowWatcherEntity,
                        HumanoidModel<br.com.murilo.liberthia.loom.entity.WindowWatcherEntity>>(
                                ctx, new HumanoidModel<>(ctx.bakeLayer(ModelLayers.ZOMBIE)), 0.5F) {
                    @Override
                    public ResourceLocation getTextureLocation(
                            br.com.murilo.liberthia.loom.entity.WindowWatcherEntity e) {
                        return TEX_WATCHER;
                    }
                });
        // Screamer
        event.registerEntityRenderer(ModEntities.LOOM_SCREAMER.get(),
                ctx -> new HumanoidMobRenderer<br.com.murilo.liberthia.loom.entity.ScreamerTeleporterEntity,
                        HumanoidModel<br.com.murilo.liberthia.loom.entity.ScreamerTeleporterEntity>>(
                                ctx, new HumanoidModel<>(ctx.bakeLayer(ModelLayers.ZOMBIE)), 0.5F) {
                    @Override
                    public ResourceLocation getTextureLocation(
                            br.com.murilo.liberthia.loom.entity.ScreamerTeleporterEntity e) {
                        return TEX_SCREAMER;
                    }
                });
        // Worm — usa silverfish renderer mas com texture custom (override texture)
        event.registerEntityRenderer(ModEntities.LOOM_WORM.get(),
                ctx -> new net.minecraft.client.renderer.entity.MobRenderer<
                        br.com.murilo.liberthia.loom.entity.DimensionalWormEntity,
                        SilverfishModel<br.com.murilo.liberthia.loom.entity.DimensionalWormEntity>>(
                                ctx, new SilverfishModel<>(ctx.bakeLayer(ModelLayers.SILVERFISH)), 0.2F) {
                    @Override
                    public ResourceLocation getTextureLocation(
                            br.com.murilo.liberthia.loom.entity.DimensionalWormEntity e) {
                        return TEX_WORM;
                    }
                });
    }

    @SubscribeEvent
    public static void onRegisterDimEffects(RegisterDimensionSpecialEffectsEvent event) {
        event.register(
                new ResourceLocation(LiberthiaMod.MODID, "loom"),
                new LoomDimensionSpecialEffects());
    }
}
