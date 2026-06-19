package br.com.murilo.liberthia.cosmic.client;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.cosmic.CosmicClientState;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Random;

/**
 * v0.1.22 r35: <b>Camera Shake / Jitter</b> client-side.
 *
 * <p>A cada client tick (50ms), perturba ligeiramente yaw/pitch do player local
 * com magnitude {@link CosmicClientState#currentCameraJitter}. NÃO faz move
 * real (não acumula posição), apenas rotação.
 *
 * <p>Pra shake REAL (translation), seria necessário Mixin em
 * {@code Camera.setRotation} ou {@code Entity.getRopeHoldPosition} — ver
 * {@link CosmicCameraMixin}.
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID, value = Dist.CLIENT)
public final class CosmicCameraShake {

    private static final Random RNG = new Random();
    private static float lastJitterX = 0;
    private static float lastJitterY = 0;

    private CosmicCameraShake() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        float jitter = CosmicClientState.currentCameraJitter;
        if (jitter < 0.001F) return;

        // Undo last jitter pra acumulação não desviar
        mc.player.setYRot(mc.player.getYRot() - lastJitterX);
        mc.player.setXRot(mc.player.getXRot() - lastJitterY);

        // Aplica novo jitter
        lastJitterX = (RNG.nextFloat() - 0.5F) * jitter * 8.0F;
        lastJitterY = (RNG.nextFloat() - 0.5F) * jitter * 4.0F;
        mc.player.setYRot(mc.player.getYRot() + lastJitterX);
        mc.player.setXRot(Math.max(-90, Math.min(90,
                mc.player.getXRot() + lastJitterY)));
    }
}
