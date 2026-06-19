package br.com.murilo.liberthia.block;

import br.com.murilo.liberthia.block.entity.MatterPipeBlockEntity;
import br.com.murilo.liberthia.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * Matter Pipe — pipe especializado por tipo de matter (dark / clear / yellow).
 *
 * <p>Cada variante (3 blocos distintos no ModBlocks) só transporta o seu tipo.
 * Isso evita contaminação cruzada e simplifica a UX (cor do pipe = cor do
 * fluido, leitura visual instantânea).
 *
 * <p>Conexões: auto-conecta com pipes do MESMO tipo, tanks e extractor que
 * exponham FLUID_HANDLER capability. Connection visual via blockstate
 * booleans (igual ao ItemPipeBlock).
 *
 * <p>O tick (no BE) faz spread de fluido pra vizinhos do mesmo tipo de pipe.
 */
public class MatterPipeBlock extends BaseEntityBlock {

    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty EAST  = BlockStateProperties.EAST;
    public static final BooleanProperty WEST  = BlockStateProperties.WEST;
    public static final BooleanProperty UP    = BlockStateProperties.UP;
    public static final BooleanProperty DOWN  = BlockStateProperties.DOWN;

    private static final VoxelShape CORE    = Block.box(5, 5, 5, 11, 11, 11);
    private static final VoxelShape S_NORTH = Block.box(5, 5, 0, 11, 11, 5);
    private static final VoxelShape S_SOUTH = Block.box(5, 5, 11, 11, 11, 16);
    private static final VoxelShape S_WEST  = Block.box(0, 5, 5, 5, 11, 11);
    private static final VoxelShape S_EAST  = Block.box(11, 5, 5, 16, 11, 11);
    private static final VoxelShape S_DOWN  = Block.box(5, 0, 5, 11, 5, 11);
    private static final VoxelShape S_UP    = Block.box(5, 11, 5, 11, 16, 11);

    /** Tipo de fluido permitido por esse pipe. Resolvido via supplier pra evitar ordem de init. */
    private final Supplier<Fluid> allowedFluid;

    public MatterPipeBlock(Properties props, Supplier<Fluid> allowedFluid) {
        super(props);
        this.allowedFluid = allowedFluid;
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(NORTH, false).setValue(SOUTH, false)
                .setValue(EAST,  false).setValue(WEST,  false)
                .setValue(UP,    false).setValue(DOWN,  false));
    }

    public Fluid getAllowedFluid() { return allowedFluid.get(); }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(NORTH, SOUTH, EAST, WEST, UP, DOWN);
    }

    @Override public RenderShape getRenderShape(BlockState s) { return RenderShape.MODEL; }
    @Override public boolean propagatesSkylightDown(BlockState s, BlockGetter g, BlockPos p) { return true; }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter g, BlockPos p, CollisionContext c) {
        VoxelShape shape = CORE;
        if (state.getValue(NORTH)) shape = Shapes.or(shape, S_NORTH);
        if (state.getValue(SOUTH)) shape = Shapes.or(shape, S_SOUTH);
        if (state.getValue(EAST))  shape = Shapes.or(shape, S_EAST);
        if (state.getValue(WEST))  shape = Shapes.or(shape, S_WEST);
        if (state.getValue(UP))    shape = Shapes.or(shape, S_UP);
        if (state.getValue(DOWN))  shape = Shapes.or(shape, S_DOWN);
        return shape;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState s, BlockGetter g, BlockPos p, CollisionContext c) {
        return getShape(s, g, p, c);
    }

    @Nullable @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return computeConnections(super.defaultBlockState(), ctx.getLevel(), ctx.getClickedPos());
    }

    @Override
    public BlockState updateShape(BlockState state, Direction dir, BlockState neighborState,
                                  LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return state.setValue(propertyFor(dir), shouldConnect(level, pos, dir));
    }

    private static BooleanProperty propertyFor(Direction d) {
        return switch (d) {
            case NORTH -> NORTH; case SOUTH -> SOUTH;
            case EAST -> EAST;   case WEST -> WEST;
            case UP -> UP;       case DOWN -> DOWN;
        };
    }

    private BlockState computeConnections(BlockState base, LevelAccessor level, BlockPos pos) {
        for (Direction d : Direction.values()) {
            base = base.setValue(propertyFor(d), shouldConnect(level, pos, d));
        }
        return base;
    }

    /**
     * Conecta com:
     *  - Outro MatterPipeBlock do MESMO tipo (mesmo allowedFluid).
     *  - Qualquer BE que expor FLUID_HANDLER capability (tank, extractor, etc).
     */
    private boolean shouldConnect(LevelAccessor level, BlockPos pos, Direction dir) {
        BlockPos npos = pos.relative(dir);
        BlockState nstate = level.getBlockState(npos);
        if (nstate.getBlock() instanceof MatterPipeBlock other) {
            // Pipes só conectam entre si se for o mesmo tipo de fluido
            return other.getAllowedFluid() == this.getAllowedFluid();
        }
        BlockEntity nbe = level.getBlockEntity(npos);
        if (nbe == null) return false;
        return nbe.getCapability(ForgeCapabilities.FLUID_HANDLER, dir.getOpposite()).isPresent();
    }

    @Nullable @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MatterPipeBlockEntity(pos, state, allowedFluid);
    }

    @Nullable @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return createTickerHelper(type, ModBlockEntities.MATTER_PIPE.get(),
                (lvl, pos, st, be) -> MatterPipeBlockEntity.tick(lvl, pos, st, be));
    }
}
