package br.com.murilo.liberthia.item;

import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;

/**
 * Crude cult weapon dropped by Blood Cultists. Stone-tier damage but slow.
 * Not enchantable, low durability — meant to be a flavor drop, not a path.
 */
public class RustedDaggerItem extends SwordItem {
    public RustedDaggerItem(Properties p) {
        super(Tiers.STONE, 1, -2.4F, p.durability(120));
    }

    @Override public boolean isEnchantable(net.minecraft.world.item.ItemStack stack) { return false; }
    @Override public int getEnchantmentValue() { return 0; }
}
