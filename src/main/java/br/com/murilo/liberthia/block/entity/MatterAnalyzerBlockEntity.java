package br.com.murilo.liberthia.block.entity;

import br.com.murilo.liberthia.matter.MatterContent;
import br.com.murilo.liberthia.matter.MatterContentRegistry;
import br.com.murilo.liberthia.menu.MatterAnalyzerMenu;
import br.com.murilo.liberthia.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Matter Analyzer — "computador" da lore. Você coloca um item/sample no slot
 * e ele analisa: mostra DM/WM/YM, energia equivalente e mutação composta.
 *
 * <p>Não consome o item. É só leitura.
 */
public class MatterAnalyzerBlockEntity extends BlockEntity implements MenuProvider {

    public static final int SLOT_INPUT = 0;

    private final ItemStackHandler inventory = new ItemStackHandler(1) {
        @Override protected void onContentsChanged(int slot) {
            setChanged();
            if (level != null && !level.isClientSide())
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
        @Override public int getSlotLimit(int slot) { return 1; }
    };
    private LazyOptional<IItemHandler> lazy = LazyOptional.empty();

    public MatterAnalyzerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MATTER_ANALYZER.get(), pos, state);
    }

    public IItemHandler getInventory() { return inventory; }

    public ItemStack getInputStack() { return inventory.getStackInSlot(SLOT_INPUT); }

    public MatterContent currentContent() {
        ItemStack stack = getInputStack();
        // Sample Vial preenchido tem leitura própria
        if (stack.getItem() instanceof br.com.murilo.liberthia.item.SampleVialItem) {
            MatterContent vial = br.com.murilo.liberthia.item.SampleVialItem.contentOf(stack);
            if (vial.total() > 0) return vial;
        }
        return MatterContentRegistry.of(stack);
    }

    @Override public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) return lazy.cast();
        return super.getCapability(cap, side);
    }
    @Override public void onLoad() { super.onLoad(); lazy = LazyOptional.of(() -> inventory); }
    @Override public void invalidateCaps() { super.invalidateCaps(); lazy.invalidate(); }

    public void drops() {
        SimpleContainer c = new SimpleContainer(inventory.getSlots());
        for (int i = 0; i < inventory.getSlots(); i++) c.setItem(i, inventory.getStackInSlot(i));
        Containers.dropContents(level, worldPosition, c);
    }

    @Override public @NotNull Component getDisplayName() {
        return Component.translatable("container.liberthia.matter_analyzer");
    }
    @Override public @Nullable AbstractContainerMenu createMenu(int id, @NotNull Inventory inv, @NotNull Player p) {
        return new MatterAnalyzerMenu(id, inv, this);
    }

    @Override protected void saveAdditional(CompoundTag tag) {
        tag.put("inv", inventory.serializeNBT());
        super.saveAdditional(tag);
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("inv")) inventory.deserializeNBT(tag.getCompound("inv"));
    }

    @Nullable @Override
    public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }
    @Override public CompoundTag getUpdateTag() { return saveWithoutMetadata(); }
}
