package br.com.murilo.liberthia.menu;

import br.com.murilo.liberthia.block.entity.PurificationBenchBlockEntity;
import br.com.murilo.liberthia.registry.ModBlocks;
import br.com.murilo.liberthia.registry.ModMenuTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;

public class PurificationBenchMenu extends AbstractContainerMenu {
    private final PurificationBenchBlockEntity blockEntity;
    private final Level level;
    private final ContainerData data;

    // Client constructor
    public PurificationBenchMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory, playerInventory.player.level().getBlockEntity(extraData.readBlockPos()), new SimpleContainerData(2));
    }

    // Server constructor
    public PurificationBenchMenu(int containerId, Inventory playerInventory, BlockEntity blockEntity, ContainerData data) {
        super(ModMenuTypes.PURIFICATION_BENCH.get(), containerId);
        checkContainerSize(playerInventory, 2);
        this.blockEntity = (PurificationBenchBlockEntity) blockEntity;
        this.level = playerInventory.player.level();
        this.data = data;

        addPlayerInventory(playerInventory);
        addPlayerHotbar(playerInventory);

        IItemHandler handler = this.blockEntity.getItemHandler();
        // Input slot
        this.addSlot(new SlotItemHandler(handler, 0, 80, 11));
        // Output slot - cannot place items into
        this.addSlot(new SlotItemHandler(handler, 1, 80, 59) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });

        addDataSlots(data);
    }

    public boolean isCrafting() {
        return data.get(0) > 0;
    }

    public int getScaledProgress() {
        int progress = data.get(0);
        int maxProgress = data.get(1);
        int progressArrowSize = 26; // height of the arrow in pixels

        return maxProgress != 0 && progress != 0 ? progress * progressArrowSize / maxProgress : 0;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        Slot slot = this.slots.get(slotIndex);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack slotStack = slot.getItem();
        ItemStack originalStack = slotStack.copy();

        int inventoryStart = 0;
        int inventoryEnd = 36; // 27 inventory + 9 hotbar
        int inputSlot = 36;
        int outputSlot = 37;

        if (slotIndex == outputSlot) {
            // Output -> player inventory
            if (!this.moveItemStackTo(slotStack, inventoryStart, inventoryEnd, true)) {
                return ItemStack.EMPTY;
            }
            slot.onQuickCraft(slotStack, originalStack);
        } else if (slotIndex == inputSlot) {
            // Input -> player inventory
            if (!this.moveItemStackTo(slotStack, inventoryStart, inventoryEnd, true)) {
                return ItemStack.EMPTY;
            }
        } else if (slotIndex < inventoryEnd) {
            // Player inventory -> input slot
            if (!this.moveItemStackTo(slotStack, inputSlot, inputSlot + 1, false)) {
                return ItemStack.EMPTY;
            }
        }

        if (slotStack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        if (slotStack.getCount() == originalStack.getCount()) {
            return ItemStack.EMPTY;
        }

        slot.onTake(player, slotStack);
        return originalStack;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(level, blockEntity.getBlockPos()), player, ModBlocks.PURIFICATION_BENCH.get());
    }

    private void addPlayerInventory(Inventory playerInventory) {
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
    }

    private void addPlayerHotbar(Inventory playerInventory) {
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }
    }
}
