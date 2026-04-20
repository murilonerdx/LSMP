package br.com.murilo.liberthia.entity;

import br.com.murilo.liberthia.logic.BloodParticles;
import br.com.murilo.liberthia.registry.ModEffects;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.level.Level;

/** Flesh Crawler — small worm that applies Blood Infection on hit. */
public class FleshCrawlerEntity extends Silverfish {
    public FleshCrawlerEntity(EntityType<? extends Silverfish> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Silverfish.createAttributes()
                .add(Attributes.MAX_HEALTH, 10.0D)
                .add(Attributes.ATTACK_DAMAGE, 3.5D)
                .add(Attributes.MOVEMENT_SPEED, 0.35D)
                .add(Attributes.FOLLOW_RANGE, 24.0D);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof LivingEntity le) {
            le.addEffect(new MobEffectInstance(ModEffects.BLOOD_INFECTION.get(), 200, 0, false, true, true));
            if (this.level() instanceof ServerLevel sl) {
                sl.sendParticles(BloodParticles.BLOOD, le.getX(), le.getY() + 1.0, le.getZ(),
                        12, 0.3, 0.3, 0.3, 0.12);
            }
        }
        return hit;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide && this.random.nextFloat() < 0.25F) {
            this.level().addParticle(BloodParticles.BLOOD,
                    getX() + (random.nextDouble() - 0.5) * 0.4, getY() + 0.15,
                    getZ() + (random.nextDouble() - 0.5) * 0.4, 0, 0.02, 0);
        }
    }
}
