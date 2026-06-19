package br.com.murilo.liberthia.block.entity;

import br.com.murilo.liberthia.block.MatterReactorBlock;
import br.com.murilo.liberthia.energy.EnergyNetwork;
import br.com.murilo.liberthia.energy.TrackedEnergyStorage;
import br.com.murilo.liberthia.registry.ModItems;
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
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * r181 — <b>Reator de Matéria Escura</b>: a INTEGRAÇÃO entre os dois sistemas do mod.
 * Queima matéria escura (shard/inactive/active/ingot/block/catalyst) e abastece toda a
 * rede FE da linha tech (células, máquinas, carregador). Empurra pela {@link EnergyNetwork}.
 * Quando aceso, o {@code client/renderer/MatterReactorRenderer} mostra um ORBE de matéria
 * escura flutuando e girando por cima (animação). Aceita combustível por hopper (ITEM_HANDLER)
 * ou clique direito.
 */
public class MatterReactorBlockEntity extends BlockEntity implements MenuProvider, ITechEnergyUI {

    private static final int BUFFER = 2_000_000, GEN_RATE = 200, PUSH = 40_000;

    private final ItemStackHandler fuel = new ItemStackHandler(1) {
        @Override protected void onContentsChanged(int slot) { setChanged(); }
        @Override public boolean isItemValid(int slot, @NotNull ItemStack s) { return burnTicksFor(s) > 0; }
    };
    private LazyOptional<IItemHandler> lazyItems = LazyOptional.empty();
    private final TrackedEnergyStorage store = new TrackedEnergyStorage(this, BUFFER, BUFFER, Integer.MAX_VALUE);
    private LazyOptional<IEnergyStorage> lazyEnergy = LazyOptional.empty();

    private int burnTime = 0, burnTotal = 0;
    private final TechEnergyData uiData = new TechEnergyData(
            store::getEnergyStored, store::getMaxEnergyStored, () -> burnTime, () -> burnTotal);

    public MatterReactorBlockEntity(BlockPos pos, BlockState state) {
        super(ModTech.MATTER_REACTOR_BE.get(), pos, state);
    }

    // ── GUI de energia (ITechEnergyUI + MenuProvider) ──
    @Override public ContainerData getEnergyData() { return uiData; }
    @Override public IItemHandler getMenuSlots() { return fuel; }
    @Override public int getLayoutId() { return LAYOUT_GENERATOR; }
    @Override public Component getDisplayName() { return getBlockState().getBlock().getName(); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inv, Player p) {
        return new br.com.murilo.liberthia.menu.TechEnergyMenu(id, inv, this, uiData);
    }

    /** Quanto cada combustível de matéria escura queima (ticks). 0 = inválido. */
    public static int burnTicksFor(ItemStack s) {
        if (s.isEmpty()) return 0;
        if (s.is(ModItems.DARK_MATTER_SHARD.get()))      return 1200;
        if (s.is(ModItems.INACTIVE_DARK_MATTER.get()))   return 1600;
        if (s.is(ModItems.DARK_MATTER_CATALYST.get()))   return 8000;
        if (s.is(ModItems.ACTIVE_DARK_MATTER.get()))     return 3200;
        if (s.is(ModItems.DARK_MATTER_INGOT.get()))      return 4800;
        if (s.is(ModItems.DARK_MATTER_BLOCK_ITEM.get())) return 24000;
        return 0;
    }

    public static void tick(Level lvl, BlockPos pos, BlockState st, MatterReactorBlockEntity be) {
        if (lvl.isClientSide || !(lvl instanceof ServerLevel sl)) return;
        boolean wasLit = st.getValue(MatterReactorBlock.LIT);

        // acender consumindo combustível
        if (be.burnTime <= 0) {
            ItemStack in = be.fuel.getStackInSlot(0);
            int ticks = burnTicksFor(in);
            if (ticks > 0 && be.store.getEnergyStored() < BUFFER) {
                be.burnTime = be.burnTotal = ticks;
                be.fuel.extractItem(0, 1, false);
            }
        }

        // gerar — só consome combustível se o buffer tem espaço (não desperdiça queima)
        if (be.burnTime > 0 && be.store.getEnergyStored() < BUFFER) {
            be.burnTime--;
            be.store.receiveEnergy(GEN_RATE, false);
            if (sl.getGameTime() % 3 == 0) {
                sl.sendParticles(ParticleTypes.REVERSE_PORTAL, pos.getX() + 0.5, pos.getY() + 1.15, pos.getZ() + 0.5, 2, 0.18, 0.18, 0.18, 0.02);
                sl.sendParticles(ParticleTypes.WITCH, pos.getX() + 0.5, pos.getY() + 1.15, pos.getZ() + 0.5, 1, 0.2, 0.2, 0.2, 0.0);
            }
        }

        // empurrar pela rede
        int stored = be.store.getEnergyStored();
        if (stored > 0) {
            int sent = EnergyNetwork.pushThroughNetwork(lvl, pos, Math.min(PUSH, stored));
            if (sent > 0) be.store.extractEnergy(sent, false);
        }

        boolean lit = be.burnTime > 0;
        if (lit != wasLit) sl.setBlock(pos, st.setValue(MatterReactorBlock.LIT, lit), 3);
        be.setChanged();
    }

    public IItemHandler getFuel() { return fuel; }
    public int getEnergyStored() { return store.getEnergyStored(); }
    public int getMaxEnergy() { return store.getMaxEnergyStored(); }
    public int getBurnTime() { return burnTime; }
    public int getBurnTotal() { return burnTotal; }

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

    /** Source-only externamente (gera + extrai; nunca recebe FE de fora). */
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
