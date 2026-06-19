package br.com.murilo.liberthia.block.entity;

import br.com.murilo.liberthia.menu.RiftForgeMenu;
import br.com.murilo.liberthia.registry.ModBlockEntities;
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
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * r185 — BlockEntity da Forja de Fendas: 12 slots de entrada (0–11) + 1 de saída (12). Quando os
 * 12 batem com {@link RiftForgeRecipes#T3}, processa por 200 ticks e produz a Adaga Corta-Fendas III.
 */
public class RiftForgeBlockEntity extends BlockEntity implements MenuProvider {
    public static final int INPUT_SLOTS = 12;
    public static final int OUTPUT_SLOT = 12;
    public static final int TOTAL_SLOTS = 13;
    private static final int MAX_PROGRESS = 200;

    private final ItemStackHandler inventory = new ItemStackHandler(TOTAL_SLOTS) {
        @Override protected void onContentsChanged(int slot) { setChanged(); }
        @Override public boolean isItemValid(int slot, @NotNull ItemStack s) { return slot < INPUT_SLOTS; }
    };
    private LazyOptional<IItemHandler> lazyItems = LazyOptional.empty();
    private int progress = 0;

    public RiftForgeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RIFT_FORGE.get(), pos, state);
    }

    public final ContainerData data = new ContainerData() {
        @Override public int get(int i) { return switch (i) { case 0 -> progress; case 1 -> MAX_PROGRESS; default -> 0; }; }
        @Override public void set(int i, int v) { if (i == 0) progress = v; }
        @Override public int getCount() { return 2; }
    };

    public IItemHandler getInventory() { return inventory; }

    public static void tick(Level lvl, BlockPos pos, BlockState st, RiftForgeBlockEntity be) {
        if (lvl.isClientSide) return;
        if (!RiftForgeRecipes.matches(be.inventory) || !be.canOutput()) {
            if (be.progress != 0) { be.progress = 0; be.setChanged(); }
            return;
        }
        be.progress++;
        if (be.progress >= MAX_PROGRESS) {
            be.progress = 0;
            for (int i = 0; i < INPUT_SLOTS; i++) be.inventory.extractItem(i, 1, false);
            ItemStack out = be.inventory.getStackInSlot(OUTPUT_SLOT);
            if (out.isEmpty()) be.inventory.setStackInSlot(OUTPUT_SLOT, RiftForgeRecipes.result());
            else out.grow(1);
        }
        be.setChanged();
    }

    private boolean canOutput() {
        ItemStack out = inventory.getStackInSlot(OUTPUT_SLOT);
        return out.isEmpty() || (out.getItem() == RiftForgeRecipes.result().getItem() && out.getCount() < out.getMaxStackSize());
    }

    @Override public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) return lazyItems.cast();
        return super.getCapability(cap, side);
    }
    @Override public void onLoad() { super.onLoad(); lazyItems = LazyOptional.of(() -> inventory); }
    @Override public void invalidateCaps() { super.invalidateCaps(); lazyItems.invalidate(); }

    public void drops() {
        SimpleContainer c = new SimpleContainer(inventory.getSlots());
        for (int i = 0; i < inventory.getSlots(); i++) c.setItem(i, inventory.getStackInSlot(i));
        Containers.dropContents(level, worldPosition, c);
    }

    @Override public @NotNull Component getDisplayName() { return Component.translatable("block.liberthia.rift_forge"); }
    @Override public @Nullable AbstractContainerMenu createMenu(int id, @NotNull Inventory inv, @NotNull Player p) {
        return new RiftForgeMenu(id, inv, this, this.data);
    }

    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("inv", inventory.serializeNBT());
        tag.putInt("progress", progress);
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("inv")) inventory.deserializeNBT(tag.getCompound("inv"));
        progress = tag.getInt("progress");
    }
}
