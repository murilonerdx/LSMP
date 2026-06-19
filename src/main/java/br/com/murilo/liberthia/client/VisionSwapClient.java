package br.com.murilo.liberthia.client;

import br.com.murilo.liberthia.LiberthiaMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

/**
 * Estado client-side da Feature Vision Swap.
 *
 * <p>Trabalha em duas etapas:
 * <ol>
 *   <li>{@link #start(UUID)} chamado por {@code StartVisionSwapS2CPacket}:
 *       resolve a entidade target no level atual e chama
 *       {@code mc.setCameraEntity(target)}. Marca {@link #active} para que os
 *       eventos de tick e movimento o bloqueiem.</li>
 *   <li>{@link #end()} chamado por {@code EndVisionSwapS2CPacket} (ou de
 *       qualquer cleanup): restaura {@code setCameraEntity(localPlayer)} e
 *       libera o lock.</li>
 * </ol>
 *
 * <p>Observação importante de side: o corpo físico do user NÃO é movido.
 * Apenas a câmera passa a renderizar pela entidade alvo. Isso é o
 * comportamento padrão e bem-suportado de {@code Minecraft.setCameraEntity};
 * é o mesmo mecanismo usado por spectators e cinematic cameras.
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID, value = Dist.CLIENT)
public final class VisionSwapClient {

    /** True enquanto a câmera está redirecionada — bloqueia inputs locais. */
    private static volatile boolean active = false;
    private static UUID currentTarget;

    private VisionSwapClient() {}

    /** Setup da câmera. Idempotente: chamadas repetidas re-resolvem o target. */
    public static void start(UUID targetUuid) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        Entity target = null;
        // mc.level.players() é o melhor caminho client-side para achar players visíveis.
        for (Player p : mc.level.players()) {
            if (p.getUUID().equals(targetUuid)) { target = p; break; }
        }
        if (target == null) {
            // Não foi possível resolver — provavelmente o target está em outra dim
            // do ponto de vista do cliente, ou ainda não foi sincronizado. Marcamos
            // active mesmo assim, pra bloquear input localmente; o server vai
            // mandar EndVisionSwapS2CPacket quando expirar/cancelar.
            active = true;
            currentTarget = targetUuid;
            return;
        }
        mc.setCameraEntity(target);
        currentTarget = targetUuid;
        active = true;
    }

    /** Restaura câmera ao próprio player e libera input. */
    public static void end() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.setCameraEntity(mc.player);
        }
        active = false;
        currentTarget = null;
    }

    public static boolean isActive() {
        return active;
    }

    /**
     * Bloqueia inputs WASD/jump/sneak/sprint do user enquanto a câmera estiver
     * redirecionada. Sem isso o LocalPlayer mexeria o próprio corpo invisível
     * pra ele, podendo cair em buracos.
     */
    @SubscribeEvent
    public static void onMovementInput(MovementInputUpdateEvent event) {
        if (!active) return;
        var input = event.getInput();
        input.forwardImpulse = 0f;
        input.leftImpulse = 0f;
        input.up = false;
        input.down = false;
        input.left = false;
        input.right = false;
        input.jumping = false;
        input.shiftKeyDown = false;
    }

    /**
     * Tick periódico: se a entidade alvo desaparecer (ex.: foi unloaded), o
     * server eventualmente manda EndVisionSwap, mas até lá garantimos que a
     * câmera não fique apontando para entity nula. Também re-tenta resolver
     * o target caso o primeiro start não tenha conseguido.
     */
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!active) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        Entity cam = mc.getCameraEntity();
        if (currentTarget != null && (cam == null || cam == mc.player || cam.isRemoved())) {
            // re-tenta resolver
            for (Player p : mc.level.players()) {
                if (p.getUUID().equals(currentTarget)) {
                    mc.setCameraEntity(p);
                    return;
                }
            }
        }
    }

    /** Se o player sair do server por qualquer motivo, restaura câmera. */
    @SubscribeEvent
    public static void onClientDisconnect(ClientPlayerNetworkEvent.LoggingOut event) {
        if (active) end();
    }
}
