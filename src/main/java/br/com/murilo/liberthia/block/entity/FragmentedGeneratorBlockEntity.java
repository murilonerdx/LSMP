package br.com.murilo.liberthia.block.entity;

import br.com.murilo.liberthia.menu.FragmentedGeneratorMenu;
import br.com.murilo.liberthia.registry.ModBlockEntities;
import br.com.murilo.liberthia.registry.ModBlocks;
import br.com.murilo.liberthia.registry.ModItems;
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
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Gerador de Matéria Escura Fragmentada — refinaria Estágio 1.
 *
 * <p>Slots:
 * <ul>
 *   <li>0 (FLUID) — bucket de matéria escura (consumido)</li>
 *   <li>1 (DIAMOND) — diamante (consumido)</li>
 *   <li>2 (OUTPUT) — saída (inactive_dark_matter)</li>
 *   <li>3 (UPGRADE) — speed_upgrade (até 4)</li>
 * </ul>
 *
 * <p>Uma operação consome 1 bucket de matéria escura + 1 diamante + 50.000 FE
 * ao longo de {@link #BASE_PROCESS_TICKS} ticks (200 ticks = 10s) e produz
 * 1 inactive_dark_matter. Cada speed upgrade reduz 25% do tempo (mín 50t).
 */
public class FragmentedGeneratorBlockEntity extends BlockEntity
        implements MenuProvider, br.com.murilo.liberthia.persistence.Persistable {

    public static final int SLOT_FLUID = 0;
    public static final int SLOT_CATALYST = 1;  // antes DIAMOND, agora NETHER_STAR
    public static final int SLOT_OUTPUT = 2;
    public static final int SLOT_UPGRADE = 3;
    /** Compat: alias antigo. */
    public static final int SLOT_DIAMOND = SLOT_CATALYST;

    public static final int BASE_PROCESS_TICKS = 200;
    public static final int FE_BUFFER = 100_000;
    public static final int FE_PER_TICK = 250;
    public static final int FE_PER_OPERATION = 50_000;

    private final ItemStackHandler inventory = new ItemStackHandler(4) {
        @Override protected void onContentsChanged(int slot) {
            setChanged();
            if (level instanceof net.minecraft.server.level.ServerLevel sl) {
                br.com.murilo.liberthia.persistence.LiberthiaPersistence.get(sl)
                        .snapshot(sl, worldPosition, saveWithFullMetadata());
            }
        }
        @Override public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return switch (slot) {
                case SLOT_FLUID -> stack.is(ModItems.DARK_MATTER_BUCKET.get());
                case SLOT_CATALYST -> stack.is(Items.NETHER_STAR);
                case SLOT_UPGRADE -> stack.is(ModItems.SPEED_UPGRADE.get());
                case SLOT_OUTPUT -> stack.is(ModItems.INACTIVE_DARK_MATTER.get());
                default -> false;
            };
        }
        @Override public int getSlotLimit(int slot) {
            return slot == SLOT_UPGRADE ? 4 : 64;
        }
    };

    private final br.com.murilo.liberthia.energy.TrackedEnergyStorage energy =
            new br.com.murilo.liberthia.energy.TrackedEnergyStorage(this, FE_BUFFER, FE_PER_TICK * 4, 0);
    private LazyOptional<IItemHandler> lazyItem = LazyOptional.empty();
    private LazyOptional<net.minecraftforge.energy.IEnergyStorage> lazyEnergy = LazyOptional.empty();

    private int progress = 0;
    private int feSpent = 0;

    public final ContainerData data = new ContainerData() {
        @Override public int get(int i) {
            int v = switch (i) {
                case 0, 1 -> energy.getEnergyStored();
                case 2, 3 -> energy.getMaxEnergyStored();
                case 4 -> progress;
                case 5 -> processTicksRequired();
                case 6 -> feSpent;
                default -> 0;
            };
            return switch (i) {
                case 0, 2 -> (v >> 16) & 0xFFFF;
                case 1, 3 -> v & 0xFFFF;
                default -> v;
            };
        }
        @Override public void set(int i, int v) {}
        @Override public int getCount() { return 7; }
    };

    public FragmentedGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FRAGMENTED_GENERATOR.get(), pos, state);
    }

    public IItemHandler getInventory() { return inventory; }
    public int processTicksRequired() {
        int speedCount = inventory.getStackInSlot(SLOT_UPGRADE).getCount();
        return Math.max(50, BASE_PROCESS_TICKS - speedCount * 50);
    }

    @Override public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) return lazyItem.cast();
        if (cap == ForgeCapabilities.ENERGY) return lazyEnergy.cast();
        return super.getCapability(cap, side);
    }
    @Override public void onLoad() {
        super.onLoad();
        lazyItem = LazyOptional.of(() -> inventory);
        lazyEnergy = LazyOptional.of(() -> energy);
        br.com.murilo.liberthia.persistence.Persistable.LIVE.add(this);
        if (level instanceof net.minecraft.server.level.ServerLevel sl && isStateEmpty()) {
            CompoundTag snap = br.com.murilo.liberthia.persistence.LiberthiaPersistence
                    .get(sl).getSnapshot(sl, worldPosition);
            if (snap != null) restoreFromSnapshot(snap);
        }
    }

    @Override public boolean isStateEmpty() {
        if (energy.getEnergyStored() > 0) return false;
        if (progress > 0) return false;
        for (int i = 0; i < inventory.getSlots(); i++) {
            if (!inventory.getStackInSlot(i).isEmpty()) return false;
        }
        return true;
    }
    @Override public void restoreFromSnapshot(CompoundTag tag) { load(tag); setChanged(); }
    @Override public void invalidateCaps() {
        super.invalidateCaps();
        lazyItem.invalidate();
        lazyEnergy.invalidate();
    }
    @Override public void setRemoved() {
        super.setRemoved();
        br.com.murilo.liberthia.persistence.Persistable.LIVE.remove(this);
    }

    public void drops() {
        SimpleContainer c = new SimpleContainer(inventory.getSlots());
        for (int i = 0; i < inventory.getSlots(); i++) c.setItem(i, inventory.getStackInSlot(i));
        Containers.dropContents(level, worldPosition, c);
    }

    @Override public @NotNull Component getDisplayName() {
        return Component.translatable("container.liberthia.fragmented_generator");
    }
    @Override public @Nullable AbstractContainerMenu createMenu(int id, @NotNull Inventory inv, @NotNull Player p) {
        return new FragmentedGeneratorMenu(id, inv, this, this.data);
    }

    private static void snapshotIfDue(Level level, BlockPos pos, FragmentedGeneratorBlockEntity be) {
        if (level instanceof net.minecraft.server.level.ServerLevel sl
                && level.getGameTime() % br.com.murilo.liberthia.persistence.PersistenceHandler.SNAPSHOT_PERIOD == 0) {
            br.com.murilo.liberthia.persistence.LiberthiaPersistence.get(sl)
                    .snapshot(sl, pos, be.saveWithFullMetadata());
        }
    }

    public static void tick(Level level, BlockPos pos, BlockState state, FragmentedGeneratorBlockEntity be) {
        snapshotIfDue(level, pos, be);
        if (level.isClientSide) return;
        if (!canProcess(be)) {
            be.progress = 0;
            be.feSpent = 0;
            return;
        }
        // FE drain por tick
        int drain = Math.min(FE_PER_TICK, be.energy.getEnergyStored());
        if (drain <= 0) return;
        be.energy.extractEnergy(drain, false);
        be.feSpent += drain;
        be.progress++;

        if (be.progress >= be.processTicksRequired() && be.feSpent >= FE_PER_OPERATION) {
            // Consome inputs
            be.inventory.extractItem(SLOT_FLUID, 1, false);
            be.inventory.extractItem(SLOT_CATALYST, 1, false);
            // Output: inactive_dark_matter com purity 0 (base)
            ItemStack out = new ItemStack(ModItems.INACTIVE_DARK_MATTER.get());
            br.com.murilo.liberthia.util.Purity.setPurity(out, 0);
            be.inventory.insertItem(SLOT_OUTPUT, out, false);
            be.progress = 0;
            be.feSpent = 0;
        }
        be.setChanged();
    }

    private static boolean canProcess(FragmentedGeneratorBlockEntity be) {
        ItemStack fluid = be.inventory.getStackInSlot(SLOT_FLUID);
        ItemStack catalyst = be.inventory.getStackInSlot(SLOT_CATALYST);
        ItemStack output = be.inventory.getStackInSlot(SLOT_OUTPUT);
        if (fluid.isEmpty() || catalyst.isEmpty()) return false;
        if (!output.isEmpty() && (
                !output.is(ModItems.INACTIVE_DARK_MATTER.get())
                        || output.getCount() >= output.getMaxStackSize())) return false;
        return true;
    }

    @Override protected void saveAdditional(CompoundTag tag) {
        tag.put("inv", inventory.serializeNBT());
        tag.put("energy", energy.serializeNBT());
        tag.putInt("progress", progress);
        tag.putInt("feSpent", feSpent);
        super.saveAdditional(tag);
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("inv")) inventory.deserializeNBT(tag.getCompound("inv"));
        if (tag.contains("energy")) energy.deserializeNBT(tag.get("energy"));
        progress = tag.getInt("progress");
        feSpent = tag.getInt("feSpent");
    }
}
