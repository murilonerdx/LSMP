package br.com.murilo.liberthia.block;

import br.com.murilo.liberthia.block.entity.LaserEmitterBlockEntity;
import br.com.murilo.liberthia.registry.ModBlockEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
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
import org.jetbrains.annotations.Nullable;

/**
 * Bloco do Laser Emitter — direção(ões) ativa(s) ficam no
 * {@link LaserEmitterBlockEntity}, não no BlockState.
 *
 * <p>Interação:
 * <ul>
 *   <li><b>Shift + right-click numa face</b> — toggle se aquela face dispara.</li>
 *   <li><b>Right-click sem shift</b> — mostra o status (faces ativas).</li>
 * </ul>
 *
 * <p>Você pode ter múltiplas faces ativas simultaneamente — cada uma consome
 * 500 FE/tick.
 */
public class LaserEmitterBlock extends BaseEntityBlock {

    public LaserEmitterBlock(Properties p) { super(p); }

    @Override public RenderShape getRenderShape(BlockState s) { return RenderShape.MODEL; }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof LaserEmitterBlockEntity be))
            return InteractionResult.PASS;

        if (player.isShiftKeyDown()) {
            // Toggle a face em que o jogador clicou.
            Direction face = hit.getDirection();
            boolean nowActive = be.toggleFacing(face);
            player.displayClientMessage(
                    Component.literal((nowActive ? "ATIVADO" : "Desativado") + " — "
                            + face.getName().toUpperCase())
                            .withStyle(nowActive ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.GRAY),
                    true);
        } else {
            // Mostra o status.
            String list = be.getActiveFacings().isEmpty()
                    ? "(nenhuma)"
                    : be.getActiveFacings().stream()
                            .map(Direction::getName)
                            .reduce((a, b) -> a + ", " + b).orElse("");
            player.displayClientMessage(
                    Component.literal("Faces ativas: " + list)
                            .withStyle(ChatFormatting.AQUA), false);
        }
        return InteractionResult.CONSUME;
    }

    /**
     * Quando o bloco é destruído COM feixe ativo, explode. Sem feixe = remove normal.
     */
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && !level.isClientSide) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof LaserEmitterBlockEntity laser && laser.isAnyBeamActive()) {
                // Explosão proporcional ao número de feixes ativos.
                float power = 2.0f + Math.min(2.0f, laser.getActiveFacings().size() * 0.5f);
                level.explode(null,
                        pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                        power, true,
                        Level.ExplosionInteraction.BLOCK);
            }
        }
        super.onRemove(state, level, pos, newState, moved);
    }

    @Nullable @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState s) {
        return new LaserEmitterBlockEntity(pos, s);
    }
    @Nullable @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState s, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.LASER_EMITTER.get(),
                LaserEmitterBlockEntity::tick);
    }
}
