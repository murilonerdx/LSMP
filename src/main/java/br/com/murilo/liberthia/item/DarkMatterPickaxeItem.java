package br.com.murilo.liberthia.item;

import br.com.murilo.liberthia.registry.ModBlocks;
import br.com.murilo.liberthia.registry.ModCapabilities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

public class DarkMatterPickaxeItem extends PickaxeItem {
    public DarkMatterPickaxeItem(Properties properties) {
        super(DarkMatterToolMaterial.INSTANCE, 1, -2.8F, properties);
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity miner) {
        boolean result = super.mineBlock(stack, level, state, pos, miner);

        if (!level.isClientSide && result && isDarkMatterOre(state.getBlock())) {
            // Vein mine connected dark matter ores (max 8 blocks)
            veinMine(level, pos, state.getBlock(), stack, miner, 8);
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

    private boolean isDarkMatterOre(Block block) {
        return block == ModBlocks.DARK_MATTER_ORE.get() || block == ModBlocks.DEEPSLATE_DARK_MATTER_ORE.get();
    }

    private void veinMine(Level level, BlockPos origin, Block oreBlock, ItemStack tool, LivingEntity miner, int maxBlocks) {
        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new LinkedList<>();
        visited.add(origin);
        int mined = 0;

        // Add adjacent positions
        for (BlockPos neighbor : BlockPos.betweenClosed(origin.offset(-1, -1, -1), origin.offset(1, 1, 1))) {
            BlockPos immutable = neighbor.immutable();
            if (!immutable.equals(origin)) {
                queue.add(immutable);
            }
        }

        while (!queue.isEmpty() && mined < maxBlocks) {
            BlockPos current = queue.poll();
            if (visited.contains(current)) continue;
            visited.add(current);

            BlockState currentState = level.getBlockState(current);
            if (isDarkMatterOre(currentState.getBlock())) {
                level.destroyBlock(current, true, miner);
                tool.hurtAndBreak(1, miner, e -> {});
                mined++;

                // Add neighbors of this block
                for (BlockPos neighbor : BlockPos.betweenClosed(current.offset(-1, -1, -1), current.offset(1, 1, 1))) {
                    BlockPos immutable = neighbor.immutable();
                    if (!visited.contains(immutable)) {
                        queue.add(immutable);
                    }
                }
            }
        }
    }
}
