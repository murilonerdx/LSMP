package br.com.murilo.liberthia.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Feather Fall — soft slow-falling buff. Negates fall damage and clamps
 * vertical descent velocity to a gentle drift. Lighter than vanilla Slow
 * Falling (which fully zeroes gravity); this lets you fall but never fast
 * enough to take damage.
 */
public class FeatherFallEffect extends MobEffect {
    public FeatherFallEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xddd9b8);
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
//        if (entity.isOnGround()) return;
        Vec3 m = entity.getDeltaMovement();
        if (m.y < -0.18) {
            entity.setDeltaMovement(m.x, -0.18, m.z);
            entity.fallDistance = 0F;
            entity.hasImpulse = true;
        }
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) { return true; }
}
