package br.com.murilo.liberthia.block;

import br.com.murilo.liberthia.block.entity.MatterPillBrewerBlockEntity;
import br.com.murilo.liberthia.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

/**
 * v0.1.44: Bloco do Matter Pill Brewer — fabrica pílulas alquímicas a partir
 * de ingots purificados + glass bottle. Sem energia, alquimia simples (3s/lote).
 *
 * <p>Recipes:
 * <ul>
 *   <li>Purified Dark Matter Ingot + Glass Bottle → 3× Dark Matter Pill</li>
 *   <li>Purified Clear Matter Ingot + Glass Bottle → 3× Clear Matter Pill</li>
 *   <li>Purified Yellow Matter Ingot + Glass Bottle → 3× Yellow Matter Pill</li>
 * </ul>
 */
public class MatterPillBrewerBlock extends BaseEntityBlock {

    public MatterPillBrewerBlock(Properties props) {
        super(props);
    }

    @Override public RenderShape getRenderShape(BlockState s) { return RenderShape.MODEL; }

    @Override @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                  InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer sp) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof MatterPillBrewerBlockEntity brewer) {
                NetworkHooks.openScreen(sp, brewer, pos);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override @SuppressWarnings("deprecation")
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof MatterPillBrewerBlockEntity p) p.drops();
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }

    @Nullable @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MatterPillBrewerBlockEntity(pos, state);
    }

    @Nullable @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.MATTER_PILL_BREWER.get(),
                (lvl, pos, st, be) -> MatterPillBrewerBlockEntity.tick(lvl, pos, st, be));
    }

    public static Component getCreativeTabName() {
        return Component.translatable("block.liberthia.matter_pill_brewer");
    }
}
