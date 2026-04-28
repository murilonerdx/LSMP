package br.com.murilo.liberthia.logic;

import br.com.murilo.liberthia.block.entity.BloodSacrificialBowlBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Stone bowl that holds a single item used as a ritual ingredient.
 *   • Right-click with item → place 1 in the bowl.
 *   • Right-click with empty hand → take the item back.
 *   • Broken → drops the held item.
 */
public class BloodSacrificialBowlBlock extends Block implements EntityBlock {

    private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 8, 14);

    public BloodSacrificialBowlBlock(Properties props) {
        super(props);
    }

    @Override
    public VoxelShape getShape(BlockState s, BlockGetter g, BlockPos p, CollisionContext c) {
        return SHAPE;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                 Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof BloodSacrificialBowlBlockEntity bowl)) return InteractionResult.PASS;

        ItemStack inHand = player.getItemInHand(hand);
        if (bowl.isEmpty() && !inHand.isEmpty()) {
            if (bowl.tryPlace(inHand)) {
                if (!player.getAbilities().instabuild) inHand.shrink(1);
            }
            return InteractionResult.CONSUME;
        }
        if (!bowl.isEmpty() && inHand.isEmpty()) {
            ItemStack taken = bowl.takeItem();
            if (!player.getInventory().add(taken)) {
                player.drop(taken, false);
            }
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos,
                         BlockState newState, boolean moving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof BloodSacrificialBowlBlockEntity bowl && !bowl.isEmpty()) {
                Containers.dropItemStack(level, pos.getX(), pos.getY() + 0.4, pos.getZ(),
                        bowl.takeItem());
            }
        }
        super.onRemove(state, level, pos, newState, moving);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BloodSacrificialBowlBlockEntity(pos, state);
    }
}
