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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;

/**
 * Frost Flask — freezes water around impact, applies Slowness V + Mining Fatigue III.
 */
public class FrostFlaskEntity extends ThrowableItemProjectile {

    public FrostFlaskEntity(EntityType<? extends FrostFlaskEntity> type, Level level) {
        super(type, level);
    }

    public FrostFlaskEntity(Level level, LivingEntity shooter) {
        super(ModEntities.FROST_FLASK.get(), shooter, level);
    }

    @Override
    protected Item getDefaultItem() { return ModItems.FROST_FLASK.get(); }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (level().isClientSide) return;
        ServerLevel sl = (ServerLevel) level();
        BlockPos impact = blockPosition();

        // Freeze water in 3x3x2 below impact
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                for (int dy = -1; dy <= 0; dy++) {
                    BlockPos p = impact.offset(dx, dy, dz);
                    if (sl.getFluidState(p).getType().isSame(Fluids.WATER)) {
                        sl.setBlockAndUpdate(p, Blocks.FROSTED_ICE.defaultBlockState());
                    }
                }
            }
        }

        for (LivingEntity le : sl.getEntitiesOfClass(LivingEntity.class, new AABB(impact).inflate(4.0))) {
            if (BloodKin.is(le)) continue;
            if (le == getOwner()) continue;
            le.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, 4));
            le.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 200, 2));
            le.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 0));
            // Frostburn: fire ticks set negative don't work; instead deal cold damage
            le.hurt(le.damageSources().freeze(), 2.0F);
            le.setTicksFrozen(Math.min(300, le.getTicksFrozen() + 200));
        }

        sl.sendParticles(ParticleTypes.SNOWFLAKE, getX(), getY() + 0.6, getZ(), 40, 1.0, 0.8, 1.0, 0.05);
        sl.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 0.4, getZ(), 16, 0.8, 0.4, 0.8, 0.02);
        sl.playSound(null, impact, SoundEvents.GLASS_BREAK, SoundSource.NEUTRAL, 0.9F, 1.4F);
        discard();
    }
}
