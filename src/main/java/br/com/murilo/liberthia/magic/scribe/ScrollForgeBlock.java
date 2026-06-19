package br.com.murilo.liberthia.magic.scribe;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;

/**
 * r165: <b>Scroll Forge Block</b> — converte Focus de Escola em Scroll de Escola.
 *
 * <p>Right-click abre uma GUI bonita com:
 * <ul>
 *   <li>Slot de Focus (input — aceita focus_fire, focus_ice, …)</li>
 *   <li>Output scroll gerado automaticamente ao colocar o Focus</li>
 *   <li>Tabela de mapeamento no painel esquerdo (7 escolas)</li>
 * </ul>
 */
public class ScrollForgeBlock extends BaseEntityBlock {

    public ScrollForgeBlock(BlockBehaviour.Properties props) {
        super(props);
    }

    // ── EntityBlock ──────────────────────────────────────────────────────────

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ScrollForgeBlockEntity(pos, state);
    }

    /**
     * BaseEntityBlock retorna INVISIBLE por padrão (pensado pra blocos com BER).
     * Como o Scroll Forge usa um modelo JSON normal (cube_bottom_top), precisamos
     * forçar MODEL — senão o bloco fica <b>invisível</b> no mundo (bug reportado).
     */
    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    // ── Interaction ──────────────────────────────────────────────────────────

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                  InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof MenuProvider mp && player instanceof ServerPlayer sp) {
            NetworkHooks.openScreen(sp, mp, buf -> buf.writeBlockPos(pos));
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    // ── Block removal — drop contents ─────────────────────────────────────────

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof ScrollForgeBlockEntity sfbe) {
                for (int i = 0; i < ScrollForgeBlockEntity.TOTAL_SLOTS; i++) {
                    Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(),
                            sfbe.getItemHandler().getStackInSlot(i));
                }
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}
