package br.com.murilo.liberthia.block.entity;

import br.com.murilo.liberthia.menu.MatterInfuserMenu;
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

public class MatterInfuserBlockEntity extends BlockEntity implements MenuProvider {
    // Slot 0: dark matter input, 1: clear matter input, 2: yellow matter input, 3: catalyst, 4: output
    private final ItemStackHandler inventory = new ItemStackHandler(5) {
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

    public MatterInfuserBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MATTER_INFUSER.get(), pos, state);
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("container.liberthia.matter_infuser");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, @NotNull Inventory inv, @NotNull Player player) {
        return new MatterInfuserMenu(containerId, inv, this, this.data);
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

    public static void tick(Level level, BlockPos pos, BlockState state, MatterInfuserBlockEntity entity) {
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
        ItemStack dark = inventory.getStackInSlot(0);
        ItemStack clear = inventory.getStackInSlot(1);
        ItemStack yellow = inventory.getStackInSlot(2);
        ItemStack catalyst = inventory.getStackInSlot(3);

        // All 3 matter blocks + Singularity Core -> Matter Core
        // The Infuser is the ONLY machine that can safely combine all three matters.
        // Clear Matter acts as a bridge between Dark and Yellow (which repel each other).
        // The Singularity Core contains the infused dark energy needed to stabilize the fusion.
        if (dark.is(ModBlocks.DARK_MATTER_BLOCK.get().asItem())
                && clear.is(ModBlocks.CLEAR_MATTER_BLOCK.get().asItem())
                && yellow.is(ModBlocks.YELLOW_MATTER_BLOCK.get().asItem())
                && catalyst.is(ModItems.SINGULARITY_CORE.get())) {
            return new ItemStack(ModItems.MATTER_CORE.get());
        }
        // Dark Shard + Clear Block + Holy Essence (no yellow) -> Purified Essence x3
        // Dark + Clear creates a hostile consciousness, but Holy Essence purifies it
        if (dark.is(ModItems.DARK_MATTER_SHARD.get())
                && clear.is(ModBlocks.CLEAR_MATTER_BLOCK.get().asItem())
                && yellow.isEmpty()
                && catalyst.is(ModItems.HOLY_ESSENCE.get())) {
            return new ItemStack(ModItems.PURIFIED_ESSENCE.get(), 3);
        }
        // Clear Block + Yellow Ingot + Holy Essence (no dark) -> Clear Matter Pill x4
        // Clear + Yellow amplifies emotions but preserves the mind; with Holy Essence it becomes medicine
        if (dark.isEmpty()
                && clear.is(ModBlocks.CLEAR_MATTER_BLOCK.get().asItem())
                && yellow.is(ModItems.YELLOW_MATTER_INGOT.get())
                && catalyst.is(ModItems.HOLY_ESSENCE.get())) {
            return new ItemStack(ModItems.CLEAR_MATTER_PILL.get(), 4);
        }
        return ItemStack.EMPTY;
    }

    private boolean canInsertIntoOutput(ItemStack result) {
        ItemStack output = inventory.getStackInSlot(4);
        if (output.isEmpty()) return true;
        if (!ItemStack.isSameItemSameTags(output, result)) return false;
        return output.getCount() + result.getCount() <= output.getMaxStackSize();
    }

    private void completeProcess() {
        ItemStack result = getResult();
        if (result.isEmpty()) return;
        // Consume 1 from each non-empty input
        for (int i = 0; i < 4; i++) {
            if (!inventory.getStackInSlot(i).isEmpty()) {
                inventory.extractItem(i, 1, false);
            }
        }
        ItemStack output = inventory.getStackInSlot(4);
        if (output.isEmpty()) {
            inventory.setStackInSlot(4, result.copy());
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
