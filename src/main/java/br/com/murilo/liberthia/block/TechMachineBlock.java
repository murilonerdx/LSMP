package br.com.murilo.liberthia.block;

import br.com.murilo.liberthia.block.entity.TechMachineBlockEntity;
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

/**
 * r180c — máquina tech (Mekanism-style, COM GUI). Tipo define a receita (ver
 * {@link TechMachineBlockEntity}). Right-click abre a GUI (slot in→out + barra de
 * energia + progresso). Auto-processa consumindo FE recebido das células.
 */
public class TechMachineBlock extends Block implements EntityBlock {

    public enum Type { CRUSHER, SMELTER, COMPRESSOR, ALLOY, SAWMILL }

    private final Type type;

    public TechMachineBlock(Type type, Properties props) { super(props); this.type = type; }

    public Type getType() { return type; }

    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TechMachineBlockEntity(pos, state);
    }

    @Override @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> beType) {
        if (level.isClientSide) return null;
        return (lvl, pos, st, be) -> { if (be instanceof TechMachineBlockEntity e) TechMachineBlockEntity.tick(lvl, pos, st, e); };
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer sp
                && level.getBlockEntity(pos) instanceof TechMachineBlockEntity be) {
            NetworkHooks.openScreen(sp, be, buf -> buf.writeBlockPos(pos));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof TechMachineBlockEntity be) {
            be.drops();
        }
        super.onRemove(state, level, pos, newState, moved);
    }
}
