package br.com.murilo.liberthia.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Sanguine Ward Pickaxe — diamond-tier. While held in either hand, slowly
 * heals the wielder's Blood Infection drain counter (handled in
 * {@link br.com.murilo.liberthia.event.SanguineWardEvents}).
 */
public class SanguineWardPickaxeItem extends PickaxeItem {
    public SanguineWardPickaxeItem(Properties props) {
        super(Tiers.DIAMOND, 1, -2.8F, props);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tip, flag);
        tip.add(Component.literal("§7Enquanto na mão: §acura passiva §7do dreno de sangue."));
    }
}
