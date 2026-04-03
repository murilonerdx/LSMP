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
    public boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (ProtectionUtils.isSpreadBlockedByProtectiveBlocks(level, pos)) {
            return;
        }

        // 15% chance to spread to adjacent stone variants
        if (random.nextFloat() < 0.15f) {
            spreadToAdjacentStone(level, pos, random);
        }

        // 5% chance to emit SQUID_INK particles
        if (random.nextFloat() < 0.05f) {
            level.sendParticles(
                    ParticleTypes.SQUID_INK,
                    pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                    2,
                    0.3D, 0.3D, 0.3D,
                    0.02D
            );
        }
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
