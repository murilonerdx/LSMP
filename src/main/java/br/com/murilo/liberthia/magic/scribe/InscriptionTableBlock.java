package br.com.murilo.liberthia.magic.scribe;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;

/**
 * r164: <b>Inscription Table</b> com GUI completa — recipe panel à esquerda,
 * 3 slots (input/ink/output) à direita.
 */
public class InscriptionTableBlock extends Block implements EntityBlock {

    public InscriptionTableBlock(BlockBehaviour.Properties props) {
        super(props);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new InscriptionTableBlockEntity(pos, state);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                  InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof MenuProvider mp && player instanceof ServerPlayer sp) {
            NetworkHooks.openScreen(sp, mp, buf -> buf.writeBlockPos(pos));
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof InscriptionTableBlockEntity itbe) {
                // Drop conteúdo dos 3 slots
                for (int i = 0; i < InscriptionTableBlockEntity.TOTAL_SLOTS; i++) {
                    Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(),
                            itbe.getItemHandler().getStackInSlot(i));
                }
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}
