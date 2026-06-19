package br.com.murilo.liberthia.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * v0.1.44: BloodLeavesBlock custom — sobrescreve {@link LeavesBlock} pra
 * IMPEDIR decay quando o algoritmo vanilla não reconhece o blood_log como
 * tronco. User reclamou que "as folhas dela não detectam que ela é uma
 * arvore e começam a sumir".
 *
 * <p>Solução: tag {@code minecraft:logs} foi adicionada (data/minecraft/tags/
 * blocks/logs.json) MAS pra garantia adicional, esse bloco também:
 * <ul>
 *   <li>Sobrescreve {@link #updateDistance} pra sempre setar {@code persistent=true}.</li>
 *   <li>Sobrescreve {@link #randomTick} pra ser no-op (sem decay).</li>
 * </ul>
 *
 * <p>Folhas continuam dropando saplings em drop loot e podem ser quebradas
 * normalmente. Só não DESAPARECEM por estarem longe de tronco.
 */
public class BloodLeavesBlock extends LeavesBlock {

    public BloodLeavesBlock(Properties props) {
        super(props);
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // No-op: folhas de sangue não fazem decay. Vanilla LeavesBlock.randomTick
        // chama updateDistance + decay se distance == 7. Aqui ignoramos.
    }

    @Override
    public BlockState updateShape(BlockState state, net.minecraft.core.Direction direction,
                                   BlockState neighborState, LevelAccessor level,
                                   BlockPos pos, BlockPos neighborPos) {
        // Vanilla updateShape chama updateDistance pra recalcular `distance` quando
        // logs próximos mudam. Aqui forçamos PERSISTENT=true (mesmo flag de folhas
        // colocadas por player com silk touch) — nunca decai.
        return state.setValue(PERSISTENT, true).setValue(DISTANCE, 1);
    }
}
