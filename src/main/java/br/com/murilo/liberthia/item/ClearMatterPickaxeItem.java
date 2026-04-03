package br.com.murilo.liberthia.item;

import br.com.murilo.liberthia.logic.MatterHistoryManager;
import br.com.murilo.liberthia.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class ClearMatterPickaxeItem extends PickaxeItem {
    public ClearMatterPickaxeItem(Properties properties) {
        super(ClearMatterToolMaterial.INSTANCE, 1, -2.8F, properties);
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity miner) {
        boolean result = super.mineBlock(stack, level, state, pos, miner);

        if (!level.isClientSide && result && isCorruptedBlock(state.getBlock())) {
            if (level instanceof ServerLevel serverLevel) {
                MatterHistoryManager.restoreMappedBlocks(serverLevel, pos, 1);
            }
        }

        return result;
    }

    private boolean isCorruptedBlock(Block block) {
        return block == ModBlocks.CORRUPTED_SOIL.get()
                || block == ModBlocks.DARK_MATTER_BLOCK.get()
                || block == ModBlocks.INFECTION_GROWTH.get();
    }
}
