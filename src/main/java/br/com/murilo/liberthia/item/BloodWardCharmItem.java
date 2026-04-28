package br.com.murilo.liberthia.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Blood Ward Charm — passive trinket. Held in offhand, gives a 50% chance to
 * block fresh Blood Infection applications (rolled per-tick by
 * {@link br.com.murilo.liberthia.event.SanguineWardEvents}).
 *
 * Stackable to 1 so the player can't double up.
 */
public class BloodWardCharmItem extends Item {
    public BloodWardCharmItem(Properties props) {
        super(props);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tip, flag);
        tip.add(Component.literal("§7Segure na mão secundária para ganhar §c50% §7de"));
        tip.add(Component.literal("§7resistência a Infecção de Sangue."));
        tip.add(Component.literal("§oFunciona empilhado com a armadura Sanguine Ward."));
    }
}
