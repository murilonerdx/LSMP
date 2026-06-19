package br.com.murilo.liberthia.storage;

import br.com.murilo.liberthia.menu.PrinterMenu;
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
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import br.com.murilo.liberthia.energy.TrackedEnergyStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * <b>Impressora</b> — BlockEntity com 1 slot de papel. Coloque ao lado de um
 * Computador; a GUI lê os relatórios do computador vizinho como "pendências" e
 * imprime o selecionado num livro escrito (consome 1 papel do slot).
 */
public class PrinterBlockEntity extends BlockEntity implements MenuProvider {

    public static final int SLOT_PAPER = 0;

    private final ItemStackHandler inventory = new ItemStackHandler(1) {
        @Override public boolean isItemValid(int slot, @NotNull ItemStack stack) { return stack.is(Items.PAPER); }
        @Override protected void onContentsChanged(int slot) {
            setChanged();
            if (level != null && !level.isClientSide())
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    };
    private LazyOptional<IItemHandler> lazy = LazyOptional.empty();

    // ── Energia (FE) ─────────────────────────────────────────────────────────────
    // A impressora precisa de energia pra imprimir. Recebe FE por qualquer lado.
    public static final int ENERGY_CAPACITY = 30_000;
    /** FE consumido por impressão (1 livro). */
    public static final int PRINT_COST = 800;

    private final TrackedEnergyStorage energy = new TrackedEnergyStorage(this, ENERGY_CAPACITY, 1024, 0);
    private final LazyOptional<IEnergyStorage> lazyEnergy = LazyOptional.of(() -> energy);

    public PrinterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PRINTER.get(), pos, state);
    }

    public IItemHandler getInventory() { return inventory; }
    public ItemStack getPaper() { return inventory.getStackInSlot(SLOT_PAPER); }

    // ── Energia ──────────────────────────────────────────────────────────────────
    public int getEnergyStored() { return energy.getEnergyStored(); }
    public int getMaxEnergy() { return energy.getMaxEnergyStored(); }
    public boolean hasEnergy(int amount) { return energy.getEnergyStored() >= amount; }
    /** Consome {@code amount} FE se houver. true se consumiu. */
    public boolean useEnergy(int amount) {
        if (energy.getEnergyStored() < amount) return false;
        energy.setStored(energy.getEnergyStored() - amount);
        return true;
    }

    /** Acha o Computador adjacente (6 vizinhos). */
    @Nullable
    public ComputerBlockEntity findComputer() {
        if (level == null) return null;
        for (Direction d : Direction.values()) {
            BlockEntity be = level.getBlockEntity(worldPosition.relative(d));
            if (be instanceof ComputerBlockEntity c) return c;
        }
        return null;
    }

    @Override public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) return lazy.cast();
        if (cap == ForgeCapabilities.ENERGY) return lazyEnergy.cast();
        return super.getCapability(cap, side);
    }
    @Override public void onLoad() { super.onLoad(); lazy = LazyOptional.of(() -> inventory); }
    @Override public void invalidateCaps() { super.invalidateCaps(); lazy.invalidate(); lazyEnergy.invalidate(); }

    public void drops() {
        SimpleContainer c = new SimpleContainer(inventory.getSlots());
        for (int i = 0; i < inventory.getSlots(); i++) c.setItem(i, inventory.getStackInSlot(i));
        Containers.dropContents(level, worldPosition, c);
    }

    @Override public @NotNull Component getDisplayName() {
        return Component.translatable("container.liberthia.printer");
    }
    @Override public @Nullable AbstractContainerMenu createMenu(int id, @NotNull Inventory inv, @NotNull Player p) {
        return new PrinterMenu(id, inv, this);
    }

    @Override protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("inv", inventory.serializeNBT());
        tag.putInt("Energy", energy.getEnergyStored());
    }
    @Override public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        if (tag.contains("inv")) inventory.deserializeNBT(tag.getCompound("inv"));
        energy.setStored(tag.getInt("Energy"));
    }

    @Nullable @Override
    public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }
    @Override public CompoundTag getUpdateTag() { return saveWithoutMetadata(); }
}
