package br.com.murilo.liberthia.occult;

import br.com.murilo.liberthia.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * v0.1.22 r32: <b>Círculo Ritualístico</b> — coração do sistema occult.
 *
 * <p>Player right-click com um {@link OccultItems.SigilItem}:
 * <ol>
 *   <li>Sigilo é identificado, ritual procurado em {@link RitualRegistry}</li>
 *   <li>BE escaneia raio 3b: chalks + candles (acesas) + items soltos</li>
 *   <li>Se config bater + recursos suficientes → começa countdown</li>
 *   <li>Durante countdown: particles, som, todos players próximos veem progress</li>
 *   <li>Ao completar: executa resultado (spawn entity, give items, effect, TP)</li>
 *   <li>Itens são consumidos, chalks/candles podem ser preservados pra repetir</li>
 * </ol>
 */
public class RitualCircleBlock extends BaseEntityBlock {

    public RitualCircleBlock(BlockBehaviour.Properties p) {
        super(p);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RitualCircleBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                   BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return createTickerHelper(type, ModBlockEntities.RITUAL_CIRCLE.get(),
                RitualCircleBlockEntity::serverTick);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                   InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof RitualCircleBlockEntity rcbe)) return InteractionResult.PASS;

        ItemStack inHand = player.getItemInHand(hand);
        if (inHand.getItem() instanceof OccultItems.SigilItem) {
            return rcbe.attemptStartRitual(player, inHand);
        }
        // Sem sigilo: mostra status
        rcbe.showStatus(player);
        return InteractionResult.SUCCESS;
    }
}
