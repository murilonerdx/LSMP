package br.com.murilo.liberthia.menu;

import br.com.murilo.liberthia.block.entity.ContainmentChamberBlockEntity;
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

public class ContainmentChamberMenu extends AbstractContainerMenu {
    private final ContainmentChamberBlockEntity blockEntity;
    private final Level level;
    private final ContainerData data;

    public ContainmentChamberMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory, playerInventory.player.level().getBlockEntity(extraData.readBlockPos()), new SimpleContainerData(3));
    }

    public ContainmentChamberMenu(int containerId, Inventory playerInventory, BlockEntity blockEntity, ContainerData data) {
        super(ModMenuTypes.CONTAINMENT_CHAMBER.get(), containerId);
        this.blockEntity = (ContainmentChamberBlockEntity) blockEntity;
        this.level = playerInventory.player.level();
        this.data = data;

        addPlayerInventory(playerInventory);
        addPlayerHotbar(playerInventory);

        IItemHandler handler = this.blockEntity.getItemHandler();
        // Input 1
        this.addSlot(new SlotItemHandler(handler, 0, 34, 17));
        // Input 2
        this.addSlot(new SlotItemHandler(handler, 1, 56, 17));
        // Containment slot (clear matter only)
        this.addSlot(new SlotItemHandler(handler, 2, 45, 53));
        // Output
        this.addSlot(new SlotItemHandler(handler, 3, 124, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) { return false; }
        });

        addDataSlots(data);
    }

    public boolean isCrafting() { return data.get(0) > 0; }

    public int getScaledProgress() {
        int progress = data.get(0);
        int maxProgress = data.get(1);
        int arrowSize = 24;
        return maxProgress != 0 && progress != 0 ? progress * arrowSize / maxProgress : 0;
    }

    public int getStability() { return data.get(2); }

    public int getScaledStability() {
        int stability = data.get(2);
        int barHeight = 42;
        return stability * barHeight / 100;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        Slot slot = this.slots.get(slotIndex);
        if (!slot.hasItem()) return ItemStack.EMPTY;

        ItemStack slotStack = slot.getItem();
        ItemStack originalStack = slotStack.copy();

        if (slotIndex >= 36) {
            if (!this.moveItemStackTo(slotStack, 0, 36, true)) return ItemStack.EMPTY;
            if (slotIndex == 39) slot.onQuickCraft(slotStack, originalStack);
        } else {
            if (!this.moveItemStackTo(slotStack, 36, 39, false)) return ItemStack.EMPTY;
        }

        if (slotStack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        if (slotStack.getCount() == originalStack.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, slotStack);
        return originalStack;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(level, blockEntity.getBlockPos()), player, ModBlocks.CONTAINMENT_CHAMBER.get());
    }

    private void addPlayerInventory(Inventory playerInventory) {
        for (int row = 0; row < 3; ++row)
            for (int col = 0; col < 9; ++col)
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
    }

    private void addPlayerHotbar(Inventory playerInventory) {
        for (int col = 0; col < 9; ++col)
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
    }
}
