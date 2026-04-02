package br.com.murilo.liberthia.block.entity;

import br.com.murilo.liberthia.menu.ContainmentChamberMenu;
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

public class ContainmentChamberBlockEntity extends BlockEntity implements MenuProvider {
    // Slot 0-1: inputs, Slot 2: containment (clear matter), Slot 3: output
    private final ItemStackHandler inventory = new ItemStackHandler(4) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if (level != null && !level.isClientSide()) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            if (slot == 2) return stack.is(ModBlocks.CLEAR_MATTER_BLOCK.get().asItem());
            if (slot == 3) return false;
            return super.isItemValid(slot, stack);
        }
    };

    private LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.empty();
    private int progress = 0;
    private int maxProgress = 400;
    private int stability = 100;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> progress;
                case 1 -> maxProgress;
                case 2 -> stability;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> progress = value;
                case 1 -> maxProgress = value;
                case 2 -> stability = value;
            }
        }

        @Override
        public int getCount() { return 3; }
    };

    public ContainmentChamberBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CONTAINMENT_CHAMBER.get(), pos, state);
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("container.liberthia.containment_chamber");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, @NotNull Inventory inv, @NotNull Player player) {
        return new ContainmentChamberMenu(containerId, inv, this, this.data);
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

    public static void tick(Level level, BlockPos pos, BlockState state, ContainmentChamberBlockEntity entity) {
        if (level.isClientSide) return;

        if (entity.hasRecipe()) {
            // Check containment
            boolean hasContainment = !entity.inventory.getStackInSlot(2).isEmpty();
            if (!hasContainment && entity.progress > 0) {
                entity.stability -= 5;
                if (entity.stability <= 0) {
                    entity.stability = 0;
                    // Containment failure - explosion!
                    entity.progress = 0;
                    level.explode(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                            3.0F, Level.ExplosionInteraction.BLOCK);
                    level.destroyBlock(pos, false);
                    return;
                }
            }

            entity.progress++;
            entity.setChanged();

            if (entity.progress >= entity.maxProgress) {
                entity.completeProcess();
            }
        } else {
            if (entity.progress > 0) entity.progress = 0;
            // Slowly restore stability when idle
            if (entity.stability < 100) {
                entity.stability = Math.min(100, entity.stability + 1);
            }
        }
    }

    private boolean hasRecipe() {
        ItemStack result = getResult();
        return !result.isEmpty() && canInsertIntoOutput(result);
    }

    private ItemStack getResult() {
        ItemStack input1 = inventory.getStackInSlot(0);
        ItemStack input2 = inventory.getStackInSlot(1);

        // Dark Matter Block + Dark Matter Shard -> Void Crystal (placeholder)
        if (input1.is(ModBlocks.DARK_MATTER_BLOCK.get().asItem()) && input2.is(ModItems.DARK_MATTER_SHARD.get())) {
            return new ItemStack(ModItems.DARK_MATTER_SHARD.get(), 4);
        }
        // Yellow Matter Block + Holy Essence -> Singularity Core (placeholder as Holy Essence x3)
        if (input1.is(ModBlocks.YELLOW_MATTER_BLOCK.get().asItem()) && input2.is(ModItems.HOLY_ESSENCE.get())) {
            return new ItemStack(ModItems.HOLY_ESSENCE.get(), 3);
        }
        return ItemStack.EMPTY;
    }

    private boolean canInsertIntoOutput(ItemStack result) {
        ItemStack output = inventory.getStackInSlot(3);
        if (output.isEmpty()) return true;
        if (!ItemStack.isSameItemSameTags(output, result)) return false;
        return output.getCount() + result.getCount() <= output.getMaxStackSize();
    }

    private void completeProcess() {
        ItemStack result = getResult();
        if (result.isEmpty()) return;
        inventory.extractItem(0, 1, false);
        inventory.extractItem(1, 1, false);
        // Consume 1 containment clear matter
        inventory.extractItem(2, 1, false);
        // Reset stability on successful craft
        stability = 100;

        ItemStack output = inventory.getStackInSlot(3);
        if (output.isEmpty()) {
            inventory.setStackInSlot(3, result.copy());
        } else {
            output.grow(result.getCount());
        }
        progress = 0;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        tag.put("inventory", inventory.serializeNBT());
        tag.putInt("progress", progress);
        tag.putInt("stability", stability);
        super.saveAdditional(tag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("inventory")) inventory.deserializeNBT(tag.getCompound("inventory"));
        progress = tag.getInt("progress");
        stability = tag.contains("stability") ? tag.getInt("stability") : 100;
    }

    @Nullable @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override
    public CompoundTag getUpdateTag() { return saveWithoutMetadata(); }
}
