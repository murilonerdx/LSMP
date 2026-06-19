package br.com.murilo.liberthia.block;

import br.com.murilo.liberthia.block.entity.MatterExtractorBlockEntity;
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
 * Matter Extractor — bloco central da rede que drena matter de player vivo próximo.
 *
 * <p>Funcionamento: a cada ciclo (~5s, configurado no BE), procura por um Player
 * num raio de 3 blocos. Se acha e o player tem profile com matter ≥ 5, drena
 * 20% da maior matter, converte em fluido equivalente e despeja no FluidHandler
 * mais próximo (tank/pipe conectado). Custa 200k FE por extração.
 *
 * <p>Player extraído sofre Slowness II por 60 ticks como side effect mecânico
 * (justifica a mecânica de "extração doi" + balance — extrações em loop ficam
 * caras se o player não puder andar).
 */
public class MatterExtractorBlock extends BaseEntityBlock {

    public MatterExtractorBlock(Properties props) {
        super(props);
    }

    @Override
    public RenderShape getRenderShape(BlockState s) { return RenderShape.MODEL; }

    @Override @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer sp) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof MatterExtractorBlockEntity ext) {
                NetworkHooks.openScreen(sp, ext, pos);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override @SuppressWarnings("deprecation")
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }

    @Nullable @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MatterExtractorBlockEntity(pos, state);
    }

    @Nullable @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.MATTER_EXTRACTOR.get(),
                (lvl, pos, st, be) -> MatterExtractorBlockEntity.tick(lvl, pos, st, be));
    }

    public static Component getCreativeTabName() {
        return Component.translatable("block.liberthia.matter_extractor");
    }
}
