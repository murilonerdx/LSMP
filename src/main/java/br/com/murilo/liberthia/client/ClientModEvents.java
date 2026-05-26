package br.com.murilo.liberthia.client;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.client.renderer.BlackHoleRenderer;
import br.com.murilo.liberthia.client.screen.*;
import br.com.murilo.liberthia.registry.ModEntities;
import br.com.murilo.liberthia.registry.ModMenuTypes;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
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
        // REMOVIDO v0.1.13: CLEANSING_GRENADE renderer
        event.registerEntityRenderer(ModEntities.CORRUPTED_ZOMBIE.get(), br.com.murilo.liberthia.client.renderer.CorruptedZombieRenderer::new);
        event.registerEntityRenderer(ModEntities.SPORE_SPITTER.get(), br.com.murilo.liberthia.client.renderer.SporeSpitterRenderer::new);
        event.registerEntityRenderer(ModEntities.WHITE_MATTER_EXPLOSION.get(), br.com.murilo.liberthia.client.renderer.WhiteMatterExplosionRenderer::new);
        event.registerEntityRenderer(ModEntities.CLONE_PLAYER.get(), br.com.murilo.liberthia.client.renderer.ClonePlayerRenderer::new);
        // r48: Reflection Entity (clone com AI) — usa renderer próprio
        event.registerEntityRenderer(ModEntities.REFLECTION_ENTITY.get(),
                br.com.murilo.liberthia.cosmic.observatory.client.ReflectionEntityRenderer::new);
        // r64: Observation Projectile — renderer mínimo (trail via particles em tick())
        event.registerEntityRenderer(ModEntities.OBSERVATION_PROJECTILE.get(),
                br.com.murilo.liberthia.observation.entity.EntityObservationProjectileRenderer::new);
        // r24: SoulBody reusa o ClonePlayerRenderer (mesma skin via OWNER_UUID)
        event.registerEntityRenderer(ModEntities.SOUL_BODY.get(), br.com.murilo.liberthia.client.renderer.ClonePlayerRenderer::new);
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

        // BlockEntity renderers
        event.registerBlockEntityRenderer(
                br.com.murilo.liberthia.registry.ModBlockEntities.LASER_EMITTER.get(),
                br.com.murilo.liberthia.client.renderer.LaserBeamRenderer::new);

        // v0.1.39: Matter Tank BER — renderiza fluido visual proporcional ao
        // fillage, substitui o sistema antigo de blockstate level 0-4 que o
        // user reclamou que "mudava de modelo".
        event.registerBlockEntityRenderer(
                br.com.murilo.liberthia.registry.ModBlockEntities.MATTER_TANK.get(),
                br.com.murilo.liberthia.client.renderer.MatterTankRenderer::new);



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
        event.registerEntityRenderer(ModEntities.WEAVING_SHADE.get(),
                br.com.murilo.liberthia.client.renderer.WeavingShadeRenderer::new);
        event.registerEntityRenderer(ModEntities.BLOOD_WARDEN.get(),
                br.com.murilo.liberthia.client.renderer.BloodWardenRenderer::new);
        event.registerEntityRenderer(ModEntities.DISARMER.get(),
                br.com.murilo.liberthia.client.renderer.DisarmerRenderer::new);
        event.registerEntityRenderer(ModEntities.BLOOD_ORB.get(),
                ctx -> new net.minecraft.client.renderer.entity.EntityRenderer<br.com.murilo.liberthia.entity.BloodOrbEntity>(ctx) {
                    @Override public net.minecraft.resources.ResourceLocation getTextureLocation(br.com.murilo.liberthia.entity.BloodOrbEntity e) {
                        return new net.minecraft.resources.ResourceLocation("textures/misc/white.png");
                    }
                    @Override public boolean shouldRender(br.com.murilo.liberthia.entity.BloodOrbEntity e, net.minecraft.client.renderer.culling.Frustum f, double x, double y, double z) { return false; }
                });

        // r81: Horror Framework entities — usam HumanoidMobRenderer simples
        // (Monster genérico) com modelo humanoide básico
        event.registerEntityRenderer(ModEntities.EMPTY_MAN.get(),
                ctx -> new net.minecraft.client.renderer.entity.HumanoidMobRenderer<br.com.murilo.liberthia.cosmic.horror.entity.EmptyManEntity, net.minecraft.client.model.HumanoidModel<br.com.murilo.liberthia.cosmic.horror.entity.EmptyManEntity>>(
                        ctx,
                        new net.minecraft.client.model.HumanoidModel<>(
                                ctx.bakeLayer(net.minecraft.client.model.geom.ModelLayers.PLAYER)),
                        0.5F) {
                    @Override
                    public net.minecraft.resources.ResourceLocation getTextureLocation(br.com.murilo.liberthia.cosmic.horror.entity.EmptyManEntity e) {
                        return new net.minecraft.resources.ResourceLocation("textures/entity/zombie/zombie.png");
                    }
                });
        event.registerEntityRenderer(ModEntities.OBSERVER.get(),
                ctx -> new net.minecraft.client.renderer.entity.HumanoidMobRenderer<br.com.murilo.liberthia.cosmic.horror.entity.ObserverEntity, net.minecraft.client.model.HumanoidModel<br.com.murilo.liberthia.cosmic.horror.entity.ObserverEntity>>(
                        ctx,
                        new net.minecraft.client.model.HumanoidModel<>(
                                ctx.bakeLayer(net.minecraft.client.model.geom.ModelLayers.ZOMBIE)),
                        0.5F) {
                    @Override
                    public net.minecraft.resources.ResourceLocation getTextureLocation(br.com.murilo.liberthia.cosmic.horror.entity.ObserverEntity e) {
                        return new net.minecraft.resources.ResourceLocation("textures/entity/warden/warden.png");
                    }
                });
        // Absence: renderer no-op (invisível por design)
        event.registerEntityRenderer(ModEntities.ABSENCE.get(),
                ctx -> new net.minecraft.client.renderer.entity.EntityRenderer<br.com.murilo.liberthia.cosmic.horror.entity.AbsenceEntity>(ctx) {
                    @Override public net.minecraft.resources.ResourceLocation getTextureLocation(br.com.murilo.liberthia.cosmic.horror.entity.AbsenceEntity e) {
                        return new net.minecraft.resources.ResourceLocation("textures/misc/white.png");
                    }
                    @Override public boolean shouldRender(br.com.murilo.liberthia.cosmic.horror.entity.AbsenceEntity e, net.minecraft.client.renderer.culling.Frustum f, double x, double y, double z) { return false; }
                });
        // Remembered: usa HumanoidMobRenderer com modelo magro
        event.registerEntityRenderer(ModEntities.REMEMBERED.get(),
                ctx -> new net.minecraft.client.renderer.entity.HumanoidMobRenderer<br.com.murilo.liberthia.cosmic.horror.entity.RememberedEntity, net.minecraft.client.model.HumanoidModel<br.com.murilo.liberthia.cosmic.horror.entity.RememberedEntity>>(
                        ctx,
                        new net.minecraft.client.model.HumanoidModel<>(
                                ctx.bakeLayer(net.minecraft.client.model.geom.ModelLayers.SKELETON)),
                        0.5F) {
                    @Override
                    public net.minecraft.resources.ResourceLocation getTextureLocation(br.com.murilo.liberthia.cosmic.horror.entity.RememberedEntity e) {
                        return new net.minecraft.resources.ResourceLocation("textures/entity/skeleton/skeleton.png");
                    }
                });
    }

    @SubscribeEvent
    public static void onRegisterLayers(net.minecraftforge.client.event.EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(br.com.murilo.liberthia.client.model.BloodWormModel.LAYER,
                br.com.murilo.liberthia.client.model.BloodWormModel::createBodyLayer);
    }

    @SubscribeEvent
    public static void onRegisterBlockColors(RegisterColorHandlersEvent.Block event) {
        // Pipe Extrator → laranja (0xFF8C00) tinted onde tem tintindex
        event.register((s, l, p, idx) -> 0xFF8C00, ModBlocks.ITEM_EXTRACTOR.get());
        // Pipe Inseridor → verde (0x4CD950) tinted
        event.register((s, l, p, idx) -> 0x4CD950, ModBlocks.ITEM_INSERTER.get());
    }

    @SubscribeEvent
    public static void onRegisterItemColors(RegisterColorHandlersEvent.Item event) {
        event.register((s, idx) -> 0xFF8C00, ModBlocks.ITEM_EXTRACTOR.get().asItem());
        event.register((s, idx) -> 0x4CD950, ModBlocks.ITEM_INSERTER.get().asItem());
    }

    @SubscribeEvent
    public static void onRegisterGuiOverlays(net.minecraftforge.client.event.RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("infection_hud", InfectionHudOverlay.INSTANCE);
        event.registerAboveAll("matter_energy_hud", MatterEnergyHudOverlay.INSTANCE);
        event.registerAboveAll("dna_mutation_hud", DnaMutationOverlay.INSTANCE);
        event.registerAboveAll("radiation_guide_hud", RadiationGuideOverlay.INSTANCE);
        event.registerAboveAll("infection_distortion", InfectionDistortionOverlay.INSTANCE);
        event.registerAboveAll("matter_profile_hud", br.com.murilo.liberthia.client.hud.MatterProfileHud.INSTANCE);
    }

    @SubscribeEvent
    @SuppressWarnings("removal")
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(ModMenuTypes.PURIFICATION_BENCH.get(), PurificationBenchScreen::new);
            // r69: Scribes Table — GUI de crafting de spell parchments
            MenuScreens.register(ModMenuTypes.SCRIBES_TABLE.get(),
                    br.com.murilo.liberthia.client.screen.ScribesTableScreen::new);
            // r73: Imbuement Table screen
            MenuScreens.register(ModMenuTypes.IMBUEMENT_TABLE.get(),
                    br.com.murilo.liberthia.client.screen.ImbuementScreen::new);
            // r77: Spell Binding Pedestal screen
            MenuScreens.register(ModMenuTypes.SPELL_BINDING_PEDESTAL.get(),
                    br.com.murilo.liberthia.client.screen.SpellBindingPedestalScreen::new);
            MenuScreens.register(ModMenuTypes.DARK_MATTER_FORGE.get(), DarkMatterForgeScreen::new);
            MenuScreens.register(ModMenuTypes.MATTER_INFUSER.get(), MatterInfuserScreen::new);
            MenuScreens.register(ModMenuTypes.RESEARCH_TABLE.get(), ResearchTableScreen::new);
            MenuScreens.register(ModMenuTypes.CONTAINMENT_CHAMBER.get(), ContainmentChamberScreen::new);
            MenuScreens.register(ModMenuTypes.MATTER_TRANSMUTER.get(), MatterTransmuterScreen::new);
            MenuScreens.register(ModMenuTypes.DARK_MATTER_ALCHEMIZER.get(), DarkMatterAlchemizerScreen::new);
            MenuScreens.register(ModMenuTypes.DARK_MATTER_GENERATOR.get(),
                    br.com.murilo.liberthia.client.screen.DarkMatterGeneratorScreen::new);
            MenuScreens.register(ModMenuTypes.DARK_MATTER_CHEST.get(),
                    br.com.murilo.liberthia.client.screen.DarkMatterChestScreen::new);
            MenuScreens.register(ModMenuTypes.FRAGMENTED_GENERATOR.get(),
                    br.com.murilo.liberthia.client.screen.FragmentedGeneratorScreen::new);
            MenuScreens.register(ModMenuTypes.CRYSTALLIZER.get(),
                    br.com.murilo.liberthia.client.screen.CrystallizerScreen::new);
            MenuScreens.register(ModMenuTypes.AUTO_FARMER.get(),
                    br.com.murilo.liberthia.client.screen.AutoFarmerScreen::new);
            MenuScreens.register(ModMenuTypes.MATTER_ANALYZER.get(),
                    br.com.murilo.liberthia.client.screen.MatterAnalyzerScreen::new);
            MenuScreens.register(ModMenuTypes.PIPE_FILTER.get(),
                    br.com.murilo.liberthia.client.screen.PipeFilterScreen::new);
            MenuScreens.register(ModMenuTypes.MATTER_PURIFIER.get(),
                    br.com.murilo.liberthia.client.screen.MatterPurifierScreen::new);
            MenuScreens.register(ModMenuTypes.MATTER_PILL_BREWER.get(),
                    br.com.murilo.liberthia.client.screen.MatterPillBrewerScreen::new);
            MenuScreens.register(ModMenuTypes.MATTER_TANK.get(),
                    br.com.murilo.liberthia.client.screen.MatterTankScreen::new);
            MenuScreens.register(ModMenuTypes.MATTER_EXTRACTOR.get(),
                    br.com.murilo.liberthia.client.screen.MatterExtractorScreen::new);
            MenuScreens.register(ModMenuTypes.DIMENSIONAL_EXTRACTOR.get(),
                    br.com.murilo.liberthia.client.screen.DimensionalExtractorScreen::new);
            MenuScreens.register(ModMenuTypes.DARK_MATTER_BATTERY.get(),
                    br.com.murilo.liberthia.client.screen.DarkMatterBatteryScreen::new);
            MenuScreens.register(ModMenuTypes.DIMENSIONAL_CHEST.get(),
                    br.com.murilo.liberthia.client.screen.DimensionalChestScreen::new);
            MenuScreens.register(ModMenuTypes.MATTER_REFINER.get(),
                    br.com.murilo.liberthia.client.screen.MatterRefinerScreen::new);
            MenuScreens.register(ModMenuTypes.WIRELESS_CHARGER.get(),
                    br.com.murilo.liberthia.client.screen.WirelessChargerScreen::new);
            // v0.1.22 r27: Dimensional Antenna
            MenuScreens.register(ModMenuTypes.DIMENSIONAL_ANTENNA.get(),
                    br.com.murilo.liberthia.client.screen.DimensionalAntennaScreen::new);
            // v0.1.22 r28: Quantum Terminal
            MenuScreens.register(ModMenuTypes.QUANTUM_TERMINAL.get(),
                    br.com.murilo.liberthia.client.screen.QuantumTerminalScreen::new);

            // Sample Vial: model override "filled" baseado no NBT (legacy, mantido).
            net.minecraft.client.renderer.item.ItemProperties.register(
                    br.com.murilo.liberthia.registry.ModItems.SAMPLE_VIAL.get(),
                    new net.minecraft.resources.ResourceLocation(
                            br.com.murilo.liberthia.LiberthiaMod.MODID, "filled"),
                    (stack, level, entity, seed) ->
                            stack.hasTag() && stack.getTag().contains("src") ? 1f : 0f);

            // Sample Vial: matter_type property — escolhe textura por matéria dominante.
            // Valores (sincronizados com sample_vial.json):
            //   0.0 → vazio (sample_vial.png)
            //   0.2 → DM dominante (purple)   threshold 0.1
            //   0.5 → WM dominante (white)    threshold 0.4
            //   0.8 → YM dominante (yellow)   threshold 0.7
            net.minecraft.client.renderer.item.ItemProperties.register(
                    br.com.murilo.liberthia.registry.ModItems.SAMPLE_VIAL.get(),
                    new net.minecraft.resources.ResourceLocation(
                            br.com.murilo.liberthia.LiberthiaMod.MODID, "matter_type"),
                    (stack, level, entity, seed) -> {
                        if (!stack.hasTag()) return 0f;
                        var tag = stack.getTag();
                        if (!tag.contains("src")) return 0f;
                        float dm = tag.getFloat("dm");
                        float wm = tag.getFloat("wm");
                        float ym = tag.getFloat("ym");
                        float max = Math.max(dm, Math.max(wm, ym));
                        if (max <= 0) return 0f;
                        if (max == ym) return 0.8f;
                        if (max == wm) return 0.5f;
                        return 0.2f;  // DM (também é fallback se tudo igual)
                    });

            // Cutout render so connection arms transparency works
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.ENERGY_CABLE.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.ITEM_PIPE.get(), RenderType.cutout());

            // Matter pipes — cutout pra suportar transparência das pontas
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.MATTER_PIPE_DARK.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.MATTER_PIPE_CLEAR.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.MATTER_PIPE_YELLOW.get(), RenderType.cutout());
            // Matter tank — translucent pra deixar ver fluido por dentro do vidro
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.MATTER_TANK.get(), RenderType.translucent());

            event.enqueueWork(() ->
                    MenuScreens.register(ModMenuTypes.SPIRITUAL_TRADE.get(), SpiritualTradeScreen::new)
            );

            // RenderType registration for transparent/cutout blocks
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.SPORE_BLOOM.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.INFECTION_GROWTH.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.WORMHOLE_BLOCK.get(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.CLEAR_MATTER_FLUID_BLOCK.get(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.YELLOW_MATTER_FLUID_BLOCK.get(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.BLOOD_FLUID_BLOCK.get(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.CHALK_SYMBOL.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.THORN_BRIAR.get(), RenderType.cutout());
            // r34: OCCULT chalks + candles + portal — RENDER LAYER CUTOUT
            // (sem isso, fundo transparente vira PRETO no chão)
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.CHALK_MARK_WHITE.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.CHALK_MARK_GOLDEN.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.CHALK_MARK_PURPLE.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.CHALK_MARK_RED.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.CHALK_MARK_BLACK.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.CANDLE_WHITE_OCCULT.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.CANDLE_GOLDEN_OCCULT.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.CANDLE_PURPLE_OCCULT.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.CANDLE_RED_OCCULT.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.CANDLE_BLACK_OCCULT.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.LOOM_PORTAL.get(), RenderType.translucent());
            // v0.1.20 fix: SANGUINE_SAPLING precisa de render layer cutout pra
            // não renderizar com fundo preto. Bloco cross sem cutout = pixels
            // transparentes da textura viram pretos opacos.
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.SANGUINE_SAPLING.get(), RenderType.cutout());
            // Mesma issue: tocha de sangue (torch model usa cutout vanilla).
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.BLOOD_TORCH.get(), RenderType.cutout());

            // --- Blood Tree family — sapling (cross model), leaves (cutout_mipped
            //     pra folhas com transparência), door/trapdoor (cutout pra
            //     furos no model vanilla door_bottom_left etc).
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.BLOOD_SAPLING.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.BLOOD_LEAVES.get(), RenderType.cutoutMipped());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.BLOOD_DOOR.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.BLOOD_TRAPDOOR.get(), RenderType.cutout());
        });
    }
}
