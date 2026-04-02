package br.com.murilo.liberthia.item;

import br.com.murilo.liberthia.registry.ModBlocks;
import br.com.murilo.liberthia.registry.ModCapabilities;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class DarkMatterAxeItem extends AxeItem {
    public DarkMatterAxeItem(Properties properties) {
        super(DarkMatterToolMaterial.INSTANCE, 5.0F, -3.0F, properties);
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity miner) {
        boolean result = super.mineBlock(stack, level, state, pos, miner);

        if (!level.isClientSide && result && state.is(BlockTags.LOGS)) {
            // Corrupt adjacent logs: 30% chance to convert each neighbor to corrupted soil
            for (BlockPos neighbor : BlockPos.betweenClosed(pos.offset(-1, -1, -1), pos.offset(1, 1, 1))) {
                if (neighbor.equals(pos)) continue;
                BlockState neighborState = level.getBlockState(neighbor);
                if (neighborState.is(BlockTags.LOGS) && level.getRandom().nextFloat() < 0.3F) {
                    level.setBlock(neighbor.immutable(), ModBlocks.CORRUPTED_SOIL.get().defaultBlockState(), 3);
                }
            }
        }

        // Infection cost
        if (miner instanceof Player player) {
            player.getCapability(ModCapabilities.INFECTION).ifPresent(data -> {
                if (level.getRandom().nextFloat() < 0.1F) {
                    data.setInfection(data.getInfection() + 1);
                    data.setDirty(true);
                }
            });
        }

        return result;
    }
}
