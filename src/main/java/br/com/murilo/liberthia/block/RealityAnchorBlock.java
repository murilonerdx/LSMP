package br.com.murilo.liberthia.block;

import br.com.murilo.liberthia.block.entity.RealityAnchorBlockEntity;
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
 * r180b — <b>Âncora de Realidade</b>: máquina que consome FE pra estabilizar a região
 * (reduz corrupção + acalma a mente). Clique direito = status. Mecânica em
 * {@link RealityAnchorBlockEntity}.
 */
public class RealityAnchorBlock extends Block implements EntityBlock {

    public RealityAnchorBlock(Properties props) {
        super(props);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RealityAnchorBlockEntity(pos, state);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return (lvl, pos, st, be) -> {
            if (be instanceof RealityAnchorBlockEntity e) RealityAnchorBlockEntity.tick(lvl, pos, st, e);
        };
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof RealityAnchorBlockEntity be) {
            int c = be.getLastContamination();
            player.displayClientMessage(Component.literal("§b§l═ Âncora de Realidade ═"), false);
            player.displayClientMessage(Component.literal("§7Energia: §b" + be.getEnergyStored() + " §7/ " + be.getMaxEnergy() + " FE"), false);
            player.displayClientMessage(Component.literal("§7Estado: " + (be.isActive() ? "§aestabilizando" : "§8dormente (sem energia)")), false);
            player.displayClientMessage(Component.literal("§7Corrupção local: §f" + c + "%"), false);
            player.displayClientMessage(Component.literal("§8§oReduz corrupção 3×3 chunks + acalma a mente (24b)."), false);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
