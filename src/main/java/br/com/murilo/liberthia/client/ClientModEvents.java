package br.com.murilo.liberthia.client;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.client.renderer.BlackHoleRenderer;
import br.com.murilo.liberthia.client.screen.*;
import br.com.murilo.liberthia.registry.ModEntities;
import br.com.murilo.liberthia.registry.ModMenuTypes;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import br.com.murilo.liberthia.registry.ModBlocks;

@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientModEvents {
    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.BLACK_HOLE.get(), BlackHoleRenderer::new);
        event.registerEntityRenderer(ModEntities.DARK_MATTER_SPORE.get(), br.com.murilo.liberthia.client.renderer.DarkMatterSporeRenderer::new);
        event.registerEntityRenderer(ModEntities.CLEANSING_GRENADE.get(), br.com.murilo.liberthia.client.renderer.CleansingGrenadeRenderer::new);
        event.registerEntityRenderer(ModEntities.CORRUPTED_ZOMBIE.get(), br.com.murilo.liberthia.client.renderer.CorruptedZombieRenderer::new);
        event.registerEntityRenderer(ModEntities.SPORE_SPITTER.get(), br.com.murilo.liberthia.client.renderer.SporeSpitterRenderer::new);
        event.registerEntityRenderer(ModEntities.WHITE_MATTER_EXPLOSION.get(), br.com.murilo.liberthia.client.renderer.WhiteMatterExplosionRenderer::new);
        event.registerEntityRenderer(ModEntities.CLONE_PLAYER.get(), br.com.murilo.liberthia.client.renderer.ClonePlayerRenderer::new);
        event.registerEntityRenderer(ModEntities.DARK_CONSCIOUSNESS.get(), br.com.murilo.liberthia.client.renderer.DarkConsciousnessRenderer::new);
        event.registerEntityRenderer(ModEntities.EYE_OF_HORUS.get(), br.com.murilo.liberthia.client.renderer.EyeOfHorusRenderer::new);
        // Reuse vanilla Silverfish renderer as fast, stable base for BloodWorm
        event.registerEntityRenderer(ModEntities.BLOOD_WORM.get(),
                ctx -> new br.com.murilo.liberthia.client.renderer.BloodWormRenderer(ctx, "blood_worm"));
        event.registerEntityRenderer(ModEntities.FLESH_CRAWLER.get(),
                ctx -> new br.com.murilo.liberthia.client.renderer.BloodWormRenderer(ctx, "flesh_crawler"));
        event.registerEntityRenderer(ModEntities.GORE_WORM.get(),
                ctx -> new br.com.murilo.liberthia.client.renderer.BloodWormRenderer(ctx, "gore_worm"));
        // Blood Orb is particle-only; render as invisible using a no-op renderer
        event.registerEntityRenderer(ModEntities.BLOOD_CULTIST.get(),
                br.com.murilo.liberthia.client.renderer.BloodCultistRenderer::new);
        event.registerEntityRenderer(ModEntities.BLOOD_PRIEST.get(),
                br.com.murilo.liberthia.client.renderer.BloodPriestRenderer::new);
        event.registerEntityRenderer(ModEntities.WOUNDED_PILGRIM.get(),
                br.com.murilo.liberthia.client.renderer.WoundedPilgrimRenderer::new);
        event.registerEntityRenderer(ModEntities.HEMO_BOLT.get(),
                br.com.murilo.liberthia.client.renderer.HemoBoltRenderer::new);
        event.registerEntityRenderer(ModEntities.BLEEDING_ARROW.get(),
                net.minecraft.client.renderer.entity.TippableArrowRenderer::new);
        event.registerEntityRenderer(ModEntities.ORDER_PALADIN.get(),
                br.com.murilo.liberthia.client.renderer.OrderPaladinRenderer::new);
        event.registerEntityRenderer(ModEntities.BLOOD_PEARL.get(),
                ctx -> new net.minecraft.client.renderer.entity.ThrownItemRenderer<>(ctx, 1.0F, true));
        event.registerEntityRenderer(ModEntities.VEILING_ORB.get(),
                net.minecraft.client.renderer.entity.ThrownItemRenderer::new);
        event.registerEntityRenderer(ModEntities.MIND_SPLINTER_DART.get(),
                net.minecraft.client.renderer.entity.ThrownItemRenderer::new);
        event.registerEntityRenderer(ModEntities.LIGHTNING_GRENADE.get(),
                net.minecraft.client.renderer.entity.ThrownItemRenderer::new);
        event.registerEntityRenderer(ModEntities.BURNING_GEM.get(),
                net.minecraft.client.renderer.entity.ThrownItemRenderer::new);
        event.registerEntityRenderer(ModEntities.FROST_FLASK.get(),
                net.minecraft.client.renderer.entity.ThrownItemRenderer::new);
        event.registerEntityRenderer(ModEntities.PURIFYING_FLASK.get(),
                net.minecraft.client.renderer.entity.ThrownItemRenderer::new);
        event.registerEntityRenderer(ModEntities.POSSESSED_ZOMBIE.get(),
                net.minecraft.client.renderer.entity.ZombieRenderer::new);
        event.registerEntityRenderer(ModEntities.POSSESSED_SKELETON.get(),
                net.minecraft.client.renderer.entity.SkeletonRenderer::new);

        // --- Blood Warden boss: ZOMBIE layer (has 'hat') + Husk texture, scaled 1.6× ---
        event.registerEntityRenderer(ModEntities.BLOOD_WARDEN.get(), ctx -> {
            net.minecraft.client.model.HumanoidModel<br.com.murilo.liberthia.entity.BloodWardenBossEntity> body =
                    new net.minecraft.client.model.HumanoidModel<>(
                            ctx.bakeLayer(net.minecraft.client.model.geom.ModelLayers.ZOMBIE));
            return new net.minecraft.client.renderer.entity.MobRenderer<
                    br.com.murilo.liberthia.entity.BloodWardenBossEntity,
                    net.minecraft.client.model.HumanoidModel<br.com.murilo.liberthia.entity.BloodWardenBossEntity>>(ctx, body, 1.0F) {
                @Override
                public net.minecraft.resources.ResourceLocation getTextureLocation(
                        br.com.murilo.liberthia.entity.BloodWardenBossEntity e) {
                    return net.minecraft.resources.ResourceLocation.tryBuild("minecraft", "textures/entity/zombie/husk.png");
                }
                @Override
                protected void scale(br.com.murilo.liberthia.entity.BloodWardenBossEntity entity,
                                     com.mojang.blaze3d.vertex.PoseStack pose, float partialTick) {
                    pose.scale(1.6F, 1.6F, 1.6F);
                }
            };
        });

        // --- Weaving Shade: ZOMBIE layer + Drowned texture, scaled 0.6× ---
        event.registerEntityRenderer(ModEntities.WEAVING_SHADE.get(), ctx -> {
            net.minecraft.client.model.HumanoidModel<br.com.murilo.liberthia.entity.WeavingShadeEntity> body =
                    new net.minecraft.client.model.HumanoidModel<>(
                            ctx.bakeLayer(net.minecraft.client.model.geom.ModelLayers.ZOMBIE));
            return new net.minecraft.client.renderer.entity.MobRenderer<
                    br.com.murilo.liberthia.entity.WeavingShadeEntity,
                    net.minecraft.client.model.HumanoidModel<br.com.murilo.liberthia.entity.WeavingShadeEntity>>(ctx, body, 0.3F) {
                @Override
                public net.minecraft.resources.ResourceLocation getTextureLocation(
                        br.com.murilo.liberthia.entity.WeavingShadeEntity e) {
                    return net.minecraft.resources.ResourceLocation.tryBuild("minecraft", "textures/entity/zombie/drowned.png");
                }
                @Override
                protected void scale(br.com.murilo.liberthia.entity.WeavingShadeEntity entity,
                                     com.mojang.blaze3d.vertex.PoseStack pose, float partialTick) {
                    pose.scale(0.6F, 0.6F, 0.6F);
                }
            };
        });

        // --- Disarmer: ZOMBIE layer + zombie texture ---
        event.registerEntityRenderer(ModEntities.DISARMER.get(), ctx -> {
            net.minecraft.client.model.HumanoidModel<br.com.murilo.liberthia.entity.DisarmerEntity> body =
                    new net.minecraft.client.model.HumanoidModel<>(
                            ctx.bakeLayer(net.minecraft.client.model.geom.ModelLayers.ZOMBIE));
            return new net.minecraft.client.renderer.entity.MobRenderer<
                    br.com.murilo.liberthia.entity.DisarmerEntity,
                    net.minecraft.client.model.HumanoidModel<br.com.murilo.liberthia.entity.DisarmerEntity>>(ctx, body, 0.5F) {
                @Override
                public net.minecraft.resources.ResourceLocation getTextureLocation(
                        br.com.murilo.liberthia.entity.DisarmerEntity e) {
                    return net.minecraft.resources.ResourceLocation.tryBuild("minecraft", "textures/entity/zombie/zombie.png");
                }
            };
        });
        event.registerEntityRenderer(ModEntities.BLOOD_MAGE.get(),
                br.com.murilo.liberthia.client.renderer.BloodMageRenderer::new);
        event.registerEntityRenderer(ModEntities.BLOOD_HOUND.get(),
                br.com.murilo.liberthia.client.renderer.BloodHoundRenderer::new);
        event.registerEntityRenderer(ModEntities.FLESH_MOTHER_BOSS.get(),
                br.com.murilo.liberthia.client.renderer.FleshMotherBossRenderer::new);
        event.registerEntityRenderer(ModEntities.BLOOD_ORB.get(),
                ctx -> new net.minecraft.client.renderer.entity.EntityRenderer<br.com.murilo.liberthia.entity.BloodOrbEntity>(ctx) {
                    @Override public net.minecraft.resources.ResourceLocation getTextureLocation(br.com.murilo.liberthia.entity.BloodOrbEntity e) {
                        return new net.minecraft.resources.ResourceLocation("textures/misc/white.png");
                    }
                    @Override public boolean shouldRender(br.com.murilo.liberthia.entity.BloodOrbEntity e, net.minecraft.client.renderer.culling.Frustum f, double x, double y, double z) { return false; }
                });
    }

    @SubscribeEvent
    public static void onRegisterLayers(net.minecraftforge.client.event.EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(br.com.murilo.liberthia.client.model.BloodWormModel.LAYER,
                br.com.murilo.liberthia.client.model.BloodWormModel::createBodyLayer);
    }

    @SubscribeEvent
    public static void onRegisterGuiOverlays(net.minecraftforge.client.event.RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("infection_hud", InfectionHudOverlay.INSTANCE);
        event.registerAboveAll("matter_energy_hud", MatterEnergyHudOverlay.INSTANCE);
        event.registerAboveAll("dna_mutation_hud", DnaMutationOverlay.INSTANCE);
        event.registerAboveAll("radiation_guide_hud", RadiationGuideOverlay.INSTANCE);
        event.registerAboveAll("infection_distortion", InfectionDistortionOverlay.INSTANCE);
    }

    @SubscribeEvent
    @SuppressWarnings("removal")
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(ModMenuTypes.PURIFICATION_BENCH.get(), PurificationBenchScreen::new);
            MenuScreens.register(ModMenuTypes.DARK_MATTER_FORGE.get(), DarkMatterForgeScreen::new);
            MenuScreens.register(ModMenuTypes.MATTER_INFUSER.get(), MatterInfuserScreen::new);
            MenuScreens.register(ModMenuTypes.RESEARCH_TABLE.get(), ResearchTableScreen::new);
            MenuScreens.register(ModMenuTypes.CONTAINMENT_CHAMBER.get(), ContainmentChamberScreen::new);
            MenuScreens.register(ModMenuTypes.MATTER_TRANSMUTER.get(), MatterTransmuterScreen::new);

            // RenderType registration for transparent/cutout blocks
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.SPORE_BLOOM.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.INFECTION_GROWTH.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.WORMHOLE_BLOCK.get(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.CLEAR_MATTER_FLUID_BLOCK.get(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.YELLOW_MATTER_FLUID_BLOCK.get(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.BLOOD_FLUID_BLOCK.get(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.CHALK_SYMBOL.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.THORN_BRIAR.get(), RenderType.cutout());
        });
    }
}
