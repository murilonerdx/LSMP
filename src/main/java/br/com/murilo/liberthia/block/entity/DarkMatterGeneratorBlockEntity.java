package br.com.murilo.liberthia.block.entity;

import br.com.murilo.liberthia.menu.DarkMatterGeneratorMenu;
import br.com.murilo.liberthia.registry.ModBlockEntities;
import br.com.murilo.liberthia.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
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
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Dark Matter Generator — furnace-style FE producer.
 *
 * <p>One {@code dark_matter_block} consumed grants {@link #ENERGY_PER_BLOCK}
 * FE worth of "burn fuel". Each tick, {@link #FE_PER_TICK} FE is moved out of
 * burn fuel into the output buffer (cap {@link #BUFFER_CAPACITY}); excess in
 * the buffer is then pushed to FE-accepting neighbours.
 *
 * <p>Implements {@link MenuProvider} so right-clicking the block opens a GUI
 * showing the live energy bar and burn progress arrow.
 */
public class DarkMatterGeneratorBlockEntity extends BlockEntity
        implements MenuProvider, br.com.murilo.liberthia.persistence.Persistable {

    public static final int ENERGY_PER_BLOCK = 500_000;
    public static final int BUFFER_CAPACITY  = 100_000;
    public static final int FE_PER_TICK      = 1_000;

    /** Slots: 0=fuel, 1=speed upgrade, 2=efficiency upgrade, 3=capacity upgrade. */
    public static final int SLOT_FUEL = 0;
    public static final int SLOT_SPEED = 1;
    public static final int SLOT_EFFICIENCY = 2;
    public static final int SLOT_CAPACITY = 3;

    private final ItemStackHandler inventory = new ItemStackHandler(4) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if (slot != SLOT_FUEL) recomputeUpgrades();
            if (level instanceof net.minecraft.server.level.ServerLevel sl) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
                // SNAPSHOT IMEDIATO em mudança de inventário
                br.com.murilo.liberthia.persistence.LiberthiaPersistence.get(sl)
                        .snapshot(sl, worldPosition, saveWithFullMetadata());
                br.com.murilo.liberthia.LiberthiaMod.LOGGER.info(
                        "[Generator@{}] onContentsChanged slot={} stack={}x{} (snapshot salvo)",
                        worldPosition, slot, getStackInSlot(slot).getCount(),
                        getStackInSlot(slot).getItem());
            }
        }
        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return switch (slot) {
                case SLOT_FUEL        -> stack.is(ModBlocks.DARK_MATTER_BLOCK.get().asItem());
                case SLOT_SPEED       -> stack.is(br.com.murilo.liberthia.registry.ModItems.SPEED_UPGRADE.get());
                case SLOT_EFFICIENCY  -> stack.is(br.com.murilo.liberthia.registry.ModItems.EFFICIENCY_UPGRADE.get());
                case SLOT_CAPACITY    -> stack.is(br.com.murilo.liberthia.registry.ModItems.CAPACITY_UPGRADE.get());
                default -> false;
            };
        }
        @Override
        public int getSlotLimit(int slot) {
            return slot == SLOT_FUEL ? 64 : 4;
        }
    };

    /**
     * EnergyStorage com capacidade/transfer mutáveis em runtime + tracking.
     *
     * <p>{@code maxReceive} controla o BURN RATE (combustível → buffer).
     * {@code maxExtract} = MAX_VALUE pra suportar pull-based (laser).
     *
     * <p>TrackedEnergyStorage chama setChanged() no BE em receiveEnergy/extractEnergy.
     * Sem isso, mutações via capability (laser puxando) não marcam chunk dirty
     * → energia "some" no restart do servidor.
     */
    private final class DynamicEnergy extends br.com.murilo.liberthia.energy.TrackedEnergyStorage {
        DynamicEnergy(int cap, int rate) {
            super(DarkMatterGeneratorBlockEntity.this, cap, rate, Integer.MAX_VALUE);
        }
        void setCapacity(int newCap) {
            this.capacity = newCap;
            if (this.energy > newCap) this.energy = newCap;
        }
        void setTransfer(int rate) {
            this.maxReceive = rate;
        }
    }

    private final DynamicEnergy energy = new DynamicEnergy(BUFFER_CAPACITY, FE_PER_TICK);

    /** Multiplicadores em cache, recalculados quando o inventário muda. */
    private int speedUpgrades = 0;
    private int efficiencyUpgrades = 0;
    private int capacityUpgrades = 0;

    /** Recalcula multiplicadores a partir dos upgrades nos slots. */
    private void recomputeUpgrades() {
        speedUpgrades      = inventory.getStackInSlot(SLOT_SPEED).getCount();
        efficiencyUpgrades = inventory.getStackInSlot(SLOT_EFFICIENCY).getCount();
        capacityUpgrades   = inventory.getStackInSlot(SLOT_CAPACITY).getCount();
        // Speed: +100% por upgrade (1, 2, 3, 4, 5 ×)
        // Capacity: +100% por upgrade (1×, 2×, 3×, 4×, 5×)
        int newCap = BUFFER_CAPACITY * (1 + capacityUpgrades);
        int newRate = FE_PER_TICK * (1 + speedUpgrades);
        energy.setCapacity(newCap);
        energy.setTransfer(newRate);
    }

    public int currentFePerTick()      { return FE_PER_TICK * (1 + speedUpgrades); }
    public int currentEnergyPerBlock() {
        // Eficiência: +50% por upgrade
        return ENERGY_PER_BLOCK + (ENERGY_PER_BLOCK / 2) * efficiencyUpgrades;
    }
    /** FE pro bloco no slot, considerando pureza NBT (1×..3.5×). */
    public int feForCurrentFuelStack() {
        int base = currentEnergyPerBlock();
        ItemStack peek = inventory.getStackInSlot(SLOT_FUEL);
        if (peek.isEmpty()) return base;
        int purity = br.com.murilo.liberthia.util.Purity.getPurity(peek);
        return (int) (base * br.com.murilo.liberthia.util.Purity.feMultiplier(purity));
    }
    public int speedUpgradeCount()      { return speedUpgrades; }
    public int efficiencyUpgradeCount() { return efficiencyUpgrades; }
    public int capacityUpgradeCount()   { return capacityUpgrades; }

    /** FE remaining in the active "burn" — counts down toward 0. */
    private int burnFuel = 0;
    /** Snapshot of {@code burnFuel} at ignition time, for progress UI. */
    private int burnFuelInitial = 0;

    private LazyOptional<IItemHandler> lazyItem = LazyOptional.empty();
    private LazyOptional<IEnergyStorage> lazyEnergy = LazyOptional.empty();

    /**
     * Sync para o cliente. {@link ContainerData} sincroniza como SHORT (16 bits),
     * então valores acima de 32.767 viram negativos. Cada {@code int} é
     * dividido em dois slots: alto (>>16) e baixo (&0xFFFF). O menu reconstrói
     * com {@code (hi << 16) | (lo & 0xFFFF)}.
     *
     * <p>Layout: 0/1=energy hi/lo, 2/3=energyMax hi/lo, 4/5=burnFuel hi/lo,
     * 6/7=burnFuelInitial hi/lo.
     */
    public final ContainerData data = new ContainerData() {
        @Override public int get(int i) {
            int v = switch (i) {
                case 0, 1 -> energy.getEnergyStored();
                case 2, 3 -> energy.getMaxEnergyStored();
                case 4, 5 -> burnFuel;
                case 6, 7 -> burnFuelInitial == 0 ? 1 : burnFuelInitial;
                default -> 0;
            };
            return (i % 2 == 0) ? (v >> 16) & 0xFFFF : v & 0xFFFF;
        }
        @Override public void set(int i, int v) {}
        @Override public int getCount() { return 8; }
    };

    public DarkMatterGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DARK_MATTER_GENERATOR.get(), pos, state);
    }

    public IItemHandler getItemHandler() { return inventory; }
    public boolean isBurning() { return burnFuel > 0; }

    @Override public @NotNull Component getDisplayName() {
        return Component.translatable("container.liberthia.dark_matter_generator");
    }

    @Nullable @Override
    public AbstractContainerMenu createMenu(int id, @NotNull Inventory inv, @NotNull Player p) {
        return new DarkMatterGeneratorMenu(id, inv, this, this.data);
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) return lazyItem.cast();
        if (cap == ForgeCapabilities.ENERGY) return lazyEnergy.cast();
        return super.getCapability(cap, side);
    }

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
        Containers.dropContents(this.level, this.worldPosition, c);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, DarkMatterGeneratorBlockEntity be) {
        if (level.isClientSide) return;

        int feRate = be.currentFePerTick();
        int feFromBlock = be.feForCurrentFuelStack();
        int cap = be.energy.getMaxEnergyStored();

        // 1) Ignite a new fuel block QUANDO a reserva total tá baixa.
        //
        // Antes: consumia bloco assim que burnFuel=0 + buffer<cap. Resultado:
        // user botava 5 blocos, todos sumiam mesmo sem consumidor — o FE
        // ficava invisível em burnFuel + buffer, dando aparência de "sumiço".
        //
        // Agora: só consome bloco se TOTAL de reserva (burnFuel + buffer) for
        // < feFromBlock. Garante que blocos só somem quando a energia foi
        // realmente usada por consumidores externos.
        long totalReserve = (long) be.burnFuel + (long) be.energy.getEnergyStored();
        if (be.burnFuel <= 0 && totalReserve < feFromBlock) {
            ItemStack peek = be.inventory.getStackInSlot(SLOT_FUEL);
            if (!peek.isEmpty() && peek.is(ModBlocks.DARK_MATTER_BLOCK.get().asItem())) {
                ItemStack extracted = be.inventory.extractItem(SLOT_FUEL, 1, false);
                if (!extracted.isEmpty()) {
                    be.burnFuel = feFromBlock;
                    be.burnFuelInitial = feFromBlock;
                    be.setChanged();
                }
            }
        }

        // 2) Burn — drain reserve into the buffer at the (upgraded) rate.
        if (be.burnFuel > 0 && be.energy.getEnergyStored() < cap) {
            int request = Math.min(feRate, be.burnFuel);
            int accepted = be.energy.receiveEnergy(request, false);
            if (accepted > 0) {
                be.burnFuel -= accepted;
                if (be.burnFuel <= 0) be.burnFuelInitial = 0;
                be.setChanged();
            }
        }

        // 3) Push buffered FE to consumers (BFS via cabos Liberthia).
        if (be.energy.getEnergyStored() > 0) {
            int budget = Math.min(feRate, be.energy.getEnergyStored());
            int sent = br.com.murilo.liberthia.energy.EnergyNetwork
                    .pushThroughNetwork(level, pos, budget);
            if (sent > 0) {
                be.energy.extractEnergy(sent, false);
                be.setChanged();
            }
        }

        // 4) Snapshot periódico AGRESSIVO (1s) quando state non-empty.
        // Garante que mesmo crash inesperado, o backup file tem dado fresco.
        if (level instanceof net.minecraft.server.level.ServerLevel sl
                && !be.isStateEmpty()
                && level.getGameTime() % 20 == 0) {
            br.com.murilo.liberthia.persistence.LiberthiaPersistence.get(sl)
                    .snapshot(sl, pos, be.saveWithFullMetadata());
        }
    }

    // ==================== Persistable ====================

    @Override
    public boolean isStateEmpty() {
        // "Vazio" = sem fuel, sem upgrades, sem energia, sem burn em curso.
        if (energy.getEnergyStored() > 0) return false;
        if (burnFuel > 0) return false;
        for (int i = 0; i < inventory.getSlots(); i++) {
            if (!inventory.getStackInSlot(i).isEmpty()) return false;
        }
        return true;
    }

    @Override
    public void restoreFromSnapshot(CompoundTag tag) {
        // Reusa load() — formato é o mesmo do saveAdditional.
        load(tag);
        setChanged();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        lazyItem = LazyOptional.of(() -> inventory);
        lazyEnergy = LazyOptional.of(() -> energy);
        br.com.murilo.liberthia.persistence.Persistable.LIVE.add(this);

        // Diagnóstico: log estado atual no onLoad
        br.com.murilo.liberthia.LiberthiaMod.LOGGER.info(
                "[Generator@{}] onLoad — fuel={}x energy={} burnFuel={} stateEmpty={}",
                worldPosition,
                inventory.getStackInSlot(SLOT_FUEL).getCount(),
                energy.getEnergyStored(), burnFuel, isStateEmpty());

        // Backup-restore: se NBT carregou vazio mas SavedData tem snapshot recente
        if (level instanceof net.minecraft.server.level.ServerLevel sl && isStateEmpty()) {
            CompoundTag snapshot = br.com.murilo.liberthia.persistence.LiberthiaPersistence
                    .get(sl).getSnapshot(sl, worldPosition);
            if (snapshot != null) {
                br.com.murilo.liberthia.LiberthiaMod.LOGGER.info(
                        "[Generator@{}] NBT vanilla VAZIO, snapshot tem fuel={} energy={} burnFuel={} — restaurando",
                        worldPosition,
                        snapshot.contains("inventory") ? "presente" : "ausente",
                        snapshot.contains("energy") ? "presente" : "ausente",
                        snapshot.getInt("burnFuel"));
                restoreFromSnapshot(snapshot);
                br.com.murilo.liberthia.LiberthiaMod.LOGGER.info(
                        "[Generator@{}] Pós-restore: fuel={}x energy={} burnFuel={}",
                        worldPosition,
                        inventory.getStackInSlot(SLOT_FUEL).getCount(),
                        energy.getEnergyStored(), burnFuel);
            } else {
                br.com.murilo.liberthia.LiberthiaMod.LOGGER.info(
                        "[Generator@{}] NBT vanilla VAZIO E sem snapshot — estado perdido",
                        worldPosition);
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        tag.put("inventory", inventory.serializeNBT());
        tag.put("energy", energy.serializeNBT());
        tag.putInt("burnFuel", burnFuel);
        tag.putInt("burnFuelInitial", burnFuelInitial);
        super.saveAdditional(tag);
        if (level != null && !level.isClientSide()) {
            br.com.murilo.liberthia.LiberthiaMod.LOGGER.debug(
                    "[Generator@{}] saveAdditional: fuel={}x energy={} burnFuel={}",
                    worldPosition,
                    inventory.getStackInSlot(SLOT_FUEL).getCount(),
                    energy.getEnergyStored(), burnFuel);
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("inventory")) inventory.deserializeNBT(tag.getCompound("inventory"));
        if (tag.contains("energy")) energy.deserializeNBT(tag.get("energy"));
        burnFuel = tag.getInt("burnFuel");
        burnFuelInitial = tag.getInt("burnFuelInitial");
        recomputeUpgrades();
    }

    @Nullable @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override
    public CompoundTag getUpdateTag() { return saveWithoutMetadata(); }
}
