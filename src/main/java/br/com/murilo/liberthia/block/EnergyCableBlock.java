package br.com.murilo.liberthia.block;

import br.com.murilo.liberthia.block.entity.EnergyCableBlockEntity;
import br.com.murilo.liberthia.registry.ModBlockEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import org.jetbrains.annotations.Nullable;

/**
 * Cabo de Energia — auto-conecta a vizinhos com FE. Right-click numa face
 * desliga aquela face (visual e funcionalmente).
 */
public class EnergyCableBlock extends BaseEntityBlock {

    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty EAST  = BlockStateProperties.EAST;
    public static final BooleanProperty WEST  = BlockStateProperties.WEST;
    public static final BooleanProperty UP    = BlockStateProperties.UP;
    public static final BooleanProperty DOWN  = BlockStateProperties.DOWN;

    private static final VoxelShape CORE  = Block.box(5, 5, 5, 11, 11, 11);
    private static final VoxelShape S_NORTH = Block.box(5, 5, 0, 11, 11, 5);
    private static final VoxelShape S_SOUTH = Block.box(5, 5, 11, 11, 11, 16);
    private static final VoxelShape S_WEST  = Block.box(0, 5, 5, 5, 11, 11);
    private static final VoxelShape S_EAST  = Block.box(11, 5, 5, 16, 11, 11);
    private static final VoxelShape S_DOWN  = Block.box(5, 0, 5, 11, 5, 11);
    private static final VoxelShape S_UP    = Block.box(5, 11, 5, 11, 16, 11);

    public EnergyCableBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(NORTH, false).setValue(SOUTH, false)
                .setValue(EAST,  false).setValue(WEST,  false)
                .setValue(UP,    false).setValue(DOWN,  false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(NORTH, SOUTH, EAST, WEST, UP, DOWN);
    }

    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
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
    public VoxelShape getCollisionShape(BlockState state, BlockGetter g, BlockPos p, CollisionContext c) {
        return getShape(state, g, p, c);
    }

    @Nullable @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return computeConnections(super.defaultBlockState(), ctx.getLevel(), ctx.getClickedPos());
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                  LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return state.setValue(propertyFor(direction),
                shouldConnect(level, pos, direction));
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        // Cabos só respondem ao Liberthia Wrench. Mão vazia / outros itens não fazem nada.
        if (player.getItemInHand(hand).is(br.com.murilo.liberthia.registry.ModItems.LIBERTHIA_WRENCH.get())) {
            return InteractionResult.PASS; // deixa o WrenchItem.useOn lidar
        }
        if (!level.isClientSide) {
            player.displayClientMessage(
                    Component.literal("Use a Chave Inglesa pra reconfigurar")
                            .withStyle(ChatFormatting.GRAY), true);
        }
        return InteractionResult.PASS;
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
     * Recomputa conexões VISUAIS — chamada de cycleMode() para atualizar o
     * BlockState quando o jogador desliga/liga uma face.
     *
     * <p>Usa flag (1|2|16) = UPDATE_CLIENTS + UPDATE_NEIGHBORS + NO_NEIGHBOR_DROPS.
     * 16 evita que blocos vizinhos "caiam" (pra blocos como tochas que precisam
     * suporte) — irrelevante pra cabo mas safer.
     */
    public static void recomputeConnections(LevelAccessor level, BlockPos pos) {
        BlockState s = level.getBlockState(pos);
        if (!(s.getBlock() instanceof EnergyCableBlock cable)) return;
        BlockState newState = cable.computeConnections(cable.defaultBlockState(), level, pos);
        // Só faz setBlock se realmente houve mudança visível
        if (newState != s && level instanceof Level lvl) {
            lvl.setBlock(pos, newState, 3);
        }
    }

    /** Conecta se: (a) face não está DISABLED no nosso cabo E
     *  (b) vizinho é cabo OU expõe FE pela face oposta. */
    private boolean shouldConnect(LevelAccessor level, BlockPos pos, Direction dir) {
        // Verifica se NOSSO cabo desligou aquela face
        BlockEntity ourBe = level.getBlockEntity(pos);
        if (ourBe instanceof EnergyCableBlockEntity cable
                && cable.getMode(dir) == EnergyCableBlockEntity.FaceMode.DISABLED) {
            return false;
        }
        BlockPos npos = pos.relative(dir);
        if (level.getBlockState(npos).getBlock() instanceof EnergyCableBlock) {
            // Vizinho também precisa não ter aquela face dele desligada (a face oposta)
            BlockEntity nbe = level.getBlockEntity(npos);
            if (nbe instanceof EnergyCableBlockEntity ncable
                    && ncable.getMode(dir.getOpposite()) == EnergyCableBlockEntity.FaceMode.DISABLED) {
                return false;
            }
            return true;
        }
        BlockEntity nbe = level.getBlockEntity(npos);
        if (nbe == null) return false;
        return nbe.getCapability(ForgeCapabilities.ENERGY, dir.getOpposite()).isPresent();
    }

    @Override
    public void onRemove(BlockState s, Level level, BlockPos pos, BlockState ns, boolean moved) {
        if (!s.is(ns.getBlock()) && !level.isClientSide) {
            // Força vizinhos a invalidarem capabilities cacheadas
            level.updateNeighborsAt(pos, this);
        }
        super.onRemove(s, level, pos, ns, moved);
    }

    @Nullable @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EnergyCableBlockEntity(pos, state);
    }

    @Nullable @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.ENERGY_CABLE.get(),
                EnergyCableBlockEntity::tick);
    }
}
