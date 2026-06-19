package br.com.murilo.liberthia.block;

import br.com.murilo.liberthia.block.entity.SanityReactorBlockEntity;
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
 * r180b — <b>Reator de Sanidade</b>: gerador de FE que devora a sanidade de quem fica
 * colado. Clique direito = status. Mecânica em {@link SanityReactorBlockEntity}.
 */
public class SanityReactorBlock extends Block implements EntityBlock {

    public SanityReactorBlock(Properties props) {
        super(props);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SanityReactorBlockEntity(pos, state);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return (lvl, pos, st, be) -> {
            if (be instanceof SanityReactorBlockEntity e) SanityReactorBlockEntity.tick(lvl, pos, st, e);
        };
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof SanityReactorBlockEntity be) {
            player.displayClientMessage(Component.literal("§5§l═ Reator de Sanidade ═"), false);
            player.displayClientMessage(Component.literal("§7Energia: §b" + be.getEnergyStored() + " §7/ " + be.getMaxEnergy() + " FE"), false);
            player.displayClientMessage(Component.literal("§7Estado: " + (be.isActive() ? "§ddevorando" : "§8dormente (ninguém colado)")), false);
            player.displayClientMessage(Component.literal("§7Geração (último ciclo): §a" + be.getLastGen() + " FE"), false);
            player.displayClientMessage(Component.literal("§8§oFique colado pra alimentá-lo — quanto menos sã sua mente, mais energia."), false);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
