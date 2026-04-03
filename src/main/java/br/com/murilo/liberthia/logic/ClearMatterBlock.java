package br.com.murilo.liberthia.logic;

import br.com.murilo.liberthia.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class ClearMatterBlock extends Block {
    public ClearMatterBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int restored = MatterHistoryManager.restoreMappedBlocks(level, pos, 16);
        if (restored > 0 && random.nextFloat() < 0.15F) {
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            return;
        }

        // Expanded purification aura: 9x5x9 area
        // Gradually reduces InfectionGrowth AGE instead of instant removal
        for (BlockPos area : BlockPos.betweenClosed(pos.offset(-4, -2, -4), pos.offset(4, 2, 4))) {
            BlockState areaState = level.getBlockState(area);
            if (areaState.is(ModBlocks.CORRUPTED_SOIL.get())) {
                level.setBlockAndUpdate(area, Blocks.DIRT.defaultBlockState());
            } else if (areaState.is(ModBlocks.INFECTION_GROWTH.get())) {
                int age = areaState.getValue(InfectionGrowthBlock.AGE);
                if (age > 0) {
                    level.setBlockAndUpdate(area, areaState.setValue(InfectionGrowthBlock.AGE, age - 1));
                } else {
                    level.setBlockAndUpdate(area, Blocks.AIR.defaultBlockState());
                }
            }
        }

        level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class, new net.minecraft.world.phys.AABB(pos).inflate(6.0D))
                .forEach(entity -> entity.getCapability(br.com.murilo.liberthia.registry.ModCapabilities.INFECTION).ifPresent(data -> {
                    data.reduceInfection(2);
                    data.setDirty(true);
                }));

        // A matéria clara atua como um sumidouro para a matéria escura
        for (Direction direction : Direction.values()) {
            BlockPos neighborPos = pos.relative(direction);
            BlockState neighborState = level.getBlockState(neighborPos);

            if (neighborState.is(ModBlocks.DARK_MATTER_BLOCK.get()) || neighborState.is(ModBlocks.DARK_MATTER_FLUID_BLOCK.get())) {
                // "Limpa" a matéria escura, transformando-a em ar (ou restaurando grama se for possível)
                level.setBlockAndUpdate(neighborPos, Blocks.AIR.defaultBlockState());
                
                // Representação da Matéria Clara ficando "preta" ou se degradando ao fazer a barreira
                if (random.nextFloat() < 0.05F) {
                    level.setBlockAndUpdate(pos, ModBlocks.DARK_MATTER_BLOCK.get().defaultBlockState());
                    break;
                }
            }
        }
    }
}
