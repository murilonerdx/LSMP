package br.com.murilo.liberthia.block;

import br.com.murilo.liberthia.block.entity.FuelGeneratorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/** r181 — Furnator / Magmator (gerador FE de combustível). Mecânica em {@link FuelGeneratorBlockEntity}. */
public class FuelGeneratorBlock extends Block implements EntityBlock {
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public FuelGeneratorBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(LIT, false));
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(LIT); }

    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new FuelGeneratorBlockEntity(pos, state); }

    @Override @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return (lvl, pos, st, be) -> { if (be instanceof FuelGeneratorBlockEntity e) FuelGeneratorBlockEntity.tick(lvl, pos, st, e); };
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof net.minecraft.server.level.ServerPlayer sp
                && level.getBlockEntity(pos) instanceof FuelGeneratorBlockEntity be)
            net.minecraftforge.network.NetworkHooks.openScreen(sp, be, b -> b.writeBlockPos(pos));
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof FuelGeneratorBlockEntity be) be.drops();
        super.onRemove(state, level, pos, newState, moved);
    }
}
