package br.com.murilo.liberthia.entity.projectile;

import br.com.murilo.liberthia.effect.BloodInfectionApplier;
import br.com.murilo.liberthia.logic.BloodKin;
import br.com.murilo.liberthia.registry.ModEffects;
import br.com.murilo.liberthia.registry.ModEntities;
import br.com.murilo.liberthia.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;

/**
 * Purifying Flask — throwable cleansing grenade.
 *
 *   • AoE 6 blocks at impact.
 *   • Removes Blood Infection (effect + drain) from every LivingEntity in range.
 *   • Damages BloodKin entities for 7 hp (silver-burn style).
 *   • Bestows Regeneration I (5s) to non-kin survivors.
 */
public class PurifyingFlaskEntity extends ThrowableItemProjectile {

    public PurifyingFlaskEntity(EntityType<? extends PurifyingFlaskEntity> type, Level level) {
        super(type, level);
    }

    public PurifyingFlaskEntity(Level level, LivingEntity shooter) {
        super(ModEntities.PURIFYING_FLASK.get(), shooter, level);
    }

    @Override
    protected Item getDefaultItem() { return ModItems.PURIFYING_FLASK.get(); }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (level().isClientSide) return;
        ServerLevel sl = (ServerLevel) level();
        BlockPos impact = blockPosition();
        double radius = 6.0;

        for (LivingEntity le : sl.getEntitiesOfClass(LivingEntity.class,
                new AABB(impact).inflate(radius))) {
            // Cleanse infection from every entity (kin or not — but kin also takes damage).
            if (ModEffects.BLOOD_INFECTION.get() != null
                    && le.hasEffect(ModEffects.BLOOD_INFECTION.get())) {
                le.removeEffect(ModEffects.BLOOD_INFECTION.get());
            }
            BloodInfectionApplier.clear(le);

            if (BloodKin.is(le)) {
                le.hurt(le.damageSources().magic(), 7.0F);
            } else {
                le.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 0,
                        false, true, true));
            }
        }

        sl.sendParticles(ParticleTypes.GLOW,
                getX(), getY() + 0.6, getZ(), 60, radius * 0.5, 0.6, radius * 0.5, 0.04);
        sl.sendParticles(ParticleTypes.WHITE_ASH,
                getX(), getY() + 0.4, getZ(), 80, radius * 0.6, 0.4, radius * 0.6, 0.02);
        sl.sendParticles(ParticleTypes.HEART,
                getX(), getY() + 1.0, getZ(), 6, 1.5, 0.4, 1.5, 0.02);
        sl.playSound(null, impact, SoundEvents.GLASS_BREAK,
                SoundSource.NEUTRAL, 0.9F, 1.5F);
        sl.playSound(null, impact, SoundEvents.PLAYER_LEVELUP,
                SoundSource.NEUTRAL, 0.4F, 1.8F);
        discard();
    }
}
