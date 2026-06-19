package br.com.murilo.liberthia.block.entity;

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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * r184 — <b>Farol de Voo</b>: enquanto tiver FE, concede VOO (estilo criativo) a todos os
 * players num raio de {@value #RADIUS} blocos, usando SÓ energia. O voo é gerenciado por
 * {@code event/FlightBeaconHandler} via marcador na persistentData. GUI de energia.
 */
public class FlightBeaconBlockEntity extends BlockEntity implements MenuProvider, ITechEnergyUI {
    public static final String NBT_FLY = "liberthia.beaconFly";
    private static final int BUFFER = 400_000, RECEIVE = 20_000, COST = 30, RADIUS = 16;
    private static final int COST_GLOBAL = 500; // r186: voo no mundo TODO drena muito mais FE

    private final TrackedEnergyStorage store = new TrackedEnergyStorage(this, BUFFER, RECEIVE, 0);
    private LazyOptional<IEnergyStorage> lazy = LazyOptional.empty();
    private boolean active = false;

    // r186: slot de upgrade (Núcleo de Voo Global) → liga o modo mundo-todo
    private boolean globalMode = false;
    private final ItemStackHandler upgradeSlot = new ItemStackHandler(1) {
        @Override public boolean isItemValid(int slot, @NotNull ItemStack s) {
            return s.is(ModTech.BEACON_FLIGHT_UPGRADE.get());
        }
        @Override protected void onContentsChanged(int slot) { refreshGlobalMode(); setChanged(); }
    };
    private LazyOptional<IItemHandler> lazySlot = LazyOptional.empty();

    private final TechEnergyData uiData =
            new TechEnergyData(store::getEnergyStored, store::getMaxEnergyStored, () -> active ? 1 : 0, () -> 1);

    public FlightBeaconBlockEntity(BlockPos pos, BlockState state) { super(ModTech.FLIGHT_BEACON_BE.get(), pos, state); }

    private void refreshGlobalMode() { globalMode = !upgradeSlot.getStackInSlot(0).isEmpty(); }

    public static void tick(Level lvl, BlockPos pos, BlockState st, FlightBeaconBlockEntity be) {
        if (lvl.isClientSide || !(lvl instanceof ServerLevel sl)) return;
        boolean wasActive = be.active;
        if (be.globalMode) {
            // r186: modo GLOBAL — voo no mundo TODO (todos os players online, qualquer dimensão), FE alto e contínuo
            be.active = be.store.getEnergyStored() >= COST_GLOBAL;
            if (be.active && sl.getServer() != null) {
                be.store.setStored(be.store.getEnergyStored() - COST_GLOBAL);
                for (Player p : sl.getServer().getPlayerList().getPlayers()) {
                    if (p.isSpectator()) continue;
                    p.getPersistentData().putInt(NBT_FLY, 10);
                }
                if (sl.getGameTime() % 8 == 0)
                    sl.sendParticles(ParticleTypes.DRAGON_BREATH, pos.getX() + 0.5, pos.getY() + 1.4, pos.getZ() + 0.5, 6, 0.5, 0.6, 0.5, 0.04);
            }
        } else {
            be.active = be.store.getEnergyStored() >= COST;
            if (be.active) {
                AABB box = new AABB(pos).inflate(RADIUS);
                boolean any = false;
                for (Player p : lvl.getEntitiesOfClass(Player.class, box)) {
                    if (p.isSpectator()) continue;
                    if (p.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > (double) RADIUS * RADIUS) continue;
                    p.getPersistentData().putInt(NBT_FLY, 10);
                    any = true;
                }
                if (any) {
                    be.store.setStored(be.store.getEnergyStored() - COST);
                    if (sl.getGameTime() % 8 == 0)
                        sl.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 4, 0.3, 0.4, 0.3, 0.02);
                }
            }
        }
        if (be.active != wasActive) sl.setBlock(pos, st.setValue(br.com.murilo.liberthia.block.FlightBeaconBlock.LIT, be.active), 3);
    }

    public int getEnergyStored() { return store.getEnergyStored(); }
    public int getMaxEnergy() { return store.getMaxEnergyStored(); }

    @Override public ContainerData getEnergyData() { return uiData; }
    @Override public IItemHandler getMenuSlots() { return upgradeSlot; } // r186: slot do upgrade
    @Override public int getLayoutId() { return LAYOUT_CONSUMER; }

    public void drops() {
        SimpleContainer c = new SimpleContainer(1);
        c.setItem(0, upgradeSlot.getStackInSlot(0));
        Containers.dropContents(level, worldPosition, c);
    }
    @Override public Component getDisplayName() { return getBlockState().getBlock().getName(); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inv, Player p) {
        return new br.com.murilo.liberthia.menu.TechEnergyMenu(id, inv, this, uiData);
    }

    @Override public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ENERGY) return lazy.cast();
        if (cap == ForgeCapabilities.ITEM_HANDLER) return lazySlot.cast();
        return super.getCapability(cap, side);
    }
    @Override public void onLoad() { super.onLoad(); lazy = LazyOptional.of(() -> store); lazySlot = LazyOptional.of(() -> upgradeSlot); }
    @Override public void invalidateCaps() { super.invalidateCaps(); lazy.invalidate(); lazySlot.invalidate(); }
    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("Energy", store.getEnergyStored());
        tag.put("UpgradeSlot", upgradeSlot.serializeNBT());
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag);
        store.setStored(tag.getInt("Energy"));
        if (tag.contains("UpgradeSlot")) upgradeSlot.deserializeNBT(tag.getCompound("UpgradeSlot"));
        refreshGlobalMode();
    }
}
