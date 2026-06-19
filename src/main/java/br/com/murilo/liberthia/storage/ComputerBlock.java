package br.com.murilo.liberthia.storage;

import br.com.murilo.liberthia.item.computer.ComputerData;
import br.com.murilo.liberthia.network.ModNetwork;
import br.com.murilo.liberthia.network.packet.OpenComputerBlockS2CPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * Bloco Computador. Clique-direito abre a tela de dados (via packet S2C).
 * Se o login estiver ligado e a senha definida, abre primeiro a tela de senha.
 */
public class ComputerBlock extends Block implements EntityBlock {

    public ComputerBlock(Properties props) {
        super(props);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ComputerBlockEntity(pos, state);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer sp
                && level.getBlockEntity(pos) instanceof ComputerBlockEntity be) {
            // Precisa de energia pra ligar. Sem energia, o computador fica "morto".
            if (!be.useEnergy(ComputerBlockEntity.OPEN_COST)) {
                sp.displayClientMessage(Component.literal(
                        "§c⚠ Computador sem energia. Conecte um cabo/bateria de energia."), true);
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
            boolean locked = be.isLocked(sp);
            boolean owner = be.isOwner(sp);
            CompoundTag wrap = locked ? new CompoundTag() : ComputerData.wrap(be.getFiles());
            ModNetwork.sendToPlayer(sp, new OpenComputerBlockS2CPacket(pos, locked, owner,
                    be.isLoginEnabled(), be.getEnergyStored(), be.getMaxEnergy(), wrap));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state,
                            @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide && placer instanceof Player p
                && level.getBlockEntity(pos) instanceof ComputerBlockEntity be) {
            be.setOwner(p.getUUID());
        }
    }

    /** Ao quebrar o computador, devolve o HD (que agora guarda TODOS os relatórios). */
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())
                && level.getBlockEntity(pos) instanceof ComputerBlockEntity be) {
            ItemStack hd = be.getHd();
            if (!hd.isEmpty()) {
                net.minecraft.world.Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), hd);
                be.setHd(ItemStack.EMPTY);
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}
