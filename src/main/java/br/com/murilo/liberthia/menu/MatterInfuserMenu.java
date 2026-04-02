package br.com.murilo.liberthia.menu;

import br.com.murilo.liberthia.block.entity.MatterInfuserBlockEntity;
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

public class MatterInfuserMenu extends AbstractContainerMenu {
    private final MatterInfuserBlockEntity blockEntity;
    private final Level level;
    private final ContainerData data;

    public MatterInfuserMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory, playerInventory.player.level().getBlockEntity(extraData.readBlockPos()), new SimpleContainerData(2));
    }

    public MatterInfuserMenu(int containerId, Inventory playerInventory, BlockEntity blockEntity, ContainerData data) {
        super(ModMenuTypes.MATTER_INFUSER.get(), containerId);
        this.blockEntity = (MatterInfuserBlockEntity) blockEntity;
        this.level = playerInventory.player.level();
        this.data = data;

        addPlayerInventory(playerInventory);
        addPlayerHotbar(playerInventory);

        IItemHandler handler = this.blockEntity.getItemHandler();
        // Dark matter input
        this.addSlot(new SlotItemHandler(handler, 0, 30, 17));
        // Clear matter input
        this.addSlot(new SlotItemHandler(handler, 1, 52, 17));
        // Yellow matter input
        this.addSlot(new SlotItemHandler(handler, 2, 74, 17));
        // Catalyst
        this.addSlot(new SlotItemHandler(handler, 3, 52, 53));
        // Output
        this.addSlot(new SlotItemHandler(handler, 4, 124, 35) {
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

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        Slot slot = this.slots.get(slotIndex);
        if (!slot.hasItem()) return ItemStack.EMPTY;

        ItemStack slotStack = slot.getItem();
        ItemStack originalStack = slotStack.copy();

        if (slotIndex >= 36) {
            if (!this.moveItemStackTo(slotStack, 0, 36, true)) return ItemStack.EMPTY;
            if (slotIndex == 40) slot.onQuickCraft(slotStack, originalStack);
        } else {
            if (!this.moveItemStackTo(slotStack, 36, 40, false)) return ItemStack.EMPTY;
        }

        if (slotStack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        if (slotStack.getCount() == originalStack.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, slotStack);
        return originalStack;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(level, blockEntity.getBlockPos()), player, ModBlocks.MATTER_INFUSER.get());
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
