package br.com.murilo.liberthia.effect;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * Blood Infection — indefinite max-HP drain. Persists across relogs via NBT.
 * Only cured by Blood Cure Pill.
 *
 * Uses a custom NBT counter to reduce max HP over time without needing
 * an AttributeModifier on every re-apply (which would conflict).
 */
public class BloodInfectionEffect extends MobEffect {

    public static final String NBT_DRAIN = "liberthia_blood_drain";

    public BloodInfectionEffect() {
        super(MobEffectCategory.HARMFUL, 0x8B0000);
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide) return;

        // Every 6 seconds (120 ticks) increase drain counter
        if (entity.tickCount % 120 == 0) {
            CompoundTag data = entity.getPersistentData();
            double cur = data.getDouble(NBT_DRAIN);
            // Amplifier 0 = -0.5 HP per 6s, amp 1 = -1 HP, amp 2 = -1.5 HP
            double inc = 0.5D + 0.5D * amplifier;
            cur = Math.min(18.0D, cur + inc);
            data.putDouble(NBT_DRAIN, cur);
            // Apply scheduled drain via event listener (see BloodInfectionEvents)
            BloodInfectionApplier.apply(entity, cur);
        }

        // Periodic damage
        if (entity.tickCount % 40 == 0) {
            entity.hurt(entity.damageSources().magic(), 0.5F + amplifier * 0.5F);
        }
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return true;
    }
}
