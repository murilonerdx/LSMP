package br.com.murilo.liberthia.logic;

import br.com.murilo.liberthia.block.entity.CommandPylonBlockEntity;
import br.com.murilo.liberthia.item.CommandRunner;
import br.com.murilo.liberthia.item.CommandTabletItem;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * Block that fires a saved command list:
 *   • on rising-edge redstone power, OR
 *   • on a configurable tick interval ({@code @loop <ticks>} as first line).
 *
 * Right-click with a Command Tablet to imprint the tablet's commands onto
 * the pylon. Right-click without a tablet (op-only) to manually fire.
 */
public class CommandPylonBlock extends Block implements EntityBlock {

    public CommandPylonBlock(Properties props) {
        super(props);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CommandPylonBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return type == ModBlockEntities.COMMAND_PYLON.get()
                ? (lvl, p, s, be) -> {
                    if (be instanceof CommandPylonBlockEntity pylon) {
                        pylon.serverTick((ServerLevel) lvl);
                    }
                }
                : null;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                 Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer sp)) return InteractionResult.PASS;
        if (!CommandRunner.isOp(sp)) {
            sp.displayClientMessage(
                    Component.literal("Apenas OPs podem operar o Pilar de Comando.")
                            .withStyle(ChatFormatting.RED), true);
            return InteractionResult.FAIL;
        }
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof CommandPylonBlockEntity pylon)) return InteractionResult.PASS;

        ItemStack held = player.getItemInHand(hand);
        if (held.getItem() instanceof CommandTabletItem) {
            pylon.imprintFromTablet(held);
            sp.displayClientMessage(
                    Component.literal("Pilar imprimido (" + pylon.commandCount()
                                    + " linhas, " + pylon.describeMode() + ").")
                            .withStyle(ChatFormatting.GREEN), true);
            return InteractionResult.CONSUME;
        }

        if (player.isShiftKeyDown()) {
            pylon.clearProgram();
            sp.displayClientMessage(
                    Component.literal("Pilar limpo.").withStyle(ChatFormatting.YELLOW), true);
            return InteractionResult.CONSUME;
        }

        pylon.fire((ServerLevel) level, sp);
        return InteractionResult.CONSUME;
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos,
                                Block neighbor, BlockPos fromPos, boolean moving) {
        if (level.isClientSide) return;
        boolean powered = level.hasNeighborSignal(pos);
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof CommandPylonBlockEntity pylon) {
            pylon.onRedstone((ServerLevel) level, powered);
        }
    }
}
