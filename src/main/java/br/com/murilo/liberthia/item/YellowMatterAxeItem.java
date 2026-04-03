package br.com.murilo.liberthia.item;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

public class YellowMatterAxeItem extends AxeItem {
    private static final int MAX_FELL_BLOCKS = 20;

    public YellowMatterAxeItem(Properties properties) {
        super(YellowMatterToolMaterial.INSTANCE, 5.0F, -3.0F, properties);
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity miner) {
        boolean result = super.mineBlock(stack, level, state, pos, miner);

        if (!level.isClientSide && result && state.is(BlockTags.LOGS)) {
            // 3x3 tree felling: mine all connected LOGS blocks via BFS (up to 20 blocks)
            fellTree(level, pos, stack, miner);
        }

        return result;
    }

    private void fellTree(Level level, BlockPos origin, ItemStack tool, LivingEntity miner) {
        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new LinkedList<>();
        visited.add(origin);
        int felled = 0;

        // Seed BFS with neighbors of origin
        for (BlockPos neighbor : BlockPos.betweenClosed(origin.offset(-1, -1, -1), origin.offset(1, 1, 1))) {
            BlockPos immutable = neighbor.immutable();
            if (!immutable.equals(origin)) {
                queue.add(immutable);
            }
        }

        while (!queue.isEmpty() && felled < MAX_FELL_BLOCKS) {
            BlockPos current = queue.poll();
            if (visited.contains(current)) continue;
            visited.add(current);

            BlockState currentState = level.getBlockState(current);
            if (currentState.is(BlockTags.LOGS)) {
                level.destroyBlock(current, true, miner);
                tool.hurtAndBreak(1, miner, e -> {});
                felled++;

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
