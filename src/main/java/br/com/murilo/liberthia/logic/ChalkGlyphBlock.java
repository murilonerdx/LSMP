package br.com.murilo.liberthia.logic;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Occultism-inspired flat glyph drawn with chalk on the ground.
 *
 * <p>Carries an {@link IntegerProperty} {@code SIGN} (0..MAX_SIGN) so the same
 * block can render 14 distinct typefaces from a single block id — keeps the
 * registry clean. Walkable, no collision; survives only on a solid surface.
 *
 * <p>Used as anchor block for blood rituals (alongside {@code blood_stone}).
 */
public class ChalkGlyphBlock extends Block {

    public static final IntegerProperty SIGN = IntegerProperty.create("sign", 0, 13);
    public static final int MAX_SIGN = 13;

    private static final VoxelShape SHAPE = Block.box(1.5, 0, 1.5, 14.5, 0.04, 14.5);

    public ChalkGlyphBlock(Properties props) {
        super(props);
        this.registerDefaultState(this.stateDefinition.any().setValue(SIGN, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(SIGN);
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
    public boolean canSurvive(BlockState state, LevelReader world, BlockPos pos) {
        BlockPos below = pos.below();
        return world.getBlockState(below).isFaceSturdy(world, below, net.minecraft.core.Direction.UP);
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }
}
