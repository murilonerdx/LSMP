package br.com.murilo.liberthia.client.hud;

import br.com.murilo.liberthia.LiberthiaMod;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Keybind {@code F8} (default) — abre/fecha o <b>editor unificado de HUD</b>
 * ({@link br.com.murilo.liberthia.client.hud.unified.UnifiedHudEditorScreen}),
 * onde TODOS os HUDs do mod podem ser arrastados livremente.
 *
 * <p>report #80 fix: antes o F8 ciclava o {@code HudPosition} (4 cantos) de um
 * sistema ANTIGO que o {@code MatterProfileHud} não lê mais (ele renderiza pela
 * posição do sistema unificado, {@code ClientHudPositions}+{@code HudId}). Por
 * isso o F8 mostrava a mensagem mas o HUD não saía do lugar. Agora o F8 abre o
 * mesmo editor de {@code /liberthia hud editor}.
 *
 * <p>Para ver/mudar a tecla: §lOptions → Controls → Liberthia§r.
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
                Minecraft mc = Minecraft.getInstance();
                if (mc.player == null) continue;
                // #80: toggle o editor unificado de HUD (arrasta qualquer HUD).
                if (mc.screen instanceof br.com.murilo.liberthia.client.hud.unified.UnifiedHudEditorScreen) {
                    mc.setScreen(null);
                } else if (mc.screen == null) {
                    mc.setScreen(new br.com.murilo.liberthia.client.hud.unified.UnifiedHudEditorScreen());
                }
            }
        }
    }
}
