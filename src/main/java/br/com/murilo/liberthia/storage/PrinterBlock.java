package br.com.murilo.liberthia.storage;

import br.com.murilo.liberthia.item.computer.ComputerData;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

/**
 * <b>Impressora</b> — bloco com BlockEntity. Coloque ao lado de um Computador.
 * Clique-direito abre a GUI: lista dos relatórios do Computador vizinho + slot
 * de papel + botão Imprimir → gera um livro escrito.
 */
public class PrinterBlock extends Block implements EntityBlock {

    public PrinterBlock(Properties props) {
        super(props);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PrinterBlockEntity(pos, state);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer sp
                && level.getBlockEntity(pos) instanceof PrinterBlockEntity printer) {
            ComputerBlockEntity comp = printer.findComputer();
            CompoundTag filesWrap = (comp != null) ? ComputerData.wrap(comp.getFiles()) : new CompoundTag();
            NetworkHooks.openScreen(sp, printer, buf -> {
                buf.writeBlockPos(pos);
                buf.writeNbt(filesWrap);
            });
            return InteractionResult.CONSUME;
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof PrinterBlockEntity printer) {
            printer.drops();
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}
