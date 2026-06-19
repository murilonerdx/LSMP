package br.com.murilo.liberthia.magic.spell;

import br.com.murilo.liberthia.LiberthiaMod;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * v0.1.145 r113: <b>ScreenShakeClient</b> — aplica o shake na câmera
 * client-side. Decai a intensidade por tick. Modifica yaw/pitch via
 * {@link ViewportEvent.ComputeCameraAngles}.
 *
 * <p>Padrão Forge moderno (1.19+): em vez de usar reflection na Camera,
 * inscrevemos no evento de cálculo de ângulo e adicionamos deslocamento
 * randomico. Original — sem copiar de outros mods.
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID, value = Dist.CLIENT)
public final class ScreenShakeClient {

    /** Intensidade atual (0-2). Decai 1/duration por tick. */
    private static float intensity = 0F;
    private static int remainingTicks = 0;
    private static float initialIntensity = 0F;
    private static int totalDuration = 0;

    private ScreenShakeClient() {}

    /** Server packet handler chama isso. */
    public static void trigger(float i, int duration) {
        // Se já tem shake ativo, usa o maior dos dois (evita stacking infinito)
        if (intensity > 0F && i < intensity) return;
        intensity = i;
        initialIntensity = i;
        remainingTicks = duration;
        totalDuration = duration;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent ev) {
        if (ev.phase != TickEvent.Phase.END) return;
        if (remainingTicks > 0) {
            remainingTicks--;
            // Linear decay
            float progress = (float) remainingTicks / Math.max(1, totalDuration);
            intensity = initialIntensity * progress;
            if (remainingTicks <= 0) {
                intensity = 0F;
                initialIntensity = 0F;
                totalDuration = 0;
            }
        }
    }

    @SubscribeEvent
    public static void onCamera(ViewportEvent.ComputeCameraAngles ev) {
        if (intensity <= 0F) return;
        // Random offset proportional to intensity (max ~6° at full intensity)
        float maxOffset = intensity * 6.0F;
        float dYaw = (float)((Math.random() - 0.5) * maxOffset);
        float dPitch = (float)((Math.random() - 0.5) * maxOffset);
        float dRoll = (float)((Math.random() - 0.5) * maxOffset * 0.5F);
        ev.setYaw(ev.getYaw() + dYaw);
        ev.setPitch(ev.getPitch() + dPitch);
        ev.setRoll(ev.getRoll() + dRoll);
    }
}
