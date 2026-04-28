package br.com.murilo.liberthia.logic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Vector3f;

/**
 * Blood torch — uses the {@code minecraft:block/cross} model template (the
 * same sprite-on-two-planes that saplings/flowers use) so the texture can be
 * any 16×16 PNG without UV-mapping concerns. Renders as a tall vertical sprite
 * with a red-flame particle drift on top.
 *
 * <p>Floor-only: requires a sturdy face below.
 */
public class BloodTorchBlock extends Block {
    private static final VoxelShape SHAPE = Block.box(5, 0, 5, 11, 12, 11);
    private static final DustParticleOptions RED_DUST =
            new DustParticleOptions(new Vector3f(0.85F, 0.10F, 0.18F), 1.4F);

    public BloodTorchBlock(Properties props) {
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
    public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader world, BlockPos pos) {
        BlockPos below = pos.below();
        return world.getBlockState(below).isFaceSturdy(world, below, Direction.UP);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource rand) {
        double cx = pos.getX() + 0.5;
        double cy = pos.getY() + 0.85;
        double cz = pos.getZ() + 0.5;
        level.addParticle(RED_DUST,
                cx + (rand.nextDouble() - 0.5) * 0.1,
                cy, cz + (rand.nextDouble() - 0.5) * 0.1, 0, 0.02, 0);
        if (rand.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.SMOKE, cx, cy, cz, 0, 0.01, 0);
        }
        if (rand.nextInt(6) == 0) {
            level.addParticle(ParticleTypes.SOUL_FIRE_FLAME, cx, cy, cz,
                    (rand.nextDouble() - 0.5) * 0.02,
                    rand.nextDouble() * 0.04,
                    (rand.nextDouble() - 0.5) * 0.02);
        }
    }
}
