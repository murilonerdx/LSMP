package br.com.murilo.liberthia.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * v0.1.24 r82: WhisperwoodLeavesBlock — folhas BULLETPROOF da Spirit World.
 *
 * <p>Override TODOS os métodos que podem causar decay:
 * <ul>
 *   <li>{@code randomTick} → no-op</li>
 *   <li>{@code tick} → no-op (scheduled ticks via updateShape)</li>
 *   <li>{@code updateShape} → força {@code persistent=true, distance=1}, sem schedule</li>
 *   <li>{@code onPlace} → força {@code persistent=true}</li>
 *   <li>{@code isRandomlyTicking} → false (Forge ainda pode chamar randomTick mas evita o sistema)</li>
 * </ul>
 *
 * <p>User reportou: "as folhas não sabem que o tronco está nelas". Esta classe
 * resolve definitivamente — folhas NUNCA decaem, independente da distância do log.
 */
public class WhisperwoodLeavesBlock extends LeavesBlock {

    public WhisperwoodLeavesBlock(Properties props) {
        super(props);
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // No-op: nunca decai aleatoriamente
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // No-op: vanilla LeavesBlock.tick chama updateDistance que pode decay.
        // Bloqueamos completamente o scheduled tick.
        // Garantia adicional: força persistent=true caso algo tenha setado false
        if (!state.getValue(PERSISTENT)) {
            level.setBlock(pos, state.setValue(PERSISTENT, true).setValue(DISTANCE, 1), 3);
        }
    }

    @Override
    public boolean isRandomlyTicking(BlockState state) {
        // Reduz pressão no tick loop — não temos mais decay
        return false;
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction,
                                   BlockState neighborState, LevelAccessor level,
                                   BlockPos pos, BlockPos neighborPos) {
        // Vanilla updateShape pode SCHEDULAR um tick pra recalcular distância.
        // Aqui retornamos persistent=true e NÃO scheduiamos nada — leaves param.
        return state.setValue(PERSISTENT, true).setValue(DISTANCE, 1);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        // Vanilla onPlace chama updateDistance(state, level, pos) que pode setar
        // distance=7 + persistent=false. Aqui REVERTEMOS forçando persistent=true.
        if (!state.getValue(PERSISTENT)) {
            level.setBlock(pos, state.setValue(PERSISTENT, true).setValue(DISTANCE, 1), 3);
        }
        // NÃO chama super.onPlace pra evitar scheduling de tick
    }
}
