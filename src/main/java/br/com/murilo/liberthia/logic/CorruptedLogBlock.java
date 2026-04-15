package br.com.murilo.liberthia.logic;

import br.com.murilo.liberthia.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;

public class CorruptedLogBlock extends Block {

    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.AXIS;

    public CorruptedLogBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(AXIS, Direction.Axis.Y));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AXIS);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(AXIS, context.getClickedFace().getAxis());
    }

    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (br.com.murilo.liberthia.config.DevMode.ACTIVE) return;
        if (ProtectionUtils.isSpreadBlockedByProtectiveBlocks(level, pos)) {
            return;
        }

        // 8% chance to spread infection to adjacent logs
        if (random.nextFloat() < 0.08f) {
            spreadToAdjacentLogs(level, pos, random);
        }

        // 4% chance to spread corrupted_soil to ground below
        if (random.nextFloat() < 0.04f) {
            spreadCorruptedSoilBelow(level, pos);
        }
    }

    private void spreadToAdjacentLogs(ServerLevel level, BlockPos pos, RandomSource random) {
        Direction direction = Direction.getRandom(random);
        BlockPos targetPos = pos.relative(direction);

        if (ProtectionUtils.isSpreadBlockedByProtectiveBlocks(level, targetPos)) {
            return;
        }

        if (ProtectionUtils.isWaterBarrier(level, targetPos)) {
            return;
        }

        BlockState targetState = level.getBlockState(targetPos);

        if (targetState.is(BlockTags.LOGS) && !targetState.is(this)) {
            // Preserve the axis direction of the original log
            Direction.Axis targetAxis = Direction.Axis.Y;
            if (targetState.hasProperty(BlockStateProperties.AXIS)) {
                targetAxis = targetState.getValue(BlockStateProperties.AXIS);
            }
            level.setBlockAndUpdate(targetPos,
                    ModBlocks.CORRUPTED_LOG.get().defaultBlockState().setValue(AXIS, targetAxis));
        }
    }

    private void spreadCorruptedSoilBelow(ServerLevel level, BlockPos pos) {
        // Find ground below the log
        BlockPos below = pos.below();
        for (int i = 0; i < 10; i++) {
            BlockState belowState = level.getBlockState(below);

            if (belowState.isAir()) {
                below = below.below();
                continue;
            }

            if (ProtectionUtils.isSpreadBlockedByProtectiveBlocks(level, below)) {
                return;
            }

            if (ProtectionUtils.isWaterBarrier(level, below)) {
                return;
            }

            // If it's a solid ground block that can be corrupted, convert it
            if (isCorruptibleGround(belowState)) {
                level.setBlockAndUpdate(below, ModBlocks.CORRUPTED_SOIL.get().defaultBlockState());
            }
            return;
        }
    }

    private static boolean isCorruptibleGround(BlockState state) {
        return state.is(BlockTags.DIRT)
                || state.is(BlockTags.SAND)
                || state.is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK)
                || state.is(net.minecraft.world.level.block.Blocks.PODZOL)
                || state.is(net.minecraft.world.level.block.Blocks.MYCELIUM)
                || state.is(net.minecraft.world.level.block.Blocks.MOSS_BLOCK)
                || state.is(net.minecraft.world.level.block.Blocks.MUD);
    }
}
