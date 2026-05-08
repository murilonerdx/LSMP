package br.com.murilo.liberthia.client.hud;

import br.com.murilo.liberthia.LiberthiaMod;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
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
 * Keybind {@code F8} (default) — cicla a posição do HUD de matéria entre os
 * 4 cantos da tela. Estado persiste em {@code config/liberthia_hud.txt}.
 *
 * <p>Para ver/mudar: §lOptions → Controls → Liberthia§r.
 *
 * <p>É registrado em DUAS event-bus:
 * <ul>
 *   <li>{@link RegisterMod} no MOD bus (registra a KeyMapping)</li>
 *   <li>{@link TickHandler} no FORGE bus (processa cliques no game loop)</li>
 * </ul>
 */
public final class MatterHudKeybind {

    public static final String CATEGORY = "key.categories.liberthia";
    public static final String NAME = "key.liberthia.cycle_matter_hud";
    public static KeyMapping CYCLE;

    private MatterHudKeybind() {}

    /** Roda no MOD bus — RegisterKeyMappingsEvent fire-a aí. */
    @Mod.EventBusSubscriber(modid = LiberthiaMod.MODID, value = Dist.CLIENT,
            bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class RegisterMod {
        @SubscribeEvent
        public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
            CYCLE = new KeyMapping(NAME, KeyConflictContext.IN_GAME,
                    InputConstants.Type.KEYSYM,
                    InputConstants.KEY_F8,
                    CATEGORY);
            event.register(CYCLE);
            HudPosition.load();
        }
    }

    /** Roda no FORGE bus — ClientTickEvent fire-a aí. */
    @Mod.EventBusSubscriber(modid = LiberthiaMod.MODID, value = Dist.CLIENT,
            bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static final class TickHandler {
        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            if (CYCLE == null) return;
            while (CYCLE.consumeClick()) {
                HudPosition pos = HudPosition.cycle();
                Minecraft mc = Minecraft.getInstance();
                if (mc.player != null) {
                    mc.player.displayClientMessage(
                            Component.literal("HUD: " + pos.name())
                                    .withStyle(ChatFormatting.LIGHT_PURPLE), true);
                }
            }
        }
    }
}
