package br.com.murilo.liberthia.effect;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * Hemo Sickness — slow persistent debuff applied by Blood Mage projectiles
 * and Blood Hound bites. Ticks 0.5 HP damage every 2s AND spawns visible
 * red-drip particles so players know they're infected. Lighter than
 * BLOOD_INFECTION (which drains max HP) — designed as a dot-style debuff.
 */
public class HemoSicknessEffect extends MobEffect {

    public HemoSicknessEffect() {
        super(MobEffectCategory.HARMFUL, 0x6b0a0a);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return duration % 40 == 0;
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (!entity.level().isClientSide) {
            float dmg = 0.5F + 0.5F * amplifier;
            entity.hurt(entity.damageSources().magic(), dmg);
            if (entity.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.DAMAGE_INDICATOR,
                        entity.getX(),
                        entity.getY() + entity.getBbHeight() * 0.5,
                        entity.getZ(),
                        4, 0.2, 0.3, 0.2, 0.02);
            }
        }
    }
}
