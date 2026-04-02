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
    }

    private static long lastGeigerTick = 0;

    @SubscribeEvent
    public static void onClientTick(net.minecraftforge.event.TickEvent.ClientTickEvent event) {
        if (event.phase != net.minecraftforge.event.TickEvent.Phase.END) return;
        
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.player != null && !mc.isPaused()) {
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
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.player != null) {
            float density = br.com.murilo.liberthia.logic.InfectionLogic.getChunkInfectionDensity(mc.level, mc.player.blockPosition());
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
}
