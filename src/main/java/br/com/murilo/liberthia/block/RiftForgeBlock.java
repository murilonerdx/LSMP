package br.com.murilo.liberthia.block;

import br.com.murilo.liberthia.block.entity.RiftForgeBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
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
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

/** r185 — <b>Forja de Fendas</b>: bloco-máquina de 12 entradas para forjar a Adaga Corta-Fendas III. */
public class RiftForgeBlock extends Block implements EntityBlock {
    public RiftForgeBlock(Properties props) { super(props); }

    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new RiftForgeBlockEntity(pos, state); }

    @Override @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> t) {
        if (level.isClientSide) return null;
        return (lvl, pos, st, be) -> { if (be instanceof RiftForgeBlockEntity e) RiftForgeBlockEntity.tick(lvl, pos, st, e); };
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer sp
                && level.getBlockEntity(pos) instanceof RiftForgeBlockEntity be)
            NetworkHooks.openScreen(sp, be, b -> b.writeBlockPos(pos));
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof RiftForgeBlockEntity be) be.drops();
        super.onRemove(state, level, pos, newState, moved);
    }
}
