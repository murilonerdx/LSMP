package br.com.murilo.liberthia.entity;

import br.com.murilo.liberthia.logic.BloodParticles;
import br.com.murilo.liberthia.registry.ModEffects;
import br.com.murilo.liberthia.registry.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Gore Worm — larger worm with heavy damage, Poison + Blood Infection II on hit. */
public class GoreWormEntity extends Silverfish {
    public GoreWormEntity(EntityType<? extends Silverfish> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Silverfish.createAttributes()
                .add(Attributes.MAX_HEALTH, 24.0D)
                .add(Attributes.ATTACK_DAMAGE, 6.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.28D)
                .add(Attributes.FOLLOW_RANGE, 32.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6D);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof LivingEntity le) {
            le.addEffect(new MobEffectInstance(ModEffects.BLOOD_INFECTION.get(), 400, 1, false, true, true));
            le.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 1, false, true, true));
            if (this.level() instanceof ServerLevel sl) {
                sl.sendParticles(BloodParticles.BLOOD, le.getX(), le.getY() + 1.0, le.getZ(),
                        20, 0.4, 0.4, 0.4, 0.18);
            }
        }
        return hit;
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(source, looting, recentlyHit);
        // Rare drop: Heart of Flesh (summoning ingredient for the Mother boss).
        if (random.nextFloat() < 0.08F) {
            spawnAtLocation(new ItemStack(ModItems.HEART_OF_FLESH_ITEM.get(), 1));
        }
    }
}
