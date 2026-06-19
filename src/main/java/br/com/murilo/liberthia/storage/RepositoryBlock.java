package br.com.murilo.liberthia.storage;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/**
 * v0.1.24 r97: <b>Repository</b> — sorting chest variant que route items
 * via {@link RepositoryBlockEntity}.
 */
public class RepositoryBlock extends BaseEntityBlock {

    public RepositoryBlock(BlockBehaviour.Properties props) {
        super(props);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RepositoryBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState s) { return RenderShape.MODEL; }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            if (level.getBlockEntity(pos) instanceof RepositoryBlockEntity be) {
                Containers.dropContents(level, pos, be);
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}
