package br.com.murilo.liberthia.block.entity;

import br.com.murilo.liberthia.block.DarkMatterForgeBlock;
import br.com.murilo.liberthia.capability.IInfectionData;
import br.com.murilo.liberthia.menu.DarkMatterForgeMenu;
import br.com.murilo.liberthia.registry.ModBlockEntities;
import br.com.murilo.liberthia.registry.ModBlocks;
import br.com.murilo.liberthia.registry.ModCapabilities;
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
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DarkMatterForgeBlockEntity extends BlockEntity implements MenuProvider {
    // Slot 0: fuel, Slot 1-2: inputs, Slot 3: output
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
            return switch (slot) {
                case 0 -> isFuel(stack); // Fuel slot
                case 1, 2 -> isValidInput(stack); // Input slots
                case 3 -> false; // Output slot — no player insertion
                default -> false;
            };
        }
    };

    private LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.empty();

    private int progress = 0;
    private int maxProgress = 200;
    private int fuelTime = 0;
    private int maxFuelTime = 0;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> progress;
                case 1 -> maxProgress;
                case 2 -> fuelTime;
                case 3 -> maxFuelTime;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> progress = value;
                case 1 -> maxProgress = value;
                case 2 -> fuelTime = value;
                case 3 -> maxFuelTime = value;
            }
        }

        @Override
        public int getCount() {
            return 4;
        }
    };

    public DarkMatterForgeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DARK_MATTER_FORGE.get(), pos, state);
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("container.liberthia.dark_matter_forge");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, @NotNull Inventory playerInventory, @NotNull Player player) {
        return new DarkMatterForgeMenu(containerId, playerInventory, this, this.data);
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

    public static void tick(Level level, BlockPos pos, BlockState state, DarkMatterForgeBlockEntity entity) {
        if (level.isClientSide) return;

        boolean wasLit = entity.fuelTime > 0;

        if (entity.fuelTime > 0) {
            entity.fuelTime--;
        }

        if (entity.hasRecipe()) {
            if (entity.fuelTime <= 0) {
                entity.consumeFuel();
            }

            if (entity.fuelTime > 0) {
                entity.progress++;
                entity.setChanged();

                if (entity.progress >= entity.maxProgress) {
                    entity.completeProcess();
                }

                // Spread infection to nearby players every 40 ticks
                if (level.getGameTime() % 40 == 0) {
                    entity.spreadInfection(level, pos);
                }
            } else {
                entity.resetProgress();
            }
        } else {
            entity.resetProgress();
        }

        boolean isLit = entity.fuelTime > 0;
        if (wasLit != isLit) {
            level.setBlock(pos, state.setValue(DarkMatterForgeBlock.LIT, isLit), 3);
        }
    }

    private void spreadInfection(Level level, BlockPos pos) {
        AABB area = new AABB(pos).inflate(8.0);
        level.getEntitiesOfClass(Player.class, area).forEach(player -> {
            player.getCapability(ModCapabilities.INFECTION).ifPresent(data -> {
                data.setInfection(data.getInfection() + 1);
                data.setDirty(true);
            });
        });
    }

    private void consumeFuel() {
        ItemStack fuel = inventory.getStackInSlot(0);
        if (fuel.isEmpty()) return;

        int burnTime = getFuelBurnTime(fuel);
        if (burnTime > 0) {
            fuelTime = burnTime;
            maxFuelTime = burnTime;
            inventory.extractItem(0, 1, false);
        }
    }

    private int getFuelBurnTime(ItemStack stack) {
        if (stack.is(ModItems.DARK_MATTER_SHARD.get())) return 200;
        if (stack.is(ModItems.DARK_MATTER_BUCKET.get())) return 1600;
        if (stack.is(ModBlocks.DARK_MATTER_BLOCK.get().asItem())) return 800;
        return 0;
    }

    private boolean hasRecipe() {
        ItemStack input1 = inventory.getStackInSlot(1);
        ItemStack input2 = inventory.getStackInSlot(2);

        ItemStack result = getResult(input1, input2);
        if (result.isEmpty()) return false;

        return canInsertIntoOutput(result);
    }

    private ItemStack getResult(ItemStack input1, ItemStack input2) {
        // Dark Matter Shard + Iron Ingot -> Stabilized Dark Matter
        // Dark matter distorts reality; iron stabilizes its chaotic energy
        if (input1.is(ModItems.DARK_MATTER_SHARD.get()) && input2.is(net.minecraft.world.item.Items.IRON_INGOT)) {
            return new ItemStack(ModItems.STABILIZED_DARK_MATTER.get());
        }
        // Dark Matter Shard + Netherite Scrap -> Void Crystal
        // The forge concentrates dark matter's reality-warping into crystalline form
        if (input1.is(ModItems.DARK_MATTER_SHARD.get()) && input2.is(net.minecraft.world.item.Items.NETHERITE_SCRAP)) {
            return new ItemStack(ModItems.VOID_CRYSTAL.get());
        }
        // Stabilized Dark Matter + Void Crystal -> Singularity Core
        // Two refined dark matter products fused into a controlled singularity
        if (input1.is(ModItems.STABILIZED_DARK_MATTER.get()) && input2.is(ModItems.VOID_CRYSTAL.get())) {
            return new ItemStack(ModItems.SINGULARITY_CORE.get());
        }
        // Dark Matter Block + Holy Essence -> Purified Essence
        // Holy Essence purifies the dark matter's hostile energy
        if (input1.is(ModBlocks.DARK_MATTER_BLOCK.get().asItem()) && input2.is(ModItems.HOLY_ESSENCE.get())) {
            return new ItemStack(ModItems.PURIFIED_ESSENCE.get(), 2);
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
        ItemStack input1 = inventory.getStackInSlot(1);
        ItemStack input2 = inventory.getStackInSlot(2);
        ItemStack result = getResult(input1, input2);
        if (result.isEmpty()) return;

        inventory.extractItem(1, 1, false);
        inventory.extractItem(2, 1, false);

        ItemStack output = inventory.getStackInSlot(3);
        if (output.isEmpty()) {
            inventory.setStackInSlot(3, result.copy());
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
        tag.putInt("fuelTime", fuelTime);
        tag.putInt("maxFuelTime", maxFuelTime);
        super.saveAdditional(tag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("inventory")) {
            inventory.deserializeNBT(tag.getCompound("inventory"));
        }
        progress = tag.getInt("progress");
        fuelTime = tag.getInt("fuelTime");
        maxFuelTime = tag.getInt("maxFuelTime");
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

    private static boolean isFuel(ItemStack stack) {
        return stack.is(ModItems.DARK_MATTER_SHARD.get())
                || stack.is(ModItems.DARK_MATTER_BUCKET.get())
                || stack.is(ModBlocks.DARK_MATTER_BLOCK.get().asItem());
    }

    private static boolean isValidInput(ItemStack stack) {
        return stack.is(ModItems.DARK_MATTER_SHARD.get())
                || stack.is(net.minecraft.world.item.Items.IRON_INGOT)
                || stack.is(net.minecraft.world.item.Items.NETHERITE_SCRAP)
                || stack.is(ModItems.STABILIZED_DARK_MATTER.get())
                || stack.is(ModItems.VOID_CRYSTAL.get())
                || stack.is(ModBlocks.DARK_MATTER_BLOCK.get().asItem())
                || stack.is(ModItems.HOLY_ESSENCE.get());
    }
}
