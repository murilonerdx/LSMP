package br.com.murilo.liberthia.entity;

import br.com.murilo.liberthia.logic.BloodKin;
import br.com.murilo.liberthia.registry.ModEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Possessed Skeleton — Occultism-style possessed vanilla skeleton. Faster,
 * tougher; arrows it fires are themed (Wither tipping). Immune to Blood
 * Infection.
 */
public class PossessedSkeletonEntity extends Skeleton {

    public PossessedSkeletonEntity(EntityType<? extends Skeleton> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Skeleton.createAttributes()
                .add(Attributes.MAX_HEALTH, 22.0)
                .add(Attributes.ATTACK_DAMAGE, 3.0)
                .add(Attributes.MOVEMENT_SPEED, 0.30)
                .add(Attributes.FOLLOW_RANGE, 36.0);
    }

    @Override
    public boolean canBeAffected(MobEffectInstance effect) {
        if (effect.getEffect() == ModEffects.BLOOD_INFECTION.get()) return false;
        return super.canBeAffected(effect);
    }

    @Override
    protected AbstractArrow getArrow(ItemStack ammo, float velocity) {
        AbstractArrow arrow = super.getArrow(ammo, velocity);
        if (arrow instanceof net.minecraft.world.entity.projectile.Arrow regular) {
            regular.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.WITHER, 80, 0));
            if (ModEffects.BLOOD_INFECTION.get() != null) {
                regular.addEffect(new MobEffectInstance(ModEffects.BLOOD_INFECTION.get(), 100, 0));
            }
        }
        return arrow;
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof LivingEntity le && !BloodKin.is(le)
                && ModEffects.BLOOD_INFECTION.get() != null) {
            le.addEffect(new MobEffectInstance(ModEffects.BLOOD_INFECTION.get(), 20 * 8, 0,
                    false, true, true));
        }
        return hit;
    }
}
