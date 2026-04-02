package br.com.murilo.liberthia.logic;

import br.com.murilo.liberthia.registry.ModBlocks;
import br.com.murilo.liberthia.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.phys.Vec3;

import java.util.function.Supplier;

public class DarkMatterFluidBlock extends LiquidBlock {
    public DarkMatterFluidBlock(Supplier<? extends FlowingFluid> fluid, Properties properties) {
        super(fluid, properties);
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // Fluid infection spread - mimicking solid dark matter behavior
        float density = InfectionLogic.getChunkInfectionDensity(level, pos);
        int attempts = 2 + (int)(density * 8);

        for (int i = 0; i < attempts; i++) {
            spreadInfection(level, pos, random);
        }

        // Fluids also launch spores but at slightly lower frequency
        if (random.nextFloat() < (0.15f + density * 0.2f)) {
            attemptSporeLaunch(level, pos, random);
        }
    }

    private void spreadInfection(ServerLevel level, BlockPos pos, RandomSource random) {
        Direction dir = Direction.getRandom(random);
        BlockPos neighborPos = pos.relative(dir);
        BlockState neighborState = level.getBlockState(neighborPos);

        if (neighborState.is(Blocks.GRASS_BLOCK) || neighborState.is(Blocks.DIRT)) {
            level.setBlockAndUpdate(neighborPos, ModBlocks.CORRUPTED_SOIL.get().defaultBlockState());
        } else if (neighborState.is(Blocks.STONE) || neighborState.is(Blocks.SAND) || neighborState.is(Blocks.GRAVEL)) {
            level.setBlockAndUpdate(neighborPos, ModBlocks.DARK_MATTER_BLOCK.get().defaultBlockState());
        }
    }

    private void attemptSporeLaunch(ServerLevel level, BlockPos pos, RandomSource random) {
        // Sample surrounding density
        int localFoci = 0;
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-2, -2, -2), pos.offset(2, 2, 2))) {
            if (level.getFluidState(p).getType().isSame(getFluid())) localFoci++;
        }

        if (localFoci >= 5) {
            br.com.murilo.liberthia.entity.DarkMatterSporeEntity spore = ModEntities.DARK_MATTER_SPORE.get().create(level);
            if (spore != null) {
                spore.setPos(pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5);
                double dist = 24.0 + random.nextDouble() * 24.0;
                double angle = random.nextDouble() * Math.PI * 2;
                spore.setTarget(new Vec3(pos.getX() + Math.cos(angle) * dist, pos.getY() + 15.0, pos.getZ() + Math.sin(angle) * dist));
                level.addFreshEntity(spore);
            }
        }
    }
}
