package br.com.murilo.liberthia.client.hud;

import br.com.murilo.liberthia.LiberthiaMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * v0.1.24 r102: Registra os 3 HUD overlays na inicialização client.
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD,
        value = net.minecraftforge.api.distmarker.Dist.CLIENT)
public final class HudOverlayRegistry {

    private HudOverlayRegistry() {}

    @SubscribeEvent
    public static void onRegisterOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAbove(VanillaGuiOverlay.HOTBAR.id(), "cast_bar",
                new CastBarOverlay());
        event.registerAbove(VanillaGuiOverlay.HOTBAR.id(), "recast_counter",
                new RecastCounterOverlay());
        event.registerAbove(VanillaGuiOverlay.VIGNETTE.id(), "screen_effects",
                new ScreenEffectsOverlay());
    }
}
