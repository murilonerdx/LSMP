package br.com.murilo.liberthia.magic.glyph.inscriber;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.network.NetworkHooks;

/**
 * v0.1.150 r118: <b>GlyphInscriberBlock</b> — bloco com GUI nativo de crafting.
 *
 * <p>Right-click (sem sneak) → abre menu. Sneak+right-click ainda funciona via
 * ScribesTable como fallback legacy.
 */
public class GlyphInscriberBlock extends Block implements EntityBlock {

    // Mesa baixa com placa de glyph em cima
    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(2, 0, 2, 14, 12, 14),
            Block.box(0, 12, 0, 16, 14, 16));

    public GlyphInscriberBlock(Properties props) {
        super(props);
    }

    @Override public VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return SHAPE; }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GlyphInscriberBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return (l, p, s, be) -> {
            if (be instanceof GlyphInscriberBlockEntity gbe) {
                GlyphInscriberBlockEntity.serverTick(l, p, s, gbe);
            }
        };
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                  Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof GlyphInscriberBlockEntity gbe && player instanceof ServerPlayer sp) {
            NetworkHooks.openScreen(sp, gbe, buf -> buf.writeBlockPos(pos));
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof GlyphInscriberBlockEntity gbe) gbe.drops();
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}
