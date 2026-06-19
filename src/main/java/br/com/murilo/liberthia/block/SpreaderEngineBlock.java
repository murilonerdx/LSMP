package br.com.murilo.liberthia.block;

import br.com.murilo.liberthia.block.entity.SpreaderEngineBlockEntity;
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
 * r183 — Motor Espalhador (Entropy / Black Matter). Mecânica em {@link SpreaderEngineBlockEntity}.
 * Ao ser DESTRUÍDO, reverte tudo que trocou (via EntropyTracker). Clique = status.
 */
public class SpreaderEngineBlock extends Block implements EntityBlock {
    public SpreaderEngineBlock(Properties props) { super(props); }

    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new SpreaderEngineBlockEntity(pos, state); }

    @Override @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return (lvl, pos, st, be) -> { if (be instanceof SpreaderEngineBlockEntity e) SpreaderEngineBlockEntity.tick(lvl, pos, st, e); };
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof SpreaderEngineBlockEntity be && level.getServer() != null) {
            int tracked = br.com.murilo.liberthia.logic.entropy.EntropyTracker.count(level.getServer(), be.getId());
            player.displayClientMessage(Component.literal("§5☣ Entropia §8[§7" + be.getId() + "§8] §fintensidade " + be.getIntensity()
                    + "/200 §8· §7" + tracked + " blocos mapeados §8(quebre p/ reverter)"), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof SpreaderEngineBlockEntity be) be.revertOnDestroy();
        super.onRemove(state, level, pos, newState, moved);
    }
}
