package br.com.murilo.liberthia.event;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.capture.CapturedPlayerManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class CaptureEvents {

    private CaptureEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Player player = event.player;

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        if (!CapturedPlayerManager.isCaptured(serverPlayer)) {
            return;
        }

        CapturedPlayerManager.get(serverPlayer).ifPresent(state -> {
            serverPlayer.teleportTo(
                    serverPlayer.serverLevel(),
                    state.prisonAnchor().getX() + 0.5D,
                    state.prisonAnchor().getY(),
                    state.prisonAnchor().getZ() + 0.5D,
                    serverPlayer.getYRot(),
                    serverPlayer.getXRot()
            );

            serverPlayer.setDeltaMovement(0, 0, 0);
            serverPlayer.hurtMarked = true;
            serverPlayer.setInvisible(true);
            serverPlayer.setInvulnerable(true);
        });
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getEntity() instanceof ServerPlayer player && CapturedPlayerManager.isCaptured(player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getEntity() instanceof ServerPlayer player && CapturedPlayerManager.isCaptured(player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getEntity() instanceof ServerPlayer player && CapturedPlayerManager.isCaptured(player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getEntity() instanceof ServerPlayer player && CapturedPlayerManager.isCaptured(player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onContainerOpen(PlayerContainerEvent.Open event) {
        if (event.getEntity() instanceof ServerPlayer player && CapturedPlayerManager.isCaptured(player)) {
            event.setCanceled(true);
            player.closeContainer();
        }
    }

    @SubscribeEvent
    public static void onCapturedPlayerAttack(LivingAttackEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && CapturedPlayerManager.isCaptured(player)) {
            event.setCanceled(true);
        }
    }
}
