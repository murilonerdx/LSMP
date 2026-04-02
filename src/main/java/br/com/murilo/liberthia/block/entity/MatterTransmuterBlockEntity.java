package br.com.murilo.liberthia.block.entity;

import br.com.murilo.liberthia.menu.MatterTransmuterMenu;
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

public class MatterTransmuterBlockEntity extends BlockEntity implements MenuProvider {
    // Slot 0: input, Slot 1: catalyst, Slot 2: output
    private final ItemStackHandler inventory = new ItemStackHandler(3) {
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
    private int maxProgress = 300;

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
        public int getCount() { return 2; }
    };

    public MatterTransmuterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MATTER_TRANSMUTER.get(), pos, state);
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("container.liberthia.matter_transmuter");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, @NotNull Inventory inv, @NotNull Player player) {
        return new MatterTransmuterMenu(containerId, inv, this, this.data);
    }

    public IItemHandler getItemHandler() { return inventory; }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) return lazyItemHandler.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void onLoad() { super.onLoad(); lazyItemHandler = LazyOptional.of(() -> inventory); }

    @Override
    public void invalidateCaps() { super.invalidateCaps(); lazyItemHandler.invalidate(); }

    public void drops() {
        SimpleContainer c = new SimpleContainer(inventory.getSlots());
        for (int i = 0; i < inventory.getSlots(); i++) c.setItem(i, inventory.getStackInSlot(i));
        Containers.dropContents(this.level, this.worldPosition, c);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MatterTransmuterBlockEntity entity) {
        if (level.isClientSide) return;
        if (entity.hasRecipe()) {
            entity.progress++;
            entity.setChanged();
            if (entity.progress >= entity.maxProgress) entity.completeProcess();
        } else {
            entity.progress = 0;
        }
    }

    private boolean hasRecipe() {
        ItemStack result = getResult();
        return !result.isEmpty() && canInsertIntoOutput(result);
    }

    private ItemStack getResult() {
        ItemStack input = inventory.getStackInSlot(0);
        ItemStack catalyst = inventory.getStackInSlot(1);
        if (input.isEmpty() || catalyst.isEmpty()) return ItemStack.EMPTY;

        // Dark Matter -> Clear Matter (catalyst: Holy Essence)
        if (input.is(ModBlocks.DARK_MATTER_BLOCK.get().asItem()) && catalyst.is(ModItems.HOLY_ESSENCE.get())) {
            return new ItemStack(ModBlocks.CLEAR_MATTER_BLOCK.get());
        }
        // Clear Matter -> Yellow Matter (catalyst: Yellow Matter Ingot)
        if (input.is(ModBlocks.CLEAR_MATTER_BLOCK.get().asItem()) && catalyst.is(ModItems.YELLOW_MATTER_INGOT.get())) {
            return new ItemStack(ModBlocks.YELLOW_MATTER_BLOCK.get());
        }
        // Yellow Matter -> Dark Matter (catalyst: Dark Matter Shard)
        if (input.is(ModBlocks.YELLOW_MATTER_BLOCK.get().asItem()) && catalyst.is(ModItems.DARK_MATTER_SHARD.get())) {
            return new ItemStack(ModBlocks.DARK_MATTER_BLOCK.get());
        }
        return ItemStack.EMPTY;
    }

    private boolean canInsertIntoOutput(ItemStack result) {
        ItemStack output = inventory.getStackInSlot(2);
        if (output.isEmpty()) return true;
        if (!ItemStack.isSameItemSameTags(output, result)) return false;
        return output.getCount() + result.getCount() <= output.getMaxStackSize();
    }

    private void completeProcess() {
        ItemStack result = getResult();
        if (result.isEmpty()) return;
        inventory.extractItem(0, 1, false);
        inventory.extractItem(1, 1, false);
        ItemStack output = inventory.getStackInSlot(2);
        if (output.isEmpty()) {
            inventory.setStackInSlot(2, result.copy());
        } else {
            output.grow(result.getCount());
        }
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
        if (tag.contains("inventory")) inventory.deserializeNBT(tag.getCompound("inventory"));
        progress = tag.getInt("progress");
    }

    @Nullable @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override
    public CompoundTag getUpdateTag() { return saveWithoutMetadata(); }
}
