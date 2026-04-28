package br.com.murilo.liberthia.entity;

import br.com.murilo.liberthia.logic.BloodKin;
import br.com.murilo.liberthia.registry.ModEffects;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.Level;

/**
 * Possessed Zombie — Occultism-style possessed vanilla mob: reuses the vanilla
 * model + AI but is faction-aligned with blood kin (immune to BloodInfection,
 * applies it on hit) and is a bit faster + tougher than a normal zombie.
 */
public class PossessedZombieEntity extends Zombie {

    public PossessedZombieEntity(EntityType<? extends Zombie> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Zombie.createAttributes()
                .add(Attributes.MAX_HEALTH, 28.0)
                .add(Attributes.ATTACK_DAMAGE, 4.5)
                .add(Attributes.MOVEMENT_SPEED, 0.27)
                .add(Attributes.ARMOR, 4.0)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    public boolean canBeAffected(MobEffectInstance effect) {
        if (effect.getEffect() == ModEffects.BLOOD_INFECTION.get()) return false;
        return super.canBeAffected(effect);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof LivingEntity le && !BloodKin.is(le)
                && ModEffects.BLOOD_INFECTION.get() != null) {
            le.addEffect(new MobEffectInstance(ModEffects.BLOOD_INFECTION.get(),
                    20 * 12, 0, false, true, true));
        }
        return hit;
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(source, looting, recentlyHit);
        if (this.random.nextFloat() < 0.4F
                && br.com.murilo.liberthia.registry.ModItems.CONGEALED_BLOOD.get() != null) {
            spawnAtLocation(new net.minecraft.world.item.ItemStack(
                    br.com.murilo.liberthia.registry.ModItems.CONGEALED_BLOOD.get()));
        }
    }
}
