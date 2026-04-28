package br.com.murilo.liberthia.logic;

import br.com.murilo.liberthia.registry.ModEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Occultism-{@code SpiritFireBlock}-inspired blood fire.
 *
 * <p>Visual flame block that:
 * <ul>
 *   <li>Doesn't extinguish from rain or block updates (returns false from
 *       {@code canSurvive} only when its support is gone).
 *   <li>{@link #entityInside} sets non-{@link BloodKin} entities on fire and
 *       applies Blood Infection.
 *   <li>Walkable for kin (no collision).
 * </ul>
 */
public class BloodFireBlock extends Block {

    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 16, 16);

    public BloodFireBlock(Properties props) {
        super(props);
    }

    @Override
    public VoxelShape getShape(BlockState s, BlockGetter g, BlockPos p, CollisionContext c) {
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState s, BlockGetter g, BlockPos p, CollisionContext c) {
        return Shapes.empty();
    }

    @Override
    public boolean propagatesSkylightDown(BlockState s, BlockGetter g, BlockPos p) {
        return true;
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader world, BlockPos pos) {
        BlockPos below = pos.below();
        return !world.getBlockState(below).isAir();
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level.isClientSide) return;
        if (!(entity instanceof LivingEntity le)) return;
        if (BloodKin.is(le)) return;
        // Set on fire.
        if (!entity.fireImmune()) {
            entity.setSecondsOnFire(4);
        }
        // Apply BloodInfection on a tick interval to avoid spam.
        if (le.tickCount % 30 == 0 && ModEffects.BLOOD_INFECTION.get() != null) {
            le.addEffect(new MobEffectInstance(ModEffects.BLOOD_INFECTION.get(), 80, 0,
                    false, true, true));
            le.hurt(le.damageSources().inFire(), 1.0F);
        }
    }

    @Override
    public boolean isRandomlyTicking(BlockState s) { return true; }

    @Override
    public void randomTick(BlockState state, net.minecraft.server.level.ServerLevel level,
                           BlockPos pos, RandomSource rand) {
        // Self-extinguish only if support gone.
        if (!canSurvive(state, level, pos)) {
            level.removeBlock(pos, false);
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource rand) {
        // Red flame particles + small smoke
        for (int i = 0; i < 3; i++) {
            level.addParticle(net.minecraft.core.particles.ParticleTypes.FLAME,
                    pos.getX() + rand.nextDouble(),
                    pos.getY() + rand.nextDouble() * 0.6,
                    pos.getZ() + rand.nextDouble(),
                    (rand.nextDouble() - 0.5) * 0.02,
                    rand.nextDouble() * 0.06,
                    (rand.nextDouble() - 0.5) * 0.02);
        }
        if (rand.nextInt(8) == 0) {
            level.addParticle(net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE,
                    pos.getX() + 0.5 + (rand.nextDouble() - 0.5) * 0.4,
                    pos.getY() + 0.5,
                    pos.getZ() + 0.5 + (rand.nextDouble() - 0.5) * 0.4,
                    0, 0.04, 0);
        }
    }
}
