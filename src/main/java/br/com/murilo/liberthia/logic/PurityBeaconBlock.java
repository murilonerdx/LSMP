package br.com.murilo.liberthia.logic;

import br.com.murilo.liberthia.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class PurityBeaconBlock extends Block {

    public PurityBeaconBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        level.scheduleTick(pos, this, 100 + level.getRandom().nextInt(100)); // 5 to 10 seconds
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        super.tick(state, level, pos, random);

        purifySurroundings(level, pos, random);

        level.scheduleTick(pos, this, 100 + random.nextInt(100)); // Next pulse in 5 to 10 seconds
    }

    private void purifySurroundings(ServerLevel level, BlockPos center, RandomSource random) {
        int radius = 16;
        int maxPurgesPerPulse = 5;
        int purges = 0;

        for (int i = 0; i < 50; i++) { // Random sampling for performance
            BlockPos scanPos = center.offset(
                    random.nextInt(radius * 2) - radius,
                    random.nextInt(radius * 2) - radius,
                    random.nextInt(radius * 2) - radius
            );

            if (scanPos.distSqr(center) <= radius * radius) {
                BlockState scanState = level.getBlockState(scanPos);
                
                if (scanState.is(ModBlocks.DARK_MATTER_BLOCK.get()) || scanState.is(ModBlocks.INFECTION_GROWTH.get())) {
                    level.setBlockAndUpdate(scanPos, Blocks.STONE.defaultBlockState());
                    purges++;
                } else if (scanState.is(ModBlocks.CORRUPTED_SOIL.get())) {
                    level.setBlockAndUpdate(scanPos, Blocks.GRASS_BLOCK.defaultBlockState());
                    purges++;
                }

                if (purges >= maxPurgesPerPulse) break; // Don't purge the whole chunk at once, do it gradually
            }
        }
    }
}
