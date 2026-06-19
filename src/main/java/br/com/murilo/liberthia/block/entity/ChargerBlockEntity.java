package br.com.murilo.liberthia.block.entity;

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
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * r180c/r182 — <b>Carregador</b>: recebe FE da rede e carrega (a) o item posto no slot
 * da GUI ({@link ITechEnergyUI} layout CHARGER) e (b) automaticamente os itens FE de
 * jogadores num raio de {@value #RADIUS} blocos. Sem GUI antes — agora abre a tela de
 * energia no clique direito.
 */
public class ChargerBlockEntity extends BlockEntity implements MenuProvider, ITechEnergyUI {
    private static final int CAP = 2_000_000, RECEIVE = 40_000, BUDGET = 20_000, RADIUS = 5;

    private final TrackedEnergyStorage store = new TrackedEnergyStorage(this, CAP, RECEIVE, 0);
    private LazyOptional<IEnergyStorage> lazy = LazyOptional.empty();

    private final ItemStackHandler charge = new ItemStackHandler(1) {
        @Override protected void onContentsChanged(int slot) { setChanged(); }
        @Override public boolean isItemValid(int slot, @NotNull ItemStack s) {
            return s.getCapability(ForgeCapabilities.ENERGY).map(IEnergyStorage::canReceive).orElse(false);
        }
    };
    private LazyOptional<IItemHandler> lazyItems = LazyOptional.empty();
    private final TechEnergyData uiData =
            new TechEnergyData(store::getEnergyStored, store::getMaxEnergyStored, () -> 0, () -> 0);

    public ChargerBlockEntity(BlockPos pos, BlockState state) { super(ModTech.CHARGER_BE.get(), pos, state); }

    public static void tick(Level lvl, BlockPos pos, BlockState st, ChargerBlockEntity be) {
        if (lvl.isClientSide) return;
        int stored = be.store.getEnergyStored();
        if (stored <= 0) return;
        int budget = Math.min(BUDGET, stored);

        // (a) item no slot da GUI
        ItemStack s = be.charge.getStackInSlot(0);
        if (!s.isEmpty() && budget > 0) {
            final int b = budget;
            int moved = s.getCapability(ForgeCapabilities.ENERGY)
                    .map(e -> e.canReceive() ? e.receiveEnergy(b, false) : 0).orElse(0);
            budget -= moved;
        }

        // (b) jogadores no raio
        AABB box = new AABB(pos).inflate(RADIUS);
        for (Player p : lvl.getEntitiesOfClass(Player.class, box)) {
            if (budget <= 0) break;
            var inv = p.getInventory();
            for (int i = 0; i < inv.getContainerSize() && budget > 0; i++) {
                ItemStack it = inv.getItem(i);
                if (it.isEmpty()) continue;
                final int b = budget;
                int moved = it.getCapability(ForgeCapabilities.ENERGY)
                        .map(e -> e.canReceive() ? e.receiveEnergy(b, false) : 0).orElse(0);
                budget -= moved;
            }
        }

        int spent = Math.min(BUDGET, stored) - budget;
        if (spent > 0) be.store.setStored(stored - spent);
    }

    public int getEnergyStored() { return store.getEnergyStored(); }
    public int getMaxEnergy() { return store.getMaxEnergyStored(); }

    // ── GUI de energia (ITechEnergyUI + MenuProvider) ──
    @Override public ContainerData getEnergyData() { return uiData; }
    @Override public IItemHandler getMenuSlots() { return charge; }
    @Override public int getLayoutId() { return LAYOUT_CHARGER; }
    @Override public Component getDisplayName() { return getBlockState().getBlock().getName(); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inv, Player p) {
        return new br.com.murilo.liberthia.menu.TechEnergyMenu(id, inv, this, uiData);
    }

    @Override public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ENERGY) return lazy.cast();
        if (cap == ForgeCapabilities.ITEM_HANDLER) return lazyItems.cast();
        return super.getCapability(cap, side);
    }
    @Override public void onLoad() { super.onLoad(); lazy = LazyOptional.of(() -> store); lazyItems = LazyOptional.of(() -> charge); }
    @Override public void invalidateCaps() { super.invalidateCaps(); lazy.invalidate(); lazyItems.invalidate(); }

    public void drops() {
        if (level != null) net.minecraft.world.Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), charge.getStackInSlot(0));
    }

    @Override protected void saveAdditional(CompoundTag tag) { super.saveAdditional(tag); tag.putInt("Energy", store.getEnergyStored()); tag.put("charge", charge.serializeNBT()); }
    @Override public void load(CompoundTag tag) { super.load(tag); store.setStored(tag.getInt("Energy")); if (tag.contains("charge")) charge.deserializeNBT(tag.getCompound("charge")); }
}
