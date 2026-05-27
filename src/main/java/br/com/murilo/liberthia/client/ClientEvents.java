package br.com.murilo.liberthia.client;

import br.com.murilo.liberthia.LiberthiaMod;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ClientEvents {
    private ClientEvents() {
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        if (KeyBindings.HUD_CONFIG_KEY.consumeClick()) {
            Minecraft.getInstance().setScreen(new HudConfigScreen());
        }
        // r164: O X (SPELL_WHEEL_KEY) agora é tratado em SpellHotbarKeyHandler.
        // Se player segura Grimoire → abre GrimoireWheelScreen (9 slots).
        // Senão → abre o Custom Spell Wheel. Removido daqui pra evitar double-consume.
        // r42: Quick Cast — V casta o spell selecionado sem abrir wheel
        if (KeyBindings.QUICK_CAST_KEY.consumeClick()) {
            var selected = br.com.murilo.liberthia.magic.custom.CustomSpellClientCache.getSelected();
            if (selected != null) {
                br.com.murilo.liberthia.network.ModNetwork.CHANNEL.sendToServer(
                        new br.com.murilo.liberthia.magic.custom.CastCustomSpellC2SPacket(selected.id));
            } else {
                Minecraft mc = Minecraft.getInstance();
                if (mc.player != null) {
                    mc.player.displayClientMessage(
                            net.minecraft.network.chat.Component.literal(
                                    "§7Nenhum feitiço selecionado — abra §dX§7 pra escolher."), true);
                }
            }
        }
    }

    private static long lastGeigerTick = 0;

    @SubscribeEvent
    public static void onClientTick(net.minecraftforge.event.TickEvent.ClientTickEvent event) {
        if (event.phase != net.minecraftforge.event.TickEvent.Phase.END) return;
        
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.player != null && !mc.isPaused()) {
            if (br.com.murilo.liberthia.config.DevMode.ACTIVE) return;
            int exposure = ClientInfectionState.getEffectiveExposure();
            if (exposure > 0) {
                // Geiger Tick rate: 1000ms / exposure (Caps at 50ms)
                long interval = Math.max(50, 1000 / exposure);
                long now = System.currentTimeMillis();
                if (now - lastGeigerTick >= interval) {
                    mc.level.playLocalSound(mc.player.getX(), mc.player.getY(), mc.player.getZ(), 
                            br.com.murilo.liberthia.registry.ModSounds.GEIGER_TICK.get(), 
                            net.minecraft.sounds.SoundSource.MASTER, 
                            0.2F + (exposure / 100.0F), 1.0F, false);
                    lastGeigerTick = now;
                }
            }
        }
    }

    @SubscribeEvent
    public static void onFogColor(net.minecraftforge.client.event.ViewportEvent.ComputeFogColor event) {
        if (br.com.murilo.liberthia.config.DevMode.ACTIVE) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.player != null) {
            float density = ClientInfectionState.getChunkDensity();
            if (density > 0.1f) {
                float r = event.getRed();
                float g = event.getGreen();
                float b = event.getBlue();

                // Shift towards Dark Purple (#2A0033)
                event.setRed(net.minecraft.util.Mth.lerp(density * 0.7f, r, 0.16f));
                event.setGreen(net.minecraft.util.Mth.lerp(density * 0.7f, g, 0.0f));
                event.setBlue(net.minecraft.util.Mth.lerp(density * 0.7f, b, 0.2f));
            }
        }
    }

    /**
     * F6: Corrupted Sky — reduce fog distance in infected areas
     */
    @SubscribeEvent
    public static void onRenderFog(net.minecraftforge.client.event.ViewportEvent.RenderFog event) {
        if (br.com.murilo.liberthia.config.DevMode.ACTIVE) return;
        float density = ClientInfectionState.getChunkDensity();
        if (density > 0.2f) {
            float factor = 1.0f - density * 0.5f;
            event.setFarPlaneDistance(event.getFarPlaneDistance() * factor);
            event.setNearPlaneDistance(event.getNearPlaneDistance() * factor);
            event.setCanceled(true);
        }
    }
}
