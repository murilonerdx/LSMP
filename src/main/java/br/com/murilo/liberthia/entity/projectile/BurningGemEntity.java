package br.com.murilo.liberthia.entity.projectile;

import br.com.murilo.liberthia.logic.BloodKin;
import br.com.murilo.liberthia.registry.ModEntities;
import br.com.murilo.liberthia.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;

/** Burning Gem — fire bomb. Sets fire 3x3 + applies Fire 8s + damage. */
public class BurningGemEntity extends ThrowableItemProjectile {

    public BurningGemEntity(EntityType<? extends BurningGemEntity> type, Level level) {
        super(type, level);
    }

    public BurningGemEntity(Level level, LivingEntity shooter) {
        super(ModEntities.BURNING_GEM.get(), shooter, level);
    }

    @Override
    protected Item getDefaultItem() { return ModItems.BURNING_GEM.get(); }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (level().isClientSide) return;
        ServerLevel sl = (ServerLevel) level();
        BlockPos impact = blockPosition();

        // Fire ring 3x3
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos p = impact.offset(dx, 0, dz);
                if (sl.getBlockState(p).isAir() && sl.getBlockState(p.below()).isFaceSturdy(sl, p.below(), net.minecraft.core.Direction.UP)) {
                    sl.setBlockAndUpdate(p, BaseFireBlock.getState(sl, p));
                } else if (sl.getBlockState(p.above()).isAir()
                        && sl.getBlockState(p).isFaceSturdy(sl, p, net.minecraft.core.Direction.UP)) {
                    sl.setBlockAndUpdate(p.above(), Blocks.FIRE.defaultBlockState());
                }
            }
        }

        for (LivingEntity le : sl.getEntitiesOfClass(LivingEntity.class, new AABB(impact).inflate(3.5))) {
            if (BloodKin.is(le)) continue;
            if (le == getOwner()) continue;
            le.setSecondsOnFire(10);
            le.hurt(le.damageSources().inFire(), 4.0F);
        }

        sl.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 0.4, getZ(), 18, 0.6, 0.4, 0.6, 0.05);
        sl.sendParticles(ParticleTypes.LAVA, getX(), getY() + 0.5, getZ(), 8, 0.4, 0.3, 0.4, 0.15);
        sl.sendParticles(ParticleTypes.FLAME, getX(), getY() + 0.5, getZ(), 24, 0.5, 0.5, 0.5, 0.1);
        sl.playSound(null, impact, SoundEvents.GENERIC_EXPLODE, SoundSource.NEUTRAL, 0.7F, 1.4F);
        discard();
    }
}
