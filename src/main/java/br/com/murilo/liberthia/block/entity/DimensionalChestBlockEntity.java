package br.com.murilo.liberthia.block.entity;

import br.com.murilo.liberthia.menu.DimensionalChestMenu;
import br.com.murilo.liberthia.persistence.DimensionalStorage;
import br.com.murilo.liberthia.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Baú Dimensional — armazena um "canal" (string) que aponta pra um inventário
 * compartilhado em {@link DimensionalStorage}. Múltiplos baús com o mesmo
 * canal compartilham o mesmo inventário, cross-dimension.
 *
 * <p>Expõe {@code ITEM_HANDLER} capability — pipes/hoppers funcionam.
 */
public class DimensionalChestBlockEntity extends BlockEntity implements MenuProvider {

    public static final String DEFAULT_CHANNEL = "default";

    private String channel = DEFAULT_CHANNEL;
    private LazyOptional<IItemHandler> lazyItem = LazyOptional.empty();

    public DimensionalChestBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DIMENSIONAL_CHEST.get(), pos, state);
    }

    public String getChannel() { return channel; }

    public void setChannel(String newChannel) {
        if (newChannel == null) newChannel = DEFAULT_CHANNEL;
        newChannel = newChannel.trim();
        if (newChannel.isEmpty()) newChannel = DEFAULT_CHANNEL;
        if (newChannel.length() > 32) newChannel = newChannel.substring(0, 32);
        if (!newChannel.equals(channel)) {
            channel = newChannel;
            // Invalida o lazy pra próxima query reapontar pro novo canal
            invalidateCaps();
            lazyItem = LazyOptional.empty();
            setChanged();
            if (level != null && !level.isClientSide()) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }
    }

    public IItemHandler getInventoryHandler() {
        if (level == null || level.getServer() == null) {
            // Cliente: retorna handler dummy vazio
            return new net.minecraftforge.items.ItemStackHandler(DimensionalStorage.CHANNEL_SIZE);
        }
        return DimensionalStorage.get(level).getOrCreateChannel(channel);
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            if (!lazyItem.isPresent()) {
                lazyItem = LazyOptional.of(this::getInventoryHandler);
            }
            return lazyItem.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override public void invalidateCaps() {
        super.invalidateCaps();
        lazyItem.invalidate();
    }

    @Override public @NotNull Component getDisplayName() {
        return Component.translatable("container.liberthia.dimensional_chest")
                .append(Component.literal(" §7[" + channel + "]"));
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, @NotNull Inventory inv, @NotNull Player p) {
        return new DimensionalChestMenu(id, inv, this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        tag.putString("channel", channel);
        super.saveAdditional(tag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("channel")) channel = tag.getString("channel");
        if (channel == null || channel.isEmpty()) channel = DEFAULT_CHANNEL;
    }

    @Nullable @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
    @Override public CompoundTag getUpdateTag() { return saveWithoutMetadata(); }
}
