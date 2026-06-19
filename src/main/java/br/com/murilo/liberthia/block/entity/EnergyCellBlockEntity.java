package br.com.murilo.liberthia.block.entity;

import br.com.murilo.liberthia.energy.EnergyNetwork;
import br.com.murilo.liberthia.energy.TrackedEnergyStorage;
import br.com.murilo.liberthia.registry.ModTech;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * r180c/r182 — <b>Célula de Energia</b> (bateria FE, 3 tiers). Recebe FE de geradores,
 * armazena, empurra pra consumidores pela {@link EnergyNetwork}, E carrega o item posto
 * no slot da GUI ({@link ITechEnergyUI} layout CHARGER). Capacidade/transfer por tier.
 */
public class EnergyCellBlockEntity extends BlockEntity implements MenuProvider, ITechEnergyUI {

    private final int transfer;
    private final TrackedEnergyStorage store;
    private LazyOptional<IEnergyStorage> lazy;

    private final ItemStackHandler charge = new ItemStackHandler(1) {
        @Override protected void onContentsChanged(int slot) { setChanged(); }
        @Override public boolean isItemValid(int slot, @NotNull ItemStack s) {
            return s.getCapability(ForgeCapabilities.ENERGY).map(IEnergyStorage::canReceive).orElse(false);
        }
    };
    private LazyOptional<IItemHandler> lazyItems;
    private final TechEnergyData uiData;

    public EnergyCellBlockEntity(BlockPos pos, BlockState state) {
        super(ModTech.ENERGY_CELL_BE.get(), pos, state);
        int cap, tr;
        var b = state.getBlock();
        if (b == ModTech.ENERGY_CELL_ULTIMATE.get())      { cap = 16_000_000; tr = 80_000; }
        else if (b == ModTech.ENERGY_CELL_ADVANCED.get()) { cap = 4_000_000;  tr = 20_000; }
        else                                              { cap = 1_000_000;  tr = 5_000;  }
        this.transfer = tr;
        this.store = new TrackedEnergyStorage(this, cap, tr, tr);
        this.lazy = LazyOptional.of(() -> store);
        this.lazyItems = LazyOptional.of(() -> charge);
        this.uiData = new TechEnergyData(store::getEnergyStored, store::getMaxEnergyStored, () -> 0, () -> 0);
    }

    public static void tick(Level lvl, BlockPos pos, BlockState st, EnergyCellBlockEntity be) {
        if (lvl.isClientSide) return;
        // carregar item no slot da GUI
        ItemStack s = be.charge.getStackInSlot(0);
        if (!s.isEmpty() && be.store.getEnergyStored() > 0) {
            int avail = Math.min(be.transfer, be.store.getEnergyStored());
            int moved = s.getCapability(ForgeCapabilities.ENERGY)
                    .map(e -> e.canReceive() ? e.receiveEnergy(avail, false) : 0).orElse(0);
            if (moved > 0) be.store.extractEnergy(moved, false);
        }
        // empurrar pra rede
        int stored = be.store.getEnergyStored();
        if (stored > 0) {
            int sent = EnergyNetwork.pushThroughNetwork(lvl, pos, Math.min(be.transfer, stored));
            if (sent > 0) be.store.extractEnergy(sent, false);
        }
    }

    public int getEnergyStored() { return store.getEnergyStored(); }
    public int getMaxEnergy()    { return store.getMaxEnergyStored(); }

    // ── GUI de energia (ITechEnergyUI + MenuProvider) ──
    @Override public ContainerData getEnergyData() { return uiData; }
    @Override public IItemHandler getMenuSlots() { return charge; }
    @Override public int getLayoutId() { return LAYOUT_CHARGER; }
    @Override public Component getDisplayName() { return getBlockState().getBlock().getName(); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inv, Player p) {
        return new br.com.murilo.liberthia.menu.TechEnergyMenu(id, inv, this, uiData);
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ENERGY) return lazy.cast();
        if (cap == ForgeCapabilities.ITEM_HANDLER) return lazyItems.cast();
        return super.getCapability(cap, side);
    }

    @Override public void invalidateCaps() { super.invalidateCaps(); lazy.invalidate(); lazyItems.invalidate(); }

    public void drops() {
        if (level != null) net.minecraft.world.Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), charge.getStackInSlot(0));
    }

    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("Energy", store.getEnergyStored());
        tag.put("charge", charge.serializeNBT());
    }

    @Override public void load(CompoundTag tag) {
        super.load(tag);
        store.setStored(tag.getInt("Energy"));
        if (tag.contains("charge")) charge.deserializeNBT(tag.getCompound("charge"));
    }
}
