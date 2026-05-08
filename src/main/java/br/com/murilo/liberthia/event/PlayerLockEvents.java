package br.com.murilo.liberthia.event;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.item.PlayerLockItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Centralised enforcement of the {@link PlayerLockItem} state.
 *
 * Locked players (NBT flag set) every tick:
 *   • position is hard-snapped back to the lock anchor;
 *   • velocity zeroed;
 *   • any open external container is force-closed.
 *
 * They can also no longer attack, interact with blocks/entities, drop items,
 * or open chests/etc. Pressing E technically still opens their inventory
 * client-side, but every server-side action through it is cancelled.
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class PlayerLockEvents {
    private PlayerLockEvents() {}

    // ---------------------------------------------------------------- per-tick anchor
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent ev) {
        if (ev.phase != TickEvent.Phase.END) return;
        if (!(ev.player instanceof ServerPlayer sp)) return;
        if (!PlayerLockItem.isLocked(sp)) return;

        var data = sp.getPersistentData();
        double ax = data.getDouble(PlayerLockItem.NBT_LOCK_X);
        double ay = data.getDouble(PlayerLockItem.NBT_LOCK_Y);
        double az = data.getDouble(PlayerLockItem.NBT_LOCK_Z);

        // Zero velocity AND snap them back to the anchor every tick.
        sp.setDeltaMovement(Vec3.ZERO);
        sp.hurtMarked = true;
        if (sp.distanceToSqr(ax, ay, az) > 0.01) {
            sp.connection.teleport(ax, ay, az, sp.getYRot(), sp.getXRot());
        }

        // If they managed to open any container (menu changed), close it.
        if (sp.containerMenu != sp.inventoryMenu) {
            sp.closeContainer();
        }
    }

    // ---------------------------------------------------------------- block all interactions
    @SubscribeEvent
    public static void onContainerOpen(PlayerContainerEvent.Open ev) {
        if (PlayerLockItem.isLocked(ev.getEntity())) {
            // Force-close immediately on the next tick — Forge doesn't expose
            // setCanceled here, but closing the container has the same effect.
            if (ev.getEntity() instanceof ServerPlayer sp) sp.closeContainer();
        }
    }

    @SubscribeEvent
    public static void onAttack(AttackEntityEvent ev) {
        if (PlayerLockItem.isLocked(ev.getEntity())) ev.setCanceled(true);
    }

    @SubscribeEvent
    public static void onTossItem(ItemTossEvent ev) {
        if (PlayerLockItem.isLocked(ev.getPlayer())) {
            ev.setCanceled(true);
            // Put the dropped stack back where it came from (the player tried
            // to Q-drop it; cancelling alone leaves the item entity alive).
            ev.getEntity().discard();
            Player p = ev.getPlayer();
            if (!ev.getEntity().getItem().isEmpty()) {
                p.getInventory().add(ev.getEntity().getItem());
            }
        }
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock ev) {
        if (PlayerLockItem.isLocked(ev.getEntity())) ev.setCanceled(true);
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock ev) {
        if (PlayerLockItem.isLocked(ev.getEntity())) ev.setCanceled(true);
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem ev) {
        if (PlayerLockItem.isLocked(ev.getEntity())) ev.setCanceled(true);
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract ev) {
        if (PlayerLockItem.isLocked(ev.getEntity())) ev.setCanceled(true);
    }

    // Take 0 damage during lockdown? No — keep them mortal so they can be killed.
    // But if you want them invincible, uncomment:
    // @SubscribeEvent public static void onAttacked(LivingAttackEvent ev) {
    //     if (ev.getEntity() instanceof Player p && PlayerLockItem.isLocked(p)) ev.setCanceled(true);
    // }
}
