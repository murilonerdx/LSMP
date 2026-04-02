package br.com.murilo.liberthia.block.entity;

import br.com.murilo.liberthia.menu.PurificationBenchMenu;
import br.com.murilo.liberthia.registry.ModBlockEntities;
import br.com.murilo.liberthia.registry.ModBlocks;
import br.com.murilo.liberthia.registry.ModItems;
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
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PurificationBenchBlockEntity extends BlockEntity implements MenuProvider {
    private final ItemStackHandler inventory = new ItemStackHandler(2) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if (level != null && !level.isClientSide()) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }
    };

    private LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.empty();

    private int progress = 0;
    private int maxProgress = 200;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> progress;
                case 1 -> maxProgress;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> progress = value;
                case 1 -> maxProgress = value;
            }
        }

        @Override
        public int getCount() {
            return 2;
        }
    };

    public PurificationBenchBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PURIFICATION_BENCH.get(), pos, state);
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("container.liberthia.purification_bench");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, @NotNull Inventory playerInventory, @NotNull Player player) {
        return new PurificationBenchMenu(containerId, playerInventory, this, this.data);
    }

    public IItemHandler getItemHandler() {
        return inventory;
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return lazyItemHandler.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        lazyItemHandler = LazyOptional.of(() -> inventory);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyItemHandler.invalidate();
    }

    public void drops() {
        SimpleContainer container = new SimpleContainer(inventory.getSlots());
        for (int i = 0; i < inventory.getSlots(); i++) {
            container.setItem(i, inventory.getStackInSlot(i));
        }
        Containers.dropContents(this.level, this.worldPosition, container);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, PurificationBenchBlockEntity entity) {
        if (level.isClientSide) return;

        if (entity.hasRecipe()) {
            entity.progress++;
            entity.setChanged();
            if (entity.progress >= entity.maxProgress) {
                entity.completeProcess();
            }
        } else {
            entity.resetProgress();
        }
    }

    private boolean hasRecipe() {
        ItemStack input = inventory.getStackInSlot(0);
        if (input.isEmpty()) return false;

        ItemStack result = getResult(input);
        if (result.isEmpty()) return false;

        return canInsertIntoOutput(result);
    }

    private ItemStack getResult(ItemStack input) {
        if (input.is(ModItems.DARK_MATTER_SHARD.get())) {
            return new ItemStack(ModItems.YELLOW_MATTER_INGOT.get());
        } else if (input.is(ModBlocks.DARK_MATTER_BLOCK.get().asItem())) {
            return new ItemStack(ModBlocks.CLEAR_MATTER_BLOCK.get());
        }
        return ItemStack.EMPTY;
    }

    private boolean canInsertIntoOutput(ItemStack result) {
        ItemStack output = inventory.getStackInSlot(1);
        if (output.isEmpty()) return true;
        if (!ItemStack.isSameItemSameTags(output, result)) return false;
        return output.getCount() + result.getCount() <= output.getMaxStackSize();
    }

    private void completeProcess() {
        ItemStack input = inventory.getStackInSlot(0);
        ItemStack result = getResult(input);
        if (result.isEmpty()) return;

        inventory.extractItem(0, 1, false);

        ItemStack output = inventory.getStackInSlot(1);
        if (output.isEmpty()) {
            inventory.setStackInSlot(1, result.copy());
        } else {
            output.grow(result.getCount());
        }

        resetProgress();
    }

    private void resetProgress() {
        progress = 0;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        tag.put("inventory", inventory.serializeNBT());
        tag.putInt("progress", progress);
        super.saveAdditional(tag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        // Backwards compatibility: migrate old "input" format
        if (tag.contains("inventory")) {
            inventory.deserializeNBT(tag.getCompound("inventory"));
        } else if (tag.contains("input")) {
            ItemStack oldInput = ItemStack.of(tag.getCompound("input"));
            inventory.setStackInSlot(0, oldInput);
        }
        progress = tag.getInt("progress");
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }
}
