package br.com.murilo.liberthia.block;

import br.com.murilo.liberthia.block.entity.DimensionalChestBlockEntity;
import br.com.murilo.liberthia.network.ModNetwork;
import br.com.murilo.liberthia.network.packet.OpenDimensionalChannelScreenS2CPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * Baú Dimensional — bloco que se conecta a outros baús dimensionais por canal
 * (string ID). Mesma string = mesmo inventário, cross-dimension.
 *
 * <ul>
 *   <li>Right-click normal: abre o baú (54 slots)</li>
 *   <li>Sneak + right-click: abre tela pra configurar o canal</li>
 * </ul>
 */
public class DimensionalChestBlock extends BaseEntityBlock {

    public DimensionalChestBlock(Properties props) { super(props); }

    @Override public RenderShape getRenderShape(BlockState s) { return RenderShape.MODEL; }

    @Nullable @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DimensionalChestBlockEntity(pos, state);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof DimensionalChestBlockEntity be))
            return InteractionResult.PASS;

        if (player.isShiftKeyDown()) {
            // Abre tela de configurar canal
            if (player instanceof ServerPlayer sp) {
                ModNetwork.sendToPlayer(sp,
                        new OpenDimensionalChannelScreenS2CPacket(pos, be.getChannel()));
            }
            return InteractionResult.CONSUME;
        }

        // Abre o baú normalmente
        if (player instanceof ServerPlayer sp) {
            net.minecraftforge.network.NetworkHooks.openScreen(sp, be, buf -> buf.writeBlockPos(pos));
        }
        player.displayClientMessage(
                Component.literal("§9⌬ Canal: §f" + be.getChannel()), true);
        return InteractionResult.CONSUME;
    }
}
