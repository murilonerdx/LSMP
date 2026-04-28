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
 * Magnetic Wand — while active, every tick pulls hostile mobs in a 12-block
 * radius toward the holder; mobs within 1.6 blocks take damage + Slowness.
 * Player-safe.
 */
public class MagneticWandItem extends Item {
    public MagneticWandItem(Properties props) {
        super(props.durability(180));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        StaffActiveLogic.handleToggle(stack, player);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level l, List<Component> tip, TooltipFlag f) {
        tip.add(Component.literal("§5Atrai mobs (12 blocos), esmaga a ≤1.6"));
        tip.add(Component.literal("§7§oRight-click: ligar/desligar (20s)"));
        if (l != null && StaffActiveLogic.isActive(stack, l)) {
            tip.add(Component.literal("§a● ATIVO §7(" + StaffActiveLogic.remainingSeconds(stack, l) + "s)"));
        }
    }
}
