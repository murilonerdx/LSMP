package br.com.murilo.liberthia.menu;

import br.com.murilo.liberthia.block.entity.DarkMatterForgeBlockEntity;
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

public class DarkMatterForgeMenu extends AbstractContainerMenu {
    private final DarkMatterForgeBlockEntity blockEntity;
    private final Level level;
    private final ContainerData data;

    // Client constructor
    public DarkMatterForgeMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory, playerInventory.player.level().getBlockEntity(extraData.readBlockPos()), new SimpleContainerData(4));
    }

    // Server constructor
    public DarkMatterForgeMenu(int containerId, Inventory playerInventory, BlockEntity blockEntity, ContainerData data) {
        super(ModMenuTypes.DARK_MATTER_FORGE.get(), containerId);
        this.blockEntity = (DarkMatterForgeBlockEntity) blockEntity;
        this.level = playerInventory.player.level();
        this.data = data;

        addPlayerInventory(playerInventory);
        addPlayerHotbar(playerInventory);

        IItemHandler handler = this.blockEntity.getItemHandler();
        // Fuel slot
        this.addSlot(new SlotItemHandler(handler, 0, 18, 50));
        // Input slot 1
        this.addSlot(new SlotItemHandler(handler, 1, 66, 16));
        // Input slot 2
        this.addSlot(new SlotItemHandler(handler, 2, 66, 50));
        // Output slot
        this.addSlot(new SlotItemHandler(handler, 3, 124, 33) {
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

    public boolean hasFuel() {
        return data.get(2) > 0;
    }

    public int getScaledProgress() {
        int progress = data.get(0);
        int maxProgress = data.get(1);
        int arrowSize = 24;
        return maxProgress != 0 && progress != 0 ? progress * arrowSize / maxProgress : 0;
    }

    public int getScaledFuelProgress() {
        int fuelTime = data.get(2);
        int maxFuelTime = data.get(3);
        int flameSize = 14;
        return maxFuelTime != 0 ? fuelTime * flameSize / maxFuelTime : 0;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        Slot slot = this.slots.get(slotIndex);
        if (!slot.hasItem()) return ItemStack.EMPTY;

        ItemStack slotStack = slot.getItem();
        ItemStack originalStack = slotStack.copy();

        int playerStart = 0;
        int playerEnd = 36;
        int fuelSlot = 36;
        int input1Slot = 37;
        int input2Slot = 38;
        int outputSlot = 39;

        if (slotIndex == outputSlot) {
            if (!this.moveItemStackTo(slotStack, playerStart, playerEnd, true)) return ItemStack.EMPTY;
            slot.onQuickCraft(slotStack, originalStack);
        } else if (slotIndex >= fuelSlot && slotIndex <= input2Slot) {
            if (!this.moveItemStackTo(slotStack, playerStart, playerEnd, true)) return ItemStack.EMPTY;
        } else if (slotIndex < playerEnd) {
            // Try fuel first, then inputs
            if (!this.moveItemStackTo(slotStack, fuelSlot, fuelSlot + 1, false)) {
                if (!this.moveItemStackTo(slotStack, input1Slot, input2Slot + 1, false)) {
                    return ItemStack.EMPTY;
                }
            }
        }

        if (slotStack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        if (slotStack.getCount() == originalStack.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, slotStack);
        return originalStack;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(level, blockEntity.getBlockPos()), player, ModBlocks.DARK_MATTER_FORGE.get());
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
