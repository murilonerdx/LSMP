package br.com.murilo.liberthia.logic;

import br.com.murilo.liberthia.registry.ModEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Blood Spike — emits red particles constantly and damages any LivingEntity
 * that walks through. Applies Blood Infection on contact.
 */
public class BloodSpikeBlock extends Block {

    private static final VoxelShape SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 5.0D, 16.0D);

    public BloodSpikeBlock(Properties props) {
        super(props);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource rng) {
        // Red particle mist rising from the spikes
        for (int i = 0; i < 3; i++) {
            double x = pos.getX() + rng.nextDouble();
            double y = pos.getY() + 0.35 + rng.nextDouble() * 0.3;
            double z = pos.getZ() + rng.nextDouble();
            level.addParticle(BloodParticles.BLOOD, x, y, z,
                    (rng.nextDouble() - 0.5) * 0.08, 0.12 + rng.nextDouble() * 0.12,
                    (rng.nextDouble() - 0.5) * 0.08);
        }
        if (rng.nextFloat() < 0.3F) {
            level.addParticle(ParticleTypes.LAVA,
                    pos.getX() + 0.3 + rng.nextDouble() * 0.4,
                    pos.getY() + 0.3,
                    pos.getZ() + 0.3 + rng.nextDouble() * 0.4,
                    0, 0.05, 0);
        }
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level.isClientSide) return;
        if (BloodKin.is(entity)) return;
        if (!(entity instanceof LivingEntity living)) return;
        // Damage roughly once per second per entity
        if (entity.tickCount % 20 == 0) {
            living.hurt(level.damageSources().magic(), 1.5F);
            living.addEffect(new MobEffectInstance(
                    ModEffects.BLOOD_INFECTION.get(), 120, 0, false, true, true));
        }
    }
}
