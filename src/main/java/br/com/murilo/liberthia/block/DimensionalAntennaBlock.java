package br.com.murilo.liberthia.block;

import br.com.murilo.liberthia.block.entity.DimensionalAntennaBlockEntity;
import br.com.murilo.liberthia.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

/**
 * v0.1.22 r28: Dimensional Antenna com FACING horizontal — antena DIRECIONAL.
 * Shift+rclick mao vazia = gira 90 graus.
 */
public class DimensionalAntennaBlock extends BaseEntityBlock {

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    public DimensionalAntennaBlock(Properties props) {
        super(props);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockState rotate(BlockState state, net.minecraft.world.level.block.Rotation rot) {
        return state.setValue(FACING, rot.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, net.minecraft.world.level.block.Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DimensionalAntennaBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                   BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return createTickerHelper(type, ModBlockEntities.DIMENSIONAL_ANTENNA.get(),
                DimensionalAntennaBlockEntity::serverTick);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                  InteractionHand hand, BlockHitResult hit) {
        if (player.isShiftKeyDown() && player.getItemInHand(hand).isEmpty()) {
            if (level.isClientSide) return InteractionResult.SUCCESS;
            Direction current = state.getValue(FACING);
            Direction next = current.getClockWise();
            level.setBlock(pos, state.setValue(FACING, next), Block.UPDATE_ALL);
            level.playSound(null, pos, SoundEvents.LEVER_CLICK,
                    SoundSource.BLOCKS, 0.6F, 1.2F);
            player.displayClientMessage(Component.literal(
                    "ROT: " + next.getName().toUpperCase()), true);
            return InteractionResult.CONSUME;
        }
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer sp)) return InteractionResult.PASS;
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof DimensionalAntennaBlockEntity antenna)) return InteractionResult.PASS;
        NetworkHooks.openScreen(sp, antenna, buf -> buf.writeBlockPos(pos));
        return InteractionResult.CONSUME;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof DimensionalAntennaBlockEntity antenna) {
                antenna.onBroken();
                if (level instanceof net.minecraft.server.level.ServerLevel sl) {
                    var inv = antenna.getInventory();
                    for (int i = 0; i < inv.getSlots(); i++) {
                        net.minecraft.world.item.ItemStack stack = inv.getStackInSlot(i);
                        if (!stack.isEmpty()) Block.popResource(sl, pos, stack);
                    }
                }
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}
