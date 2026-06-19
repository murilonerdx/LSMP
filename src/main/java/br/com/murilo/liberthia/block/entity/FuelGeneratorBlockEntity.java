package br.com.murilo.liberthia.block.entity;

import br.com.murilo.liberthia.energy.EnergyNetwork;
import br.com.murilo.liberthia.energy.TrackedEnergyStorage;
import br.com.murilo.liberthia.registry.ModTech;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * r181 — gerador FE de combustível: <b>Furnator</b> (queima qualquer combustível de
 * fornalha — coal/charcoal/blaze/madeira) ou <b>Magmator</b> (queima baldes de lava,
 * devolve o balde vazio). 1 BE p/ os 2 blocos (lê o tipo do bloco). Property LIT.
 * Empurra pela {@link EnergyNetwork} (SourceOnlyEnergyView). Aceita hopper.
 */
public class FuelGeneratorBlockEntity extends BlockEntity implements MenuProvider, ITechEnergyUI {

    private static final int BUFFER = 400_000, PUSH = 20_000;
    private static final int LAVA_BURN = 20_000;

    private final boolean lava;
    private final int genRate;
    private final ItemStackHandler fuel = new ItemStackHandler(1) {
        @Override protected void onContentsChanged(int slot) { setChanged(); }
        @Override public boolean isItemValid(int slot, @NotNull ItemStack s) {
            return lava ? (s.is(Items.LAVA_BUCKET) || s.is(br.com.murilo.liberthia.registry.ModItems.DARK_MATTER_BUCKET.get()))
                        : ForgeHooks.getBurnTime(s, null) > 0;
        }
    };
    private LazyOptional<IItemHandler> lazyItems = LazyOptional.empty();
    private final TrackedEnergyStorage store = new TrackedEnergyStorage(this, BUFFER, BUFFER, Integer.MAX_VALUE);
    private LazyOptional<IEnergyStorage> lazyEnergy = LazyOptional.empty();
    private int burnTime = 0, burnTotal = 0;
    private final TechEnergyData uiData = new TechEnergyData(
            store::getEnergyStored, store::getMaxEnergyStored, () -> burnTime, () -> burnTotal);

    public FuelGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModTech.FUEL_GENERATOR_BE.get(), pos, state);
        this.lava = state.getBlock() == ModTech.MAGMATOR.get();
        this.genRate = lava ? 140 : 100;
    }

    // ── GUI de energia (ITechEnergyUI + MenuProvider) ──
    @Override public ContainerData getEnergyData() { return uiData; }
    @Override public IItemHandler getMenuSlots() { return fuel; }
    @Override public int getLayoutId() { return LAYOUT_GENERATOR; }
    @Override public Component getDisplayName() { return getBlockState().getBlock().getName(); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inv, Player p) {
        return new br.com.murilo.liberthia.menu.TechEnergyMenu(id, inv, this, uiData);
    }

    public static void tick(Level lvl, BlockPos pos, BlockState st, FuelGeneratorBlockEntity be) {
        if (lvl.isClientSide || !(lvl instanceof ServerLevel sl)) return;
        boolean wasLit = st.getValue(br.com.murilo.liberthia.block.FuelGeneratorBlock.LIT);

        if (be.burnTime <= 0 && be.store.getEnergyStored() < BUFFER) {
            ItemStack in = be.fuel.getStackInSlot(0);
            boolean dmBucket = in.is(br.com.murilo.liberthia.registry.ModItems.DARK_MATTER_BUCKET.get());
            if (be.lava && (in.is(Items.LAVA_BUCKET) || dmBucket)) {
                be.burnTime = be.burnTotal = dmBucket ? 60000 : LAVA_BURN; // matéria escura queima MUITO mais
                be.fuel.extractItem(0, 1, false);
                if (be.fuel.getStackInSlot(0).isEmpty()) be.fuel.setStackInSlot(0, new ItemStack(Items.BUCKET));
            } else if (!be.lava) {
                int bt = ForgeHooks.getBurnTime(in, null);
                if (bt > 0) { be.burnTime = be.burnTotal = bt; be.fuel.extractItem(0, 1, false); }
            }
        }

        if (be.burnTime > 0 && be.store.getEnergyStored() < BUFFER) {
            be.burnTime--;
            be.store.receiveEnergy(be.genRate, false);
            if (sl.getGameTime() % 4 == 0) {
                if (be.lava) sl.sendParticles(ParticleTypes.LAVA, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 1, 0.2, 0.05, 0.2, 0.0);
                else sl.sendParticles(ParticleTypes.FLAME, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 1, 0.2, 0.05, 0.2, 0.01);
                sl.sendParticles(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 1.05, pos.getZ() + 0.5, 1, 0.18, 0.05, 0.18, 0.01);
            }
        }

        int stored = be.store.getEnergyStored();
        if (stored > 0) {
            int sent = EnergyNetwork.pushThroughNetwork(lvl, pos, Math.min(PUSH, stored));
            if (sent > 0) be.store.extractEnergy(sent, false);
        }

        boolean lit = be.burnTime > 0;
        if (lit != wasLit) sl.setBlock(pos, st.setValue(br.com.murilo.liberthia.block.FuelGeneratorBlock.LIT, lit), 3);
        be.setChanged();
    }

    public IItemHandler getFuel() { return fuel; }
    public int getEnergyStored() { return store.getEnergyStored(); }
    public int getMaxEnergy() { return store.getMaxEnergyStored(); }
    public int getBurnTime() { return burnTime; }
    public boolean isLava() { return lava; }

    @Override public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ENERGY) return lazyEnergy.cast();
        if (cap == ForgeCapabilities.ITEM_HANDLER) return lazyItems.cast();
        return super.getCapability(cap, side);
    }
    @Override public void onLoad() {
        super.onLoad();
        lazyEnergy = LazyOptional.of(() -> new SourceOnlyEnergyView(store));
        lazyItems = LazyOptional.of(() -> fuel);
    }
    @Override public void invalidateCaps() { super.invalidateCaps(); lazyEnergy.invalidate(); lazyItems.invalidate(); }

    public void drops() {
        if (level != null) net.minecraft.world.Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), fuel.getStackInSlot(0));
    }

    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("fuel", fuel.serializeNBT());
        tag.putInt("Energy", store.getEnergyStored());
        tag.putInt("burnTime", burnTime);
        tag.putInt("burnTotal", burnTotal);
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("fuel")) fuel.deserializeNBT(tag.getCompound("fuel"));
        store.setStored(tag.getInt("Energy"));
        burnTime = tag.getInt("burnTime");
        burnTotal = tag.getInt("burnTotal");
    }

    private static final class SourceOnlyEnergyView implements IEnergyStorage {
        private final IEnergyStorage d;
        SourceOnlyEnergyView(IEnergyStorage d) { this.d = d; }
        @Override public int receiveEnergy(int m, boolean s) { return 0; }
        @Override public int extractEnergy(int m, boolean s) { return d.extractEnergy(m, s); }
        @Override public int getEnergyStored() { return d.getEnergyStored(); }
        @Override public int getMaxEnergyStored() { return d.getMaxEnergyStored(); }
        @Override public boolean canExtract() { return true; }
        @Override public boolean canReceive() { return false; }
    }
}
