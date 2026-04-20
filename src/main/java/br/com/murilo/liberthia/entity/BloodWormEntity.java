package br.com.murilo.liberthia.entity;

import br.com.murilo.liberthia.logic.BloodParticles;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.level.Level;

/**
 * Small fast blood worm. Extends Silverfish to reuse its vanilla AI/model/renderer.
 * Overrides attacks to apply bleed + red particle effects.
 */
public class BloodWormEntity extends Silverfish {

    private int lifespanTicks = 20 * 60; // 60s

    public BloodWormEntity(EntityType<? extends Silverfish> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Silverfish.createAttributes()
                .add(Attributes.MAX_HEALTH, 6.0D)
                .add(Attributes.ATTACK_DAMAGE, 3.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.32D)
                .add(Attributes.FOLLOW_RANGE, 20.0D);
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide) {
            if (--lifespanTicks <= 0) {
                if (this.level() instanceof ServerLevel sl) {
                    sl.sendParticles(BloodParticles.BLOOD, getX(), getY() + 0.2, getZ(),
                            16, 0.3, 0.2, 0.3, 0.15);
                }
                this.discard();
            }
        } else if (this.random.nextFloat() < 0.3F) {
            this.level().addParticle(BloodParticles.BLOOD,
                    getX() + (random.nextDouble() - 0.5) * 0.3,
                    getY() + 0.1,
                    getZ() + (random.nextDouble() - 0.5) * 0.3,
                    0, 0.02, 0);
        }
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof LivingEntity living) {
            living.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.WITHER, 40, 0, false, false, true));
            if (this.level() instanceof ServerLevel sl) {
                sl.sendParticles(BloodParticles.BLOOD, living.getX(), living.getY() + 1.0, living.getZ(),
                        10, 0.3, 0.3, 0.3, 0.1);
            }
        }
        return hit;
    }

    @Override
    public boolean hurt(DamageSource src, float amount) {
        boolean r = super.hurt(src, amount);
        if (r && this.level() instanceof ServerLevel sl) {
            sl.sendParticles(BloodParticles.BLOOD, getX(), getY() + 0.3, getZ(),
                    6, 0.2, 0.2, 0.2, 0.1);
        }
        return r;
    }
}
