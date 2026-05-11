package br.com.murilo.liberthia.logic;

import br.com.murilo.liberthia.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class CorruptedStoneBlock extends Block {

    public CorruptedStoneBlock(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isRandomlyTicking(BlockState state) { return false; /* DISABLED */ }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        /* DISABLED — kill switch permanente */
    }

    private void spreadToAdjacentStone(ServerLevel level, BlockPos pos, RandomSource random) {
        Direction direction = Direction.getRandom(random);
        BlockPos targetPos = pos.relative(direction);

        if (ProtectionUtils.isSpreadBlockedByProtectiveBlocks(level, targetPos)) {
            return;
        }

        if (ProtectionUtils.isWaterBarrier(level, targetPos)) {
            return;
        }

        BlockState targetState = level.getBlockState(targetPos);

        if (isConvertibleStone(targetState)) {
            level.setBlockAndUpdate(targetPos, ModBlocks.CORRUPTED_STONE.get().defaultBlockState());
        }
    }

    private static boolean isConvertibleStone(BlockState state) {
        return state.is(Blocks.STONE)
                || state.is(Blocks.COBBLESTONE)
                || state.is(Blocks.DEEPSLATE)
                || state.is(Blocks.COBBLED_DEEPSLATE)
                || state.is(Blocks.ANDESITE)
                || state.is(Blocks.DIORITE)
                || state.is(Blocks.GRANITE)
                || state.is(Blocks.TUFF);
    }
}
