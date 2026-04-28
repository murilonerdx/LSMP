package br.com.murilo.liberthia.entity.projectile;

import br.com.murilo.liberthia.logic.BloodKin;
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

/** Veiling Orb — AoE blindness/darkness/slowness grenade. */
public class VeilingOrbEntity extends ThrowableItemProjectile {

    public VeilingOrbEntity(EntityType<? extends VeilingOrbEntity> type, Level level) {
        super(type, level);
    }

    public VeilingOrbEntity(Level level, LivingEntity shooter) {
        super(ModEntities.VEILING_ORB.get(), shooter, level);
    }

    @Override
    protected Item getDefaultItem() { return ModItems.VEILING_ORB.get(); }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (level().isClientSide) {
            for (int i = 0; i < 16; i++) {
                level().addParticle(ParticleTypes.SQUID_INK, getX(), getY(), getZ(), 0, 0, 0);
            }
            return;
        }

        BlockPos impact = BlockPos.containing(result.getLocation());
        ServerLevel sl = (ServerLevel) level();
        int radius = 4;

        // Dome of darkness particles
        for (int a = 0; a < 60; a++) {
            double t = a / 60.0 * Math.PI * 2;
            for (double r = 0.5; r <= radius; r += 1.0) {
                double x = impact.getX() + 0.5 + Math.cos(t) * r;
                double z = impact.getZ() + 0.5 + Math.sin(t) * r;
                sl.sendParticles(ParticleTypes.SQUID_INK, x, impact.getY() + 0.5, z, 1, 0.05, 0.05, 0.05, 0.0);
                if ((int) r % 2 == 0) {
                    sl.sendParticles(ParticleTypes.LARGE_SMOKE, x, impact.getY() + 0.5, z, 1, 0.1, 0.4, 0.1, 0.01);
                }
            }
        }
        sl.sendParticles(ParticleTypes.SOUL, impact.getX() + 0.5, impact.getY() + 1.0, impact.getZ() + 0.5,
                40, 0.6, 0.4, 0.6, 0.05);
        sl.playSound(null, impact, SoundEvents.WITHER_AMBIENT, SoundSource.NEUTRAL, 0.7F, 1.6F);

        AABB box = new AABB(impact).inflate(radius);
        for (LivingEntity le : sl.getEntitiesOfClass(LivingEntity.class, box)) {
            if (BloodKin.is(le)) continue;
            if (le == getOwner()) continue;
            le.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 200, 0));
            le.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 140, 0));
            le.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, 2));
            le.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 160, 0));
        }

        discard();
    }
}
