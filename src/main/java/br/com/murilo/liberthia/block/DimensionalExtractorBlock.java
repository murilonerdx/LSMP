package br.com.murilo.liberthia.block;

import br.com.murilo.liberthia.block.entity.DimensionalExtractorBlockEntity;
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
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

public class DimensionalExtractorBlock extends BaseEntityBlock {
    public DimensionalExtractorBlock(Properties p) { super(p); }
    @Override public RenderShape getRenderShape(BlockState s) { return RenderShape.MODEL; }

    @Override
    public InteractionResult use(BlockState s, Level level, BlockPos pos, Player player, InteractionHand h, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof MenuProvider mp && player instanceof net.minecraft.server.level.ServerPlayer sp) {
            NetworkHooks.openScreen(sp, mp, pos);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void onRemove(BlockState s, Level level, BlockPos pos, BlockState ns, boolean moved) {
        if (!s.is(ns.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof DimensionalExtractorBlockEntity ext) ext.drops();
            super.onRemove(s, level, pos, ns, moved);
        }
    }

    @Nullable @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState s) {
        return new DimensionalExtractorBlockEntity(pos, s);
    }
    @Nullable @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState s, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.DIMENSIONAL_EXTRACTOR.get(),
                DimensionalExtractorBlockEntity::tick);
    }
}
