package br.com.murilo.liberthia.item;

import br.com.murilo.liberthia.registry.ModCapabilities;
import br.com.murilo.liberthia.registry.ModEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;

public class DarkMatterSwordItem extends SwordItem {
    public DarkMatterSwordItem(Properties properties) {
        super(DarkMatterToolMaterial.INSTANCE, 3, -2.4F, properties);
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        boolean result = super.hurtEnemy(stack, target, attacker);

        if (result) {
            // Lifesteal: heal attacker for 1 HP
            attacker.heal(1.0F);

            // Apply Dark Infection to target
            target.addEffect(new MobEffectInstance(ModEffects.DARK_INFECTION.get(), 200, 0));

            // Increase attacker's infection slightly
            if (attacker instanceof Player player) {
                player.getCapability(ModCapabilities.INFECTION).ifPresent(data -> {
                    data.setInfection(data.getInfection() + 1);
                    data.setDirty(true);
                });
            }
        }

        return result;
    }
}
