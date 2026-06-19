package br.com.murilo.liberthia.event;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.item.AstaronAccessKeyItem;
import br.com.murilo.liberthia.item.FlameKeyItem;
import br.com.murilo.liberthia.registry.ModItems;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

/**
 * v0.1.22 r13/r14: regras das keys do mod.
 *
 * <ul>
 *   <li><b>r13 — pickup restrito ao owner:</b> só o owner gravado no NBT
 *       consegue pegar a key dropada. Outros players passam pelo item.</li>
 *   <li><b>r14 — drop bloqueado completamente:</b> player não consegue
 *       dropar as keys (Q, inventory toss). User pediu: "não permita que
 *       os players consigam de jeito nenhum dropar essas chaves, nem
 *       outros jogadores pegarem".</li>
 * </ul>
 *
 * <p>Se a key não tem owner (NBT corrompido), pickup é permitido —
 * comportamento seguro pra não soft-lockar items.
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class KeyPickupRestrictionHandler {

    private KeyPickupRestrictionHandler() {}

    /** Bloqueia outros players de pegar a key alheia. */
    @SubscribeEvent
    public static void onItemPickup(EntityItemPickupEvent event) {
        ItemStack stack = event.getItem().getItem();
        if (stack.isEmpty()) return;

        UUID owner = null;
        if (stack.is(ModItems.FLAME_KEY.get())) {
            owner = FlameKeyItem.getOwner(stack);
        } else if (stack.is(ModItems.ASTARON_ACCESS_KEY.get())) {
            owner = AstaronAccessKeyItem.getOwner(stack);
        } else {
            return;
        }
        if (owner == null) return;
        Player player = event.getEntity();
        if (!player.getUUID().equals(owner)) {
            event.setCanceled(true);
        }
    }

    /**
     * v0.1.22 r14: BLOQUEIA o drop das keys. User: "não permita que os
     * players consigam de jeito nenhum dropar essas chaves". Cobre Q-drop
     * e drag-out do inventário.
     */
    @SubscribeEvent
    public static void onItemToss(ItemTossEvent event) {
        ItemStack stack = event.getEntity().getItem();
        if (stack.isEmpty()) return;
        if (!stack.is(ModItems.FLAME_KEY.get())
                && !stack.is(ModItems.ASTARON_ACCESS_KEY.get())) return;

        // Cancela toss e devolve ao inventário do player.
        event.setCanceled(true);
        Player player = event.getPlayer();
        if (player != null) {
            player.getInventory().add(stack);
        }
        event.getEntity().discard();
    }

    /**
     * v0.1.22 r14: na MORTE do player, remove as keys do drop list — não
     * caem no chão. Garante que outro player não pode catar a key alheia
     * mesmo após PvP kill. Owner sobrevive, key some (ela é re-spawnável
     * via re-equipar relíquia/botas — flag NBT do player também é resetada
     * no respawn já que persistentData não persiste).
     */
    @SubscribeEvent
    public static void onPlayerDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        event.getDrops().removeIf(itemEntity -> {
            ItemStack s = itemEntity.getItem();
            return s.is(ModItems.FLAME_KEY.get()) || s.is(ModItems.ASTARON_ACCESS_KEY.get());
        });
    }
}
