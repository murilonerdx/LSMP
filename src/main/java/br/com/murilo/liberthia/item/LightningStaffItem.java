package br.com.murilo.liberthia.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Lightning Staff — while active, every ~30 ticks calls down a real lightning
 * bolt on the nearest hostile mob in a 12-block radius. Player-safe.
 */
public class LightningStaffItem extends Item {
    public LightningStaffItem(Properties props) {
        super(props.durability(120));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        StaffActiveLogic.handleToggle(stack, player);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level l, List<Component> tip, TooltipFlag f) {
        tip.add(Component.literal("§eRaio recorrente em mob mais próximo (12 blocos)"));
        tip.add(Component.literal("§7§oRight-click: ligar/desligar (20s)"));
        if (l != null && StaffActiveLogic.isActive(stack, l)) {
            tip.add(Component.literal("§a● ATIVO §7(" + StaffActiveLogic.remainingSeconds(stack, l) + "s)"));
        }
    }
}
