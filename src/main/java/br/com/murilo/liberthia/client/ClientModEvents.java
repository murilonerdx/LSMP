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
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientModEvents {
    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.BLACK_HOLE.get(), BlackHoleRenderer::new);
        event.registerEntityRenderer(ModEntities.DARK_MATTER_SPORE.get(), br.com.murilo.liberthia.client.renderer.DarkMatterSporeRenderer::new);
        event.registerEntityRenderer(ModEntities.CLEANSING_GRENADE.get(), br.com.murilo.liberthia.client.renderer.CleansingGrenadeRenderer::new);
    }

    @SubscribeEvent
    public static void onRegisterGuiOverlays(net.minecraftforge.client.event.RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("infection_hud", InfectionHudOverlay.INSTANCE);
        event.registerAboveAll("matter_energy_hud", MatterEnergyHudOverlay.INSTANCE);
        event.registerAboveAll("dna_mutation_hud", DnaMutationOverlay.INSTANCE);
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(ModMenuTypes.PURIFICATION_BENCH.get(), PurificationBenchScreen::new);
            MenuScreens.register(ModMenuTypes.DARK_MATTER_FORGE.get(), DarkMatterForgeScreen::new);
            MenuScreens.register(ModMenuTypes.MATTER_INFUSER.get(), MatterInfuserScreen::new);
            MenuScreens.register(ModMenuTypes.RESEARCH_TABLE.get(), ResearchTableScreen::new);
            MenuScreens.register(ModMenuTypes.CONTAINMENT_CHAMBER.get(), ContainmentChamberScreen::new);
            MenuScreens.register(ModMenuTypes.MATTER_TRANSMUTER.get(), MatterTransmuterScreen::new);
        });
    }
}
