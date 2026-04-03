package br.com.murilo.liberthia.item;

import br.com.murilo.liberthia.registry.ModCapabilities;
import br.com.murilo.liberthia.registry.ModEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;

public class ClearMatterSwordItem extends SwordItem {
    public ClearMatterSwordItem(Properties properties) {
        super(ClearMatterToolMaterial.INSTANCE, 3, -2.4F, properties);
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        boolean result = super.hurtEnemy(stack, target, attacker);

        if (result) {
            // Apply CLEAR_SHIELD effect to the attacker for 60 ticks
            attacker.addEffect(new MobEffectInstance(ModEffects.CLEAR_SHIELD.get(), 60, 0));

            // Reduce target's infection by 5 if they have the capability
            if (target instanceof Player targetPlayer) {
                targetPlayer.getCapability(ModCapabilities.INFECTION).ifPresent(data -> {
                    data.reduceInfection(5);
                    data.setDirty(true);
                });
            }
        }

        return result;
    }
}
