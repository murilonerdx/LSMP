package br.com.murilo.liberthia.block.entity;

import br.com.murilo.liberthia.menu.MatterPillBrewerMenu;
import br.com.murilo.liberthia.registry.ModBlockEntities;
import br.com.murilo.liberthia.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * v0.1.49: Matter Pill Brewer — fabrica pílulas alquímicas com ENERGIA (FE).
 *
 * <p>Slots:
 * <ul>
 *   <li>0 INGOT: purified_dark/clear/yellow_matter_ingot</li>
 *   <li>1 BOTTLE: glass_bottle</li>
 *   <li>2 OUTPUT: pílula gerada</li>
 * </ul>
 *
 * <p>Energia: capacity 50_000 FE, consumo 40 FE/tick durante o processo (60
 * ticks = 2400 FE por lote). Recebe energia por todos os 6 lados via
 * {@link IEnergyStorage} capability — conecta em energy cables / batteries.
 *
 * <p>Tick: se houver INGOT + BOTTLE + energia suficiente, processa 1 tick.
 * Quando atinge {@link #PROCESS_TICKS}, consome 1 ingot + 1 bottle e gera 3
 * pílulas no OUT. Se faltar energia ou input, pausa (não reseta progresso).
 */
public class MatterPillBrewerBlockEntity extends BlockEntity implements MenuProvider {

    public static final int SLOT_INGOT = 0;
    public static final int SLOT_BOTTLE = 1;
    public static final int SLOT_OUTPUT = 2;

    public static final int PROCESS_TICKS = 60; // 3s
    public static final int ENERGY_CAPACITY = 50_000;
    /** FE consumido por tick durante o processo. Total por lote = 60 * 40 = 2400. */
    public static final int ENERGY_PER_TICK = 40;

    private final ItemStackHandler inventory = new ItemStackHandler(3) {
        @Override protected void onContentsChanged(int slot) { setChanged(); }
        @Override public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return switch (slot) {
                case SLOT_INGOT -> ingotToPill(stack.getItem()) != null;
                case SLOT_BOTTLE -> stack.is(Items.GLASS_BOTTLE);
                default -> false; // OUT é só saída — bypass via setStackInSlot
            };
        }
    };

    /**
     * Buffer de energia interno. Aceita receber (canReceive=true) mas não emite
     * (canExtract via wrapper = false). Pipes/cabos enviam energia, processo consome.
     */
    private final EnergyStorage energy = new EnergyStorage(ENERGY_CAPACITY, 200, 0) {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            int got = super.receiveEnergy(maxReceive, simulate);
            if (got > 0 && !simulate) setChanged();
            return got;
        }
    };

    private LazyOptional<IItemHandler> lazyItem = LazyOptional.empty();
    private LazyOptional<IEnergyStorage> lazyEnergy = LazyOptional.empty();
    private int progress = 0;

    public final ContainerData data = new ContainerData() {
        @Override public int get(int i) {
            return switch (i) {
                case 0 -> progress;
                case 1 -> PROCESS_TICKS;
                case 2 -> energy.getEnergyStored();
                case 3 -> energy.getMaxEnergyStored();
                default -> 0;
            };
        }
        @Override public void set(int i, int v) {}
        @Override public int getCount() { return 4; }
    };

    public MatterPillBrewerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MATTER_PILL_BREWER.get(), pos, state);
    }

    public IItemHandler getInventory() { return inventory; }
    public IEnergyStorage getEnergy() { return energy; }

    /** Mapeia purified ingot → pílula correspondente. */
    public static @Nullable Item ingotToPill(Item ingot) {
        if (ingot == ModItems.PURIFIED_DARK_MATTER_INGOT.get())   return ModItems.DARK_MATTER_PILL.get();
        if (ingot == ModItems.PURIFIED_CLEAR_MATTER_INGOT.get())  return ModItems.CLEAR_MATTER_PILL.get();
        if (ingot == ModItems.PURIFIED_YELLOW_MATTER_INGOT.get()) return ModItems.YELLOW_MATTER_PILL.get();
        return null;
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
    }
    @Override public void invalidateCaps() {
        super.invalidateCaps();
        lazyItem.invalidate();
        lazyEnergy.invalidate();
    }

    public void drops() {
        net.minecraft.world.SimpleContainer c = new net.minecraft.world.SimpleContainer(inventory.getSlots());
        for (int i = 0; i < inventory.getSlots(); i++) c.setItem(i, inventory.getStackInSlot(i));
        net.minecraft.world.Containers.dropContents(level, worldPosition, c);
    }

    @Override public @NotNull Component getDisplayName() {
        return Component.translatable("container.liberthia.matter_pill_brewer");
    }
    @Override public @Nullable AbstractContainerMenu createMenu(int id, @NotNull Inventory inv, @NotNull Player p) {
        return new MatterPillBrewerMenu(id, inv, this, this.data);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MatterPillBrewerBlockEntity be) {
        if (level.isClientSide) return;
        if (!canProcess(be)) {
            if (be.progress > 0) {
                be.progress = 0;
                be.setChanged();
            }
            return;
        }
        // v0.1.49: requer energia pra processar.
        if (be.energy.getEnergyStored() < ENERGY_PER_TICK) {
            // Sem energia: pausa SEM resetar progresso. Quando voltar energia,
            // continua de onde parou.
            return;
        }
        be.energy.extractEnergy(ENERGY_PER_TICK, false);
        be.progress++;
        if (be.progress >= PROCESS_TICKS) {
            ItemStack ingot = be.inventory.getStackInSlot(SLOT_INGOT);
            Item pillItem = ingotToPill(ingot.getItem());
            if (pillItem != null) {
                be.inventory.extractItem(SLOT_INGOT, 1, false);
                be.inventory.extractItem(SLOT_BOTTLE, 1, false);
                // setStackInSlot pula isItemValid — necessário porque OUT
                // retorna false em isItemValid pra proteger insertion via hopper.
                ItemStack out = be.inventory.getStackInSlot(SLOT_OUTPUT);
                if (out.isEmpty()) {
                    be.inventory.setStackInSlot(SLOT_OUTPUT, new ItemStack(pillItem, 3));
                } else if (out.is(pillItem) && out.getCount() + 3 <= out.getMaxStackSize()) {
                    ItemStack newOut = out.copy();
                    newOut.grow(3);
                    be.inventory.setStackInSlot(SLOT_OUTPUT, newOut);
                }
            }
            be.progress = 0;
        }
        be.setChanged();
    }

    private static boolean canProcess(MatterPillBrewerBlockEntity be) {
        ItemStack ingot = be.inventory.getStackInSlot(SLOT_INGOT);
        ItemStack bottle = be.inventory.getStackInSlot(SLOT_BOTTLE);
        if (ingot.isEmpty() || bottle.isEmpty()) return false;
        if (!bottle.is(Items.GLASS_BOTTLE)) return false;
        Item pill = ingotToPill(ingot.getItem());
        if (pill == null) return false;
        ItemStack out = be.inventory.getStackInSlot(SLOT_OUTPUT);
        if (!out.isEmpty()) {
            if (out.getItem() != pill) return false;
            if (out.getCount() + 3 > out.getMaxStackSize()) return false;
        }
        return true;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        tag.put("inv", inventory.serializeNBT());
        tag.putInt("progress", progress);
        // Energy buffer survive restart
        CompoundTag energyTag = new CompoundTag();
        energyTag.putInt("energy", energy.getEnergyStored());
        tag.put("energy", energyTag);
        super.saveAdditional(tag);
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("inv")) inventory.deserializeNBT(tag.getCompound("inv"));
        progress = tag.getInt("progress");
        if (tag.contains("energy")) {
            int stored = tag.getCompound("energy").getInt("energy");
            // Não tem setEnergyStored público — usa receiveEnergy bypass simulate.
            // O EnergyStorage default tem campo "energy" mas é private; vamos
            // resetar e fillar.
            energy.extractEnergy(energy.getEnergyStored(), false); // zera
            energy.receiveEnergy(stored, false); // enche até o valor salvo
        }
    }
}
