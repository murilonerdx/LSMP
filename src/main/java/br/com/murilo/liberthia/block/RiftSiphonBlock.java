package br.com.murilo.liberthia.block;

import br.com.murilo.liberthia.block.entity.RiftSiphonBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
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

/**
 * r180b — <b>Sifão de Fenda</b>: gerador que suga FE de Fendas Dimensionais próximas.
 * Clique direito = status. Mecânica em {@link RiftSiphonBlockEntity}.
 */
public class RiftSiphonBlock extends Block implements EntityBlock {

    public RiftSiphonBlock(Properties props) {
        super(props);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RiftSiphonBlockEntity(pos, state);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return (lvl, pos, st, be) -> {
            if (be instanceof RiftSiphonBlockEntity e) RiftSiphonBlockEntity.tick(lvl, pos, st, e);
        };
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof RiftSiphonBlockEntity be) {
            player.displayClientMessage(Component.literal("§5§l═ Sifão de Fenda ═"), false);
            player.displayClientMessage(Component.literal("§7Energia: §b" + be.getEnergyStored() + " §7/ " + be.getMaxEnergy() + " FE"), false);
            player.displayClientMessage(Component.literal("§7Fendas no alcance: §d" + be.getLastRifts()), false);
            player.displayClientMessage(Component.literal("§7Estado: " + (be.isActive() ? "§dsifonando" : "§8dormente (sem fendas)")), false);
            player.displayClientMessage(Component.literal("§8§oAbra Fendas Dimensionais a ≤8b pra alimentá-lo."), false);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
