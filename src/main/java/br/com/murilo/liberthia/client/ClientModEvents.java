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
        });
    }
}
