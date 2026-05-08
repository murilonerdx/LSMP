package br.com.murilo.liberthia.block;

import br.com.murilo.liberthia.block.entity.DarkMatterAlchemizerBlockEntity;
import br.com.murilo.liberthia.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

/**
 * Dark Matter Alchemizer — slow gambling machine.
 *
 * Slot 0: 1× dark_matter_block (input)
 * Slot 1: 1× lava_bucket (catalyst, returns empty bucket on success)
 * Slot 2: rare result (or empty after a violent failure)
 *
 * After a 200-tick brew, rolls 60% to drop a random rare item into the
 * output slot, 40% to detonate (vanilla 4.0 power explosion at the block).
 */
public class DarkMatterAlchemizerBlock extends BaseEntityBlock {
    public DarkMatterAlchemizerBlock(Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DarkMatterAlchemizerBlockEntity(pos, state);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide()) {
            if (level.getBlockEntity(pos) instanceof DarkMatterAlchemizerBlockEntity be) {
                NetworkHooks.openScreen((ServerPlayer) player, be, pos);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (state.getBlock() != newState.getBlock()) {
            if (level.getBlockEntity(pos) instanceof DarkMatterAlchemizerBlockEntity be) be.drops();
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.DARK_MATTER_ALCHEMIZER.get(), DarkMatterAlchemizerBlockEntity::tick);
    }
}
