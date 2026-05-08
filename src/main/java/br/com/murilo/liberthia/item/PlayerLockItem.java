package br.com.murilo.liberthia.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Player Lock — right-click another player to freeze them in place.
 * The frozen player can't move, attack, drop items, or open chests.
 *
 * Right-click the same player again with this item to unlock them.
 *
 * NBT tags written on the VICTIM (not on the item):
 *   liberthia_locked       — boolean flag
 *   liberthia_lock_x/y/z   — anchor position (so they snap back if shoved)
 *
 * Op-only to prevent grief.
 */
public class PlayerLockItem extends Item {

    public static final String NBT_LOCKED = "liberthia_locked";
    public static final String NBT_LOCK_X = "liberthia_lock_x";
    public static final String NBT_LOCK_Y = "liberthia_lock_y";
    public static final String NBT_LOCK_Z = "liberthia_lock_z";

    public PlayerLockItem(Properties props) {
        super(props.stacksTo(1));
    }

    public static boolean isLocked(Player player) {
        return player.getPersistentData().getBoolean(NBT_LOCKED);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player user, LivingEntity target, InteractionHand hand) {
        if (user.level().isClientSide) return InteractionResult.SUCCESS;
        if (!(user instanceof ServerPlayer holder)) return InteractionResult.PASS;

        // Op gate.
        if (!holder.hasPermissions(2)) {
            holder.displayClientMessage(
                    Component.literal("Apenas operadores podem usar este item.")
                            .withStyle(ChatFormatting.RED), true);
            return InteractionResult.FAIL;
        }

        if (!(target instanceof ServerPlayer victim)) return InteractionResult.PASS;
        if (victim == holder) {
            holder.displayClientMessage(
                    Component.literal("Você não pode se trancar.").withStyle(ChatFormatting.RED), true);
            return InteractionResult.FAIL;
        }

        if (isLocked(victim)) {
            // Unlock.
            victim.getPersistentData().putBoolean(NBT_LOCKED, false);
            victim.level().playSound(null, victim.blockPosition(),
                    SoundEvents.IRON_DOOR_OPEN, SoundSource.PLAYERS, 1.0F, 1.2F);
            holder.displayClientMessage(
                    Component.literal("Liberou: " + victim.getGameProfile().getName())
                            .withStyle(ChatFormatting.GREEN), true);
            victim.displayClientMessage(
                    Component.literal("Você foi liberado.").withStyle(ChatFormatting.GREEN), true);
        } else {
            // Lock — store current pos as anchor.
            var data = victim.getPersistentData();
            data.putBoolean(NBT_LOCKED, true);
            data.putDouble(NBT_LOCK_X, victim.getX());
            data.putDouble(NBT_LOCK_Y, victim.getY());
            data.putDouble(NBT_LOCK_Z, victim.getZ());
            // Force-close anything they had open.
            if (victim.containerMenu != victim.inventoryMenu) {
                victim.closeContainer();
            }
            victim.level().playSound(null, victim.blockPosition(),
                    SoundEvents.IRON_DOOR_CLOSE, SoundSource.PLAYERS, 1.0F, 0.6F);
            holder.displayClientMessage(
                    Component.literal("Trancou: " + victim.getGameProfile().getName())
                            .withStyle(ChatFormatting.GOLD), true);
            victim.displayClientMessage(
                    Component.literal("§4Você foi trancado por um operador.")
                            .withStyle(ChatFormatting.RED), false);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.literal("§7Right-click num player → §ctranca §7em lugar."));
        tip.add(Component.literal("§7Click de novo → §asolta§7."));
        tip.add(Component.literal("§o§8(Apenas operadores)"));
    }
}
