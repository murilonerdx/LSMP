package br.com.murilo.liberthia.item;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Order Armor — holy protection set. Full set grants Regeneration I, Speed I,
 * Fire Resistance, and clears Blood Infection / Dark Infection periodically.
 */
public class OrderArmorItem extends ArmorItem {
    public OrderArmorItem(ArmorItem.Type type, Properties props) {
        super(OrderArmorMaterial.INSTANCE, type, props);
    }

    public static boolean hasFullSet(Player p) {
        for (int i = 0; i < 4; i++) {
            ItemStack s = p.getInventory().armor.get(i);
            if (!(s.getItem() instanceof OrderArmorItem)) return false;
        }
        return true;
    }

    public static void tickFullSet(Level level, Player p) {
        if (level.isClientSide) return;
        if (!hasFullSet(p)) return;
        if (p.tickCount % 40 == 0) {
            p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 80, 0, true, false, true));
            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 80, 0, true, false, true));
            p.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 80, 0, true, false, true));
        }
        if (p.tickCount % 200 == 0) {
            try {
                var blood = br.com.murilo.liberthia.registry.ModEffects.BLOOD_INFECTION.get();
                if (p.hasEffect(blood)) p.removeEffect(blood);
            } catch (Exception ignored) {}
            try {
                var dark = br.com.murilo.liberthia.registry.ModEffects.DARK_INFECTION.get();
                if (p.hasEffect(dark)) p.removeEffect(dark);
            } catch (Exception ignored) {}
        }
    }
}
