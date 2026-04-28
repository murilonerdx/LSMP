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
 * Thorn Staff — toggle aura that for 20s pulses Wither + Blood Infection in
 * a 5-block radius around the holder. Right-click to activate, shift +
 * right-click to deactivate. Durability decays while active (handled in
 * {@link br.com.murilo.liberthia.event.StaffAuraEvents}).
 */
public class ThornStaffItem extends Item {
    public ThornStaffItem(Properties props) {
        super(props.durability(220));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        StaffActiveLogic.handleToggle(stack, player);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level l, List<Component> tip, TooltipFlag f) {
        tip.add(Component.literal("§7Pulsa §c2 ❤§7 + Wither II + Infecção"));
        tip.add(Component.literal("§7em mobs num raio de §a5 blocos§7"));
        tip.add(Component.literal("§7§oRight-click: ligar/desligar (20s)"));
        if (l != null && StaffActiveLogic.isActive(stack, l)) {
            tip.add(Component.literal("§a● ATIVO §7(" + StaffActiveLogic.remainingSeconds(stack, l) + "s)"));
        }
    }
}
