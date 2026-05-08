package br.com.murilo.liberthia.block.entity;

import br.com.murilo.liberthia.menu.DarkMatterChestMenu;
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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Bau de matéria escura — 9×6 = 54 slots. */
public class DarkMatterChestBlockEntity extends BlockEntity implements MenuProvider {

    public static final int ROWS = 6;
    public static final int COLS = 9;
    public static final int SIZE = ROWS * COLS;

    private final ItemStackHandler inventory = new ItemStackHandler(SIZE) {
        @Override protected void onContentsChanged(int slot) { setChanged(); }
    };
    private LazyOptional<IItemHandler> lazy = LazyOptional.empty();

    public DarkMatterChestBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DARK_MATTER_CHEST.get(), pos, state);
    }

    public IItemHandler getInventory() { return inventory; }

    @Override public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) return lazy.cast();
        return super.getCapability(cap, side);
    }
    @Override public void onLoad() { super.onLoad(); lazy = LazyOptional.of(() -> inventory); }
    @Override public void invalidateCaps() { super.invalidateCaps(); lazy.invalidate(); }

    public void drops() {
        SimpleContainer c = new SimpleContainer(inventory.getSlots());
        for (int i = 0; i < inventory.getSlots(); i++) c.setItem(i, inventory.getStackInSlot(i));
        Containers.dropContents(level, worldPosition, c);
    }

    @Override public @NotNull Component getDisplayName() {
        return Component.translatable("container.liberthia.dark_matter_chest");
    }
    @Override public @Nullable AbstractContainerMenu createMenu(int id, @NotNull Inventory inv, @NotNull Player p) {
        return new DarkMatterChestMenu(id, inv, this);
    }

    @Override protected void saveAdditional(CompoundTag tag) {
        tag.put("inv", inventory.serializeNBT());
        super.saveAdditional(tag);
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("inv")) inventory.deserializeNBT(tag.getCompound("inv"));
    }
}
