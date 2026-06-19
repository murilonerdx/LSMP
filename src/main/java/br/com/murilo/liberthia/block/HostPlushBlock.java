package br.com.murilo.liberthia.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.LevelAccessor;
import org.jetbrains.annotations.Nullable;

/**
 * Bloco decorativo "Host Plush" — pelúcia 3D que vai no chão.
 *
 * <p>Características:
 * <ul>
 *   <li><b>FACING horizontal</b>: roda pra rosto pro player na hora de colocar.</li>
 *   <li><b>Não-cubo</b>: hitbox/colisão estreita (14×16×12) — player pode colar perto
 *       sem o bloco vazar nas adjacências.</li>
 *   <li><b>Waterloggable</b>: pode coexistir com água (aquário com pelúcia, river deco).</li>
 *   <li><b>Quebra fácil</b>: hardness baixo, sem ferramenta necessária — é deco, não fortaleza.</li>
 *   <li><b>Drop self</b>: configurado via loot_table (não precisa override Java).</li>
 * </ul>
 *
 * <p>Visual vem do {@code models/item/host_plush.json} (modelo Blockbench 3D com
 * head/body/arms/legs como bone groups). Esse bloco SÓ resolve o lado do jogo —
 * blockstate referencia o model existente.
 */
public class HostPlushBlock extends HorizontalDirectionalBlock implements SimpleWaterloggedBlock {

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    /**
     * Hitbox apertada — o modelo 3D do plush tem ~12 voxels de altura mas
     * espalha braços/pernas pelos 14-15. Mantemos a colisão num cubo central
     * que NÃO impede o player de colar nas adjacências (importante pra decor
     * em mesas/estantes).
     */
    private static final VoxelShape SHAPE = Block.box(2, 0, 3, 14, 14, 14);

    public HostPlushBlock(Properties props) {
        super(props);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(WATERLOGGED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(FACING, WATERLOGGED);
    }

    @Nullable @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        // FACING vira pra direção OPOSTA ao player — pelúcia "olha" pra ele.
        FluidState fluid = ctx.getLevel().getFluidState(ctx.getClickedPos());
        return this.defaultBlockState()
                .setValue(FACING, ctx.getHorizontalDirection().getOpposite())
                .setValue(WATERLOGGED, fluid.getType() == Fluids.WATER);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter g, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState s, BlockGetter g, BlockPos p, CollisionContext c) {
        return SHAPE;
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction dir, BlockState neighborState,
                                   LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (state.getValue(WATERLOGGED)) {
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        return super.updateShape(state, dir, neighborState, level, pos, neighborPos);
    }
}
