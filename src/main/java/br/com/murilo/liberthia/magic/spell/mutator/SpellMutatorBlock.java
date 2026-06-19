package br.com.murilo.liberthia.magic.spell.mutator;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;

/**
 * v0.1.162 r138: <b>Spell Mutator</b> — bloco que combina 2 scrolls em 1 híbrido.
 *
 * <p>Slots: 2 inputs (scroll A, scroll B) + 1 output. Algoritmo:
 * <ul>
 *   <li>Output mantém a {@code SpellSchool} do scroll A (mais "dominante")</li>
 *   <li>Damage = média dos 2 spells</li>
 *   <li>Mana cost = max dos 2</li>
 *   <li>Cooldown = média dos 2</li>
 *   <li>Range = max dos 2</li>
 *   <li>Nome = "{A} ⨯ {B}"</li>
 * </ul>
 *
 * <p>Right-click abre menu (sem GUI por enquanto — pickup/drop slots via dispenser-like).
 */
public class SpellMutatorBlock extends Block implements EntityBlock {

    public SpellMutatorBlock(BlockBehaviour.Properties props) {
        super(props);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SpellMutatorBlockEntity(pos, state);
    }

    @Override
    @SuppressWarnings({"unchecked","rawtypes"})
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                    BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return (lvl, pos, st, be) -> {
            if (be instanceof SpellMutatorBlockEntity t) SpellMutatorBlockEntity.serverTick(lvl, pos, st, t);
        };
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                  Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof SpellMutatorBlockEntity tile)) return InteractionResult.PASS;
        if (player instanceof ServerPlayer sp) {
            NetworkHooks.openScreen(sp, tile, pos);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof SpellMutatorBlockEntity t) t.drops();
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}
