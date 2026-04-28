package br.com.murilo.liberthia.item;

import br.com.murilo.liberthia.registry.ModEffects;
import br.com.murilo.liberthia.registry.ModItems;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.ArrayList;

/**
 * Ported from EvilCraft's {@code ItemInvigoratingPendant}. While in inventory,
 * every 0.5s it consumes 1 SANGUINE_ESSENCE to reduce one harmful effect's
 * duration by ~2s. Won't touch BLOOD_INFECTION (that's curable only by the
 * Cure Pill / Hemomancer ritual). Also extinguishes the player if on fire,
 * costing 1 essence.
 */
public class PurgingPendantItem extends Item {

    private static final int TICK_INTERVAL = 10; // 0.5s
    private static final int REDUCE_PER_USE = 40; // 2s

    public PurgingPendantItem(Properties props) {
        super(props);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level.isClientSide) return;
        if (!(entity instanceof Player player)) return;
        if (player.tickCount % TICK_INTERVAL != 0) return;

        if (player.isOnFire() && consumeEssence(player)) {
            player.clearFire();
        }

        // Find first reducible harmful effect (skip BLOOD_INFECTION + ambient).
        for (MobEffectInstance fx : new ArrayList<>(player.getActiveEffects())) {
            MobEffect type = fx.getEffect();
            if (type == null) continue;
            if (type.getCategory() != MobEffectCategory.HARMFUL) continue;
            if (fx.isAmbient()) continue;
            if (ModEffects.BLOOD_INFECTION.get() != null && type == ModEffects.BLOOD_INFECTION.get()) continue;

            if (!consumeEssence(player)) return;
            int dur = fx.getDuration();
            int newDur = Math.max(0, dur - REDUCE_PER_USE * Math.max(1, fx.getAmplifier() + 1));
            if (newDur <= 0) {
                player.removeEffect(type);
            } else {
                player.removeEffect(type);
                player.addEffect(new MobEffectInstance(type, newDur, fx.getAmplifier(), fx.isAmbient(), fx.isVisible(), fx.showIcon()));
            }
            return; // one effect per tick
        }
    }

    /** True if a Sanguine Essence was found and consumed. */
    private static boolean consumeEssence(Player player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (s.getItem() == ModItems.SANGUINE_ESSENCE.get()) {
                s.shrink(1);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isFoil(ItemStack stack) { return true; }
}
