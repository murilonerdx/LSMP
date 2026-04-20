package br.com.murilo.liberthia.item;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Blood Armor — strong tanky set. Full-set wearer is immune to Wither and
 * receives small lifesteal on damage dealt (handled elsewhere via events)
 * plus Resistance I and Strength I while complete.
 */
public class BloodArmorItem extends ArmorItem {
    public BloodArmorItem(ArmorItem.Type type, Properties props) {
        super(BloodArmorMaterial.INSTANCE, type, props);
    }

    public static boolean hasFullSet(Player p) {
        for (int i = 0; i < 4; i++) {
            ItemStack s = p.getInventory().armor.get(i);
            if (!(s.getItem() instanceof BloodArmorItem)) return false;
        }
        return true;
    }

    public static void tickFullSet(Level level, Player p) {
        if (level.isClientSide) return;
        if (!hasFullSet(p)) return;
        if (p.tickCount % 40 == 0) {
            p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 0, true, false, true));
            p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 60, 0, true, false, true));
            // cleanse wither
            if (p.hasEffect(MobEffects.WITHER)) p.removeEffect(MobEffects.WITHER);
        }
    }
}
