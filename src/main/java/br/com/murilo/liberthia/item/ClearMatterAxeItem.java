package br.com.murilo.liberthia.item;

import br.com.murilo.liberthia.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class ClearMatterAxeItem extends AxeItem {
    public ClearMatterAxeItem(Properties properties) {
        super(ClearMatterToolMaterial.INSTANCE, 6.0F, -3.1F, properties);
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity miner) {
        boolean result = super.mineBlock(stack, level, state, pos, miner);

        if (!level.isClientSide && result && state.is(BlockTags.LOGS)) {
            // Check 3-block radius for corrupted_log and restore them to oak_log
            for (BlockPos neighbor : BlockPos.betweenClosed(pos.offset(-3, -3, -3), pos.offset(3, 3, 3))) {
                BlockState neighborState = level.getBlockState(neighbor);
                if (neighborState.is(ModBlocks.CORRUPTED_SOIL.get())) {
                    level.setBlock(neighbor.immutable(), Blocks.OAK_LOG.defaultBlockState(), 3);
                }
            }
        }

        return result;
    }
}
