package br.com.murilo.liberthia.block;

import br.com.murilo.liberthia.block.entity.MatterRefinerBlockEntity;
import br.com.murilo.liberthia.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class MatterRefinerBlock extends BaseEntityBlock {

    public MatterRefinerBlock(Properties props) { super(props); }

    @Override public RenderShape getRenderShape(BlockState s) { return RenderShape.MODEL; }

    @Nullable @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MatterRefinerBlockEntity(pos, state);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof MenuProvider mp && player instanceof net.minecraft.server.level.ServerPlayer sp) {
            net.minecraftforge.network.NetworkHooks.openScreen(sp, mp, pos);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    @Override
    public void onRemove(BlockState s, Level level, BlockPos pos, BlockState ns, boolean moved) {
        if (!s.is(ns.getBlock()) && level.getBlockEntity(pos) instanceof MatterRefinerBlockEntity be) {
            be.drops();
        }
        super.onRemove(s, level, pos, ns, moved);
    }

    @Nullable @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        // Ticker em cliente E servidor — server processa, client anima.
        return createTickerHelper(type, ModBlockEntities.MATTER_REFINER.get(),
                MatterRefinerBlockEntity::tick);
    }
}
