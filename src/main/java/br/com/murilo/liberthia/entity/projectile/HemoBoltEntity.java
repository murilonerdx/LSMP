package br.com.murilo.liberthia.entity.projectile;

import br.com.murilo.liberthia.registry.ModEffects;
import br.com.murilo.liberthia.registry.ModEntities;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Blood bolt fired by Blood Priests (Fase 1). Reused as the projectile for
 * HemomancerStaffItem in Fase 4. Applies BloodInfection on hit.
 */
public class HemoBoltEntity extends AbstractHurtingProjectile {

    public HemoBoltEntity(EntityType<? extends HemoBoltEntity> type, Level level) {
        super(type, level);
    }

    public HemoBoltEntity(Level level, LivingEntity shooter, double dx, double dy, double dz) {
        super(ModEntities.HEMO_BOLT.get(), shooter, dx, dy, dz, level);
    }

    @Override
    protected ParticleOptions getTrailParticle() {
        return ParticleTypes.DAMAGE_INDICATOR;
    }

    @Override
    public boolean isOnFire() { return false; }

    @Override
    protected boolean shouldBurn() { return false; }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (level().isClientSide) return;
        if (result.getEntity() instanceof LivingEntity target) {
            float dmg = 5.0F;
            target.hurt(this.damageSources().indirectMagic(this, this.getOwner()), dmg);
            if (ModEffects.BLOOD_INFECTION != null && ModEffects.BLOOD_INFECTION.get() != null) {
                target.addEffect(new MobEffectInstance(ModEffects.BLOOD_INFECTION.get(), 160, 0));
            }
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!level().isClientSide) {
            ((net.minecraft.server.level.ServerLevel) level()).sendParticles(
                    ParticleTypes.DAMAGE_INDICATOR,
                    getX(), getY(), getZ(),
                    12, 0.3, 0.3, 0.3, 0.1);
            this.discard();
        }
    }

    @Override
    public boolean isPickable() { return false; }
}
