package br.com.murilo.liberthia.item;

import br.com.murilo.liberthia.registry.ModEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class DarkMatterShardItem extends Item {
    public DarkMatterShardItem(Properties properties) {
        super(properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (level.isClientSide || !(entity instanceof Player player)) {
            return;
        }

        if (entity.tickCount % 200 == 0) {
            int shardCount = 0;
            for (ItemStack inv : player.getInventory().items) {
                if (inv.getItem() instanceof DarkMatterShardItem) {
                    shardCount += inv.getCount();
                }
            }

            if (shardCount >= 8) {
                player.addEffect(new MobEffectInstance(ModEffects.RADIATION_SICKNESS.get(), 400, 1, true, false, true));
            } else if (shardCount >= 3) {
                player.addEffect(new MobEffectInstance(ModEffects.RADIATION_SICKNESS.get(), 200, 0, true, false, true));
            }
        }
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
