package br.com.murilo.liberthia.block.entity;

import br.com.murilo.liberthia.energy.EnergyNetwork;
import br.com.murilo.liberthia.energy.TrackedEnergyStorage;
import br.com.murilo.liberthia.registry.ModTech;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * r180c — gerador FE tech: <b>Painel Solar</b> (FE com céu+dia) ou <b>Gerador Térmico</b>
 * (FE por lava adjacente). Lê o tipo do bloco. Empurra pela rede ({@link EnergyNetwork}).
 * Mesmo padrão dos geradores existentes (SourceOnlyEnergyView).
 */
public class TechGeneratorBlockEntity extends BlockEntity implements MenuProvider, ITechEnergyUI {

    private static final int BUFFER = 200_000;
    private static final int PUSH = 20_000;

    private final boolean solar;
    private final int genRate;
    private final TrackedEnergyStorage store;
    private LazyOptional<IEnergyStorage> lazy;
    private int lastGen = 0;
    private final TechEnergyData uiData;

    public TechGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModTech.TECH_GENERATOR_BE.get(), pos, state);
        this.solar = state.getBlock() == ModTech.SOLAR_PANEL.get();
        this.genRate = solar ? 80 : 120;
        this.store = new TrackedEnergyStorage(this, BUFFER, BUFFER, Integer.MAX_VALUE);
        this.lazy = LazyOptional.of(() -> new SourceOnlyEnergyView(store));
        this.uiData = new TechEnergyData(store::getEnergyStored, store::getMaxEnergyStored, () -> lastGen, () -> genRate);
    }

    // ── GUI de energia (PASSIVE: só energia + taxa de geração) ──
    @Override public ContainerData getEnergyData() { return uiData; }
    @Override public IItemHandler getMenuSlots() { return null; }
    @Override public int getLayoutId() { return LAYOUT_PASSIVE; }
    @Override public Component getDisplayName() { return getBlockState().getBlock().getName(); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inv, Player p) {
        return new br.com.murilo.liberthia.menu.TechEnergyMenu(id, inv, this, uiData);
    }

    public static void tick(Level lvl, BlockPos pos, BlockState st, TechGeneratorBlockEntity be) {
        if (lvl.isClientSide || !(lvl instanceof ServerLevel sl)) return;

        int gen = 0;
        if (be.solar) {
            if (sl.isDay() && !sl.isRaining() && lvl.canSeeSky(pos.above())) gen = be.genRate;
        } else {
            int lava = 0;
            for (Direction d : Direction.values()) {
                if (lvl.getFluidState(pos.relative(d)).is(Fluids.LAVA)) lava++;
            }
            gen = be.genRate * lava;
        }
        if (gen > 0) be.store.receiveEnergy(gen, false);
        be.lastGen = gen;

        int stored = be.store.getEnergyStored();
        if (stored > 0) {
            int sent = EnergyNetwork.pushThroughNetwork(lvl, pos, Math.min(PUSH, stored));
            if (sent > 0) be.store.extractEnergy(sent, false);
        }
    }

    public int getEnergyStored() { return store.getEnergyStored(); }
    public int getMaxEnergy()    { return store.getMaxEnergyStored(); }
    public int getLastGen()      { return lastGen; }
    public boolean isSolar()     { return solar; }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ENERGY) return lazy.cast();
        return super.getCapability(cap, side);
    }

    @Override public void invalidateCaps() { super.invalidateCaps(); lazy.invalidate(); }

    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("Energy", store.getEnergyStored());
    }

    @Override public void load(CompoundTag tag) {
        super.load(tag);
        store.setStored(tag.getInt("Energy"));
    }

    /** Source-only externamente (não recebe FE de fora; só gera + extrai). */
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
