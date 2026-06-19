package br.com.murilo.liberthia.observation.client;

import br.com.murilo.liberthia.LiberthiaMod;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * v0.1.22 r73: <b>Keybind handler</b> pra HUD do Source.
 *
 * <ul>
 *   <li><b>H</b> — cycle HudPosition (BOTTOM_LEFT → BOTTOM_RIGHT → TOP_LEFT → TOP_RIGHT → CENTER_TOP → loop)</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID, value = Dist.CLIENT)
public final class HudKeybinds {

    public static final KeyMapping CYCLE_HUD_POS = new KeyMapping(
        "key.liberthia.cycle_hud_pos",
        KeyConflictContext.IN_GAME,
        InputConstants.Type.KEYSYM,
        InputConstants.KEY_H,
        "key.categories.liberthia"
    );

    private HudKeybinds() {}

    /** Mod-bus event: registra a keybind. */
    @Mod.EventBusSubscriber(modid = LiberthiaMod.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class ModBus {
        @SubscribeEvent
        public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
            event.register(CYCLE_HUD_POS);
        }
    }

    /** Forge-bus tick: checa se foi pressed (no main hand only, client side). */
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return;

        while (CYCLE_HUD_POS.consumeClick()) {
            // Cycle position
            SourceHud.HudPosition[] all = SourceHud.HudPosition.values();
            int idx = SourceHud.position.ordinal();
            SourceHud.position = all[(idx + 1) % all.length];
            mc.player.displayClientMessage(Component.literal(
                "§5§l✦ HUD: §e" + SourceHud.position.name()), true);
        }
    }
}
