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
