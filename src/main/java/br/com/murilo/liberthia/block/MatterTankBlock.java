package br.com.murilo.liberthia.block;

import br.com.murilo.liberthia.block.entity.MatterTankBlockEntity;
import br.com.murilo.liberthia.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

/**
 * Matter Tank — bloco de armazenamento de fluido matter (dark / clear / yellow).
 *
 * <p>Capacidade: 16000 mB (16 buckets) por BE individual. Stacking vertical/horizontal
 * permite formar uma "rede" virtual onde os tanks compartilham fluido via auto-balance
 * por equalização (tick faz spread pra vizinhos do mesmo tipo).
 *
 * <p>Visual: blockstate {@link #LEVEL} 0..4 escolhe entre 5 modelos pré-fabricados
 * (vazio / 25% / 50% / 75% / cheio). Decisão de design: optei por blockstate ao invés
 * de BER (BlockEntityRenderer) pra simplicidade — BER custom envolveria buffers,
 * Tesselator, mais bug surface, e não vale a pena por essa fidelidade. 5 níveis é
 * suficiente pra dar leitura visual clara da capacidade.
 *
 * <p>Right-click abre a GUI {@link br.com.murilo.liberthia.menu.MatterTankMenu}.
 */
public class MatterTankBlock extends BaseEntityBlock {

    /** Nível visual do tank: 0=vazio, 1=25%, 2=50%, 3=75%, 4=cheio. */
    public static final IntegerProperty LEVEL = IntegerProperty.create("level", 0, 4);

    public MatterTankBlock(Properties props) {
        super(props);
        this.registerDefaultState(this.stateDefinition.any().setValue(LEVEL, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(LEVEL);
    }

    @Override
    public RenderShape getRenderShape(BlockState s) {
        return RenderShape.MODEL;
    }

    @Override @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer sp) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof MatterTankBlockEntity tank) {
                NetworkHooks.openScreen(sp, tank, pos);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override @SuppressWarnings("deprecation")
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            // Fluido NÃO é dropado quando o tank é quebrado — decisão consciente:
            // é fluido em forma "virtual", não tem item drop intuitivo (seria
            // bizarro dropar 16 buckets cheios). User pode esvaziar via "Purgar"
            // ou bucket out antes de quebrar.
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }

    @Nullable @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MatterTankBlockEntity(pos, state);
    }

    @Nullable @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.MATTER_TANK.get(),
                (lvl, pos, st, be) -> MatterTankBlockEntity.tick(lvl, pos, st, be));
    }

    public static Component getCreativeTabName() {
        return Component.translatable("block.liberthia.matter_tank");
    }
}
