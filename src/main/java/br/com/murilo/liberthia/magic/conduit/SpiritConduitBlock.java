package br.com.murilo.liberthia.magic.conduit;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * v0.1.149 r117: <b>SpiritConduitBlock</b> — visualmente pilar de 1x2x1.
 * Coloca no Spirit World pra acumular charge. Right-click mostra charge atual.
 */
public class SpiritConduitBlock extends Block implements EntityBlock {

    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(4, 0, 4, 12, 14, 12),     // pilar central
            Block.box(2, 14, 2, 14, 16, 14));   // cap top

    public SpiritConduitBlock(Properties props) {
        super(props);
    }

    @Override public RenderShape getRenderShape(BlockState s) { return RenderShape.MODEL; }
    @Override public VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return SHAPE; }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SpiritConduitBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return (l, p, s, be) -> {
            if (be instanceof SpiritConduitBlockEntity conduit) {
                SpiritConduitBlockEntity.serverTick(l, p, s, conduit);
            }
        };
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                  Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof SpiritConduitBlockEntity conduit) {
            int charge = conduit.getCharge();
            int max = SpiritConduitBlockEntity.MAX_CHARGE;
            int pct = charge * 100 / max;
            player.displayClientMessage(Component.literal(
                    "§5✦ Spirit Conduit: §d" + charge + "§7/§d" + max + " §7(" + pct + "%)"), true);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }
}
