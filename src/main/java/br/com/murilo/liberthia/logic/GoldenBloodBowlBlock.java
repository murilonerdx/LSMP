package br.com.murilo.liberthia.logic;

import br.com.murilo.liberthia.block.entity.GoldenBloodBowlBlockEntity;
import br.com.murilo.liberthia.registry.ModBlockEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Central golden bowl. Right-click with a ritual activator to start.
 */
public class GoldenBloodBowlBlock extends Block implements EntityBlock {

    private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 10, 14);

    public GoldenBloodBowlBlock(Properties props) {
        super(props);
    }

    @Override
    public VoxelShape getShape(BlockState s, BlockGetter g, BlockPos p, CollisionContext c) {
        return SHAPE;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                 Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer sp)) return InteractionResult.PASS;

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof GoldenBloodBowlBlockEntity ritual)) return InteractionResult.PASS;

        // Sneak + empty hand → interrupt active ritual.
        if (sp.isShiftKeyDown() && player.getItemInHand(hand).isEmpty()) {
            if (ritual.isActive()) {
                ritual.interrupt((ServerLevel) level, "cancelado pelo jogador");
                return InteractionResult.CONSUME;
            }
            return InteractionResult.PASS;
        }

        if (ritual.isActive()) {
            sp.displayClientMessage(
                    Component.literal("Já há um ritual em andamento.")
                            .withStyle(ChatFormatting.RED), true);
            return InteractionResult.CONSUME;
        }

        ItemStack inHand = player.getItemInHand(hand);
        if (inHand.isEmpty()) return InteractionResult.PASS;

        boolean started = ritual.tryStart((ServerLevel) level, inHand.getItem(), sp);
        if (!started) {
            sp.displayClientMessage(
                    Component.literal("Ritual inválido — verifique pentacle e ingredientes.")
                            .withStyle(ChatFormatting.RED), true);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.CONSUME;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GoldenBloodBowlBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,
                                                                  BlockState state,
                                                                  BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return type == ModBlockEntities.GOLDEN_BLOOD_BOWL.get()
                ? (lvl, p, s, be) -> {
                    if (be instanceof GoldenBloodBowlBlockEntity ritual) {
                        ritual.serverTick((ServerLevel) lvl);
                    }
                }
                : null;
    }
}
