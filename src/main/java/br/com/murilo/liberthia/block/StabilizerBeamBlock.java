package br.com.murilo.liberthia.block;

import br.com.murilo.liberthia.block.entity.StabilizerBeamBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * r180b — <b>Raio Estabilizador</b>: bloco que dispara um feixe direcional purificador
 * (limpa corrupção + dispersa o irreal). A direção é a que o colocador estava virado.
 * Mecânica em {@link StabilizerBeamBlockEntity}.
 */
public class StabilizerBeamBlock extends Block implements EntityBlock {

    public StabilizerBeamBlock(Properties props) {
        super(props);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new StabilizerBeamBlockEntity(pos, state);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return (lvl, pos, st, be) -> {
            if (be instanceof StabilizerBeamBlockEntity e) StabilizerBeamBlockEntity.tick(lvl, pos, st, e);
        };
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide && placer != null
                && level.getBlockEntity(pos) instanceof StabilizerBeamBlockEntity be) {
            be.setFacing(placer.getDirection());
        }
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof StabilizerBeamBlockEntity be) {
            player.displayClientMessage(Component.literal("§b§l═ Raio Estabilizador ═"), false);
            player.displayClientMessage(Component.literal("§7Energia: §b" + be.getEnergyStored() + " §7/ " + be.getMaxEnergy() + " FE"), false);
            player.displayClientMessage(Component.literal("§7Estado: " + (be.isActive() ? "§adisparando" : "§8dormente (sem energia)")), false);
            player.displayClientMessage(Component.literal("§7Direção: §f" + be.getFacing().getName()), false);
            player.displayClientMessage(Component.literal("§8§oLimpa corrupção na linha + dispersa entidades irreais."), false);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
