package br.com.murilo.liberthia.block;

import br.com.murilo.liberthia.block.entity.MatterPurifierBlockEntity;
import br.com.murilo.liberthia.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

/**
 * Bloco do Matter Purifier — purifica ingots de matter via GUI.
 */
public class MatterPurifierBlock extends BaseEntityBlock {

    public MatterPurifierBlock(Properties props) {
        super(props);
    }

    @Override public RenderShape getRenderShape(BlockState s) { return RenderShape.MODEL; }

    @Override @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                  InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer sp) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof MatterPurifierBlockEntity purifier) {
                NetworkHooks.openScreen(sp, purifier, pos);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override @SuppressWarnings("deprecation")
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof MatterPurifierBlockEntity p) p.drops();
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }

    @Nullable @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MatterPurifierBlockEntity(pos, state);
    }

    @Nullable @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.MATTER_PURIFIER.get(),
                (lvl, pos, st, be) -> MatterPurifierBlockEntity.tick(lvl, pos, st, be));
    }

    public static Component getCreativeTabName() {
        return Component.translatable("block.liberthia.matter_purifier");
    }
}
