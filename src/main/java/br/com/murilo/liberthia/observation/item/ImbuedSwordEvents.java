package br.com.murilo.liberthia.observation.item;

import br.com.murilo.liberthia.LiberthiaMod;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * v0.1.22 r71: Event hooks pro sistema de Imbued Sword.
 *
 * <ul>
 *   <li><b>Sneak + Right-click</b> com sword na main + parchment na offhand:
 *       imbue (consume parchment + 1 soul fragment do inventory)</li>
 *   <li><b>AttackEntityEvent</b>: se sword é imbued, 25% proc cast no hit</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class ImbuedSwordEvents {

    private ImbuedSwordEvents() {}

    /** Sneak+rclick com sword + parchment offhand = imbue. */
    @SubscribeEvent
    public static void onRightClick(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) return;
        if (!player.isShiftKeyDown()) return;

        ItemStack main = player.getMainHandItem();
        ItemStack off = player.getOffhandItem();

        // Main deve ser sword
        if (!(main.getItem() instanceof SwordItem)) return;
        // Off deve ser parchment com recipe
        if (!(off.getItem() instanceof SpellParchmentItem)) return;
        if (SpellParchmentItem.getRecipe(off).isEmpty()) return;

        // Verifica se tem Soul Fragment no inventory
        int slotWithFragment = -1;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (s.getItem() == br.com.murilo.liberthia.registry.ModItems.SOUL_FRAGMENT.get()) {
                slotWithFragment = i;
                break;
            }
        }
        if (slotWithFragment == -1) {
            if (player instanceof ServerPlayer sp) {
                sp.displayClientMessage(Component.literal(
                    "§c⚠ Precisa de §5Fragmento de Alma§c no inventário."), true);
            }
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
            return;
        }

        // Verifica se sword já é imbued
        if (ImbuedSwordHandler.isImbued(main)) {
            if (player instanceof ServerPlayer sp) {
                sp.displayClientMessage(Component.literal(
                    "§c⚠ Espada já está encantada. Use outra espada."), true);
            }
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
            return;
        }

        // Imbue!
        ImbuedSwordHandler.imbueSword(main, off);
        off.shrink(1); // consome parchment
        player.getInventory().getItem(slotWithFragment).shrink(1); // consome 1 fragment

        if (player instanceof ServerPlayer sp) {
            var spell = ImbuedSwordHandler.buildSpellFromSword(main);
            String name = spell != null ? spell.name() : "Imbued";
            sp.displayClientMessage(Component.literal(
                "§5§l✦ Espada encantada com §r§e" + name + "§5§l ✦"), false);
            sp.level().playSound(null, sp.blockPosition(),
                net.minecraft.sounds.SoundEvents.ENCHANTMENT_TABLE_USE,
                net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 1.0f);
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }

    /** Hit em mob: 25% proc cast se sword é imbued. */
    @SubscribeEvent
    public static void onAttack(AttackEntityEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) return;
        ItemStack main = player.getMainHandItem();
        if (!(main.getItem() instanceof SwordItem)) return;
        if (!ImbuedSwordHandler.isImbued(main)) return;
        if (event.getTarget() instanceof net.minecraft.world.entity.LivingEntity target) {
            ImbuedSwordHandler.tryProcOnHit(main, player, target);
        }
    }
}
