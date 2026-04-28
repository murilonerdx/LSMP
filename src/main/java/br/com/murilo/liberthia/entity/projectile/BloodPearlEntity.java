package br.com.murilo.liberthia.entity.projectile;

import br.com.murilo.liberthia.registry.ModEffects;
import br.com.murilo.liberthia.registry.ModEntities;
import br.com.murilo.liberthia.registry.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.event.entity.EntityTeleportEvent;
import net.minecraftforge.common.MinecraftForge;

/**
 * Ported from EvilCraft's {@code EntityBloodPearl}. Thrown projectile that
 * teleports the owner to the impact location, applying Slowness III + a light
 * Blood Infection tick on arrival.
 */
public class BloodPearlEntity extends ThrowableItemProjectile {

    public BloodPearlEntity(EntityType<? extends BloodPearlEntity> type, Level level) {
        super(type, level);
    }

    public BloodPearlEntity(Level level, LivingEntity thrower) {
        super(ModEntities.BLOOD_PEARL.get(), thrower, level);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.BLOOD_TELEPORT_PEARL.get();
    }

    @Override
    protected void onHit(HitResult result) {
        if (result.getType() == HitResult.Type.ENTITY) {
            ((EntityHitResult) result).getEntity()
                    .hurt(level().damageSources().thrown(this, getOwner()), 0.0F);
        }

        if (level().isClientSide) {
            for (int i = 0; i < 24; i++) {
                level().addParticle(ParticleTypes.PORTAL,
                        getX(), getY() + random.nextDouble() * 1.6D, getZ(),
                        random.nextGaussian(), 0.0D, random.nextGaussian());
            }
            return;
        }

        if (getOwner() instanceof ServerPlayer sp && sp.connection != null
                && sp.level() == level()) {
            EntityTeleportEvent.EnderPearl evt = new EntityTeleportEvent.EnderPearl(
                    sp, getX(), getY(), getZ(), null, 0F, result);
            if (!MinecraftForge.EVENT_BUS.post(evt)) {
                if (sp.isPassenger()) sp.stopRiding();
                sp.teleportTo(evt.getTargetX(), evt.getTargetY(), evt.getTargetZ());
                sp.fallDistance = 0.0F;
                sp.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 2));
                if (ModEffects.BLOOD_INFECTION.get() != null) {
                    sp.addEffect(new MobEffectInstance(ModEffects.BLOOD_INFECTION.get(), 120, 0));
                }
                ServerLevel sl = (ServerLevel) level();
                sl.sendParticles(ParticleTypes.PORTAL, getX(), getY() + 1, getZ(), 40, 0.4, 0.9, 0.4, 0.05);
                sl.sendParticles(ParticleTypes.DAMAGE_INDICATOR, getX(), getY() + 1, getZ(), 20, 0.3, 0.4, 0.3, 0.03);
                sl.playSound(null, sp.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.9F, 0.7F);
            }
        }

        discard();
    }
}
