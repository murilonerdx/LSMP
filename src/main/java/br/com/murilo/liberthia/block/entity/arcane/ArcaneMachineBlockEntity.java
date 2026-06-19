package br.com.murilo.liberthia.block.entity.arcane;

import br.com.murilo.liberthia.block.ArcaneMachineBlock;
import br.com.murilo.liberthia.energy.TrackedEnergyStorage;
import br.com.murilo.liberthia.menu.ArcaneMachineMenu;
import br.com.murilo.liberthia.registry.ModTech;
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
 * r184 — máquina genérica Tecno/Tecno-Arcano, configurada por {@link ArcaneMachineType}.
 * Recebe FE da rede, processa receitas posicionais ({@link ArcaneRecipeRegistry}) e produz
 * nos slots de saída. Suporta cadeias multi-etapa (saída de uma = entrada de outra).
 */
public class ArcaneMachineBlockEntity extends BlockEntity implements MenuProvider {
    private static final int BUFFER = 100_000, RECEIVE = 20_000;

    public final ArcaneMachineType type;
    private final ItemStackHandler inventory;
    private LazyOptional<IItemHandler> lazyItems = LazyOptional.empty();
    private final TrackedEnergyStorage energy = new TrackedEnergyStorage(this, BUFFER, RECEIVE, 0);
    private LazyOptional<IEnergyStorage> lazyEnergy = LazyOptional.empty();
    private int progress = 0, currentMax = 100;

    public ArcaneMachineBlockEntity(BlockPos pos, BlockState state) {
        super(ModTech.ARCANE_MACHINE_BE.get(), pos, state);
        this.type = (state.getBlock() instanceof ArcaneMachineBlock b) ? b.type : ArcaneMachineType.METAL_PRESS;
        this.currentMax = type.processTime;
        this.inventory = new ItemStackHandler(Math.max(1, type.totalSlots())) {
            @Override protected void onContentsChanged(int slot) { setChanged(); }
            @Override public boolean isItemValid(int slot, @NotNull ItemStack s) { return slot < type.inputSlots; }
        };
    }

    public final ContainerData data = new ContainerData() {
        @Override public int get(int i) {
            int e = energy.getEnergyStored(), m = energy.getMaxEnergyStored();
            return switch (i) {
                case 0 -> e & 0xFFFF; case 1 -> (e >>> 16) & 0xFFFF;
                case 2 -> m & 0xFFFF; case 3 -> (m >>> 16) & 0xFFFF;
                case 4 -> progress; case 5 -> Math.max(1, currentMax);
                default -> 0;
            };
        }
        @Override public void set(int i, int v) {}
        @Override public int getCount() { return 6; }
    };

    public IItemHandler getInventory() { return inventory; }

    public static void tick(Level lvl, BlockPos pos, BlockState st, ArcaneMachineBlockEntity be) {
        if (lvl.isClientSide) return;
        ArcaneRecipe r = ArcaneRecipeRegistry.find(be.type, be.inventory);
        if (r == null) { if (be.progress != 0) { be.progress = 0; be.setChanged(); } return; }
        be.currentMax = r.processTime() > 0 ? r.processTime() : be.type.processTime;
        if (be.type.needsEnergy && be.energy.getEnergyStored() < be.type.fePerTick) return;
        if (be.type.needsEnergy) be.energy.setStored(be.energy.getEnergyStored() - be.type.fePerTick);
        be.progress++;
        if (be.progress >= be.currentMax) {
            be.progress = 0;
            for (int i = 0; i < r.inputs().size(); i++) be.inventory.extractItem(i, r.inputs().get(i).getCount(), false);
            for (int j = 0; j < r.outputs().size(); j++) {
                int slot = be.type.inputSlots + j;
                ItemStack cur = be.inventory.getStackInSlot(slot).copy();
                ItemStack out = r.outputs().get(j);
                if (cur.isEmpty()) cur = out.copy(); else cur.grow(out.getCount());
                be.inventory.setStackInSlot(slot, cur);
            }
        }
        be.setChanged();
    }

    @Override public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) return lazyItems.cast();
        if (cap == ForgeCapabilities.ENERGY) return lazyEnergy.cast();
        return super.getCapability(cap, side);
    }
    @Override public void onLoad() { super.onLoad(); lazyItems = LazyOptional.of(() -> inventory); lazyEnergy = LazyOptional.of(() -> energy); }
    @Override public void invalidateCaps() { super.invalidateCaps(); lazyItems.invalidate(); lazyEnergy.invalidate(); }

    public void drops() {
        SimpleContainer c = new SimpleContainer(inventory.getSlots());
        for (int i = 0; i < inventory.getSlots(); i++) c.setItem(i, inventory.getStackInSlot(i));
        Containers.dropContents(level, worldPosition, c);
    }

    @Override public @NotNull Component getDisplayName() { return getBlockState().getBlock().getName(); }
    @Override public @Nullable AbstractContainerMenu createMenu(int id, @NotNull Inventory inv, @NotNull Player p) {
        return new ArcaneMachineMenu(id, inv, this, this.data);
    }

    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("inv", inventory.serializeNBT());
        tag.putInt("progress", progress);
        tag.putInt("Energy", energy.getEnergyStored());
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("inv")) inventory.deserializeNBT(tag.getCompound("inv"));
        progress = tag.getInt("progress");
        energy.setStored(tag.getInt("Energy"));
    }
}
