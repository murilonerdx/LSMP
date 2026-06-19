package br.com.murilo.liberthia.block;

import br.com.murilo.liberthia.block.entity.arcane.ArcaneMachineBlockEntity;
import br.com.murilo.liberthia.block.entity.arcane.ArcaneMachineType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/** r184 — bloco genérico de máquina Tecno/Tecno-Arcano. Mecânica em {@link ArcaneMachineBlockEntity}. */
public class ArcaneMachineBlock extends Block implements EntityBlock {
    public final ArcaneMachineType type;

    public ArcaneMachineBlock(ArcaneMachineType type, Properties props) { super(props); this.type = type; }

    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new ArcaneMachineBlockEntity(pos, state); }

    @Override @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> t) {
        if (level.isClientSide) return null;
        return (lvl, pos, st, be) -> { if (be instanceof ArcaneMachineBlockEntity e) ArcaneMachineBlockEntity.tick(lvl, pos, st, e); };
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof net.minecraft.server.level.ServerPlayer sp
                && level.getBlockEntity(pos) instanceof ArcaneMachineBlockEntity be)
            net.minecraftforge.network.NetworkHooks.openScreen(sp, be, b -> b.writeBlockPos(pos));
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof ArcaneMachineBlockEntity be) be.drops();
        super.onRemove(state, level, pos, newState, moved);
    }
}
