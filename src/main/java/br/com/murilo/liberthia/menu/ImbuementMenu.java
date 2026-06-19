package br.com.murilo.liberthia.menu;

import br.com.murilo.liberthia.observation.block.ImbuementBlockEntity;
import br.com.murilo.liberthia.registry.ModBlocks;
import br.com.murilo.liberthia.registry.ModMenuTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;

public class ImbuementMenu extends AbstractContainerMenu {
    private final ImbuementBlockEntity blockEntity;
    private final Level level;

    public ImbuementMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory,
            playerInventory.player.level().getBlockEntity(extraData.readBlockPos()));
    }

    public ImbuementMenu(int containerId, Inventory playerInventory, BlockEntity be) {
        super(ModMenuTypes.IMBUEMENT_TABLE.get(), containerId);
        this.blockEntity = (ImbuementBlockEntity) be;
        this.level = playerInventory.player.level();

        addPlayerInventory(playerInventory);
        addPlayerHotbar(playerInventory);

        IItemHandler h = blockEntity.getItemHandler();
        addSlot(new SlotItemHandler(h, ImbuementBlockEntity.SLOT_SWORD, 30, 40));
        addSlot(new SlotItemHandler(h, ImbuementBlockEntity.SLOT_PARCHMENT, 60, 40));
        addSlot(new SlotItemHandler(h, ImbuementBlockEntity.SLOT_FRAGMENT, 90, 40));
        addSlot(new SlotItemHandler(h, ImbuementBlockEntity.SLOT_OUTPUT, 132, 40) {
            @Override public boolean mayPlace(ItemStack s) { return false; }
            @Override
            public void onTake(Player player, ItemStack stack) {
                super.onTake(player, stack);
                blockEntity.onOutputTaken();
            }
        });
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        Slot slot = this.slots.get(slotIndex);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stk = slot.getItem();
        ItemStack orig = stk.copy();
        int playerEnd = 36;
        int tableStart = 36;
        int tableEnd = 40;
        if (slotIndex < playerEnd) {
            if (!moveItemStackTo(stk, tableStart, tableEnd, false)) return ItemStack.EMPTY;
        } else {
            if (slotIndex == 39) {
                if (!moveItemStackTo(stk, 0, playerEnd, true)) return ItemStack.EMPTY;
                slot.onTake(player, stk);
            } else {
                if (!moveItemStackTo(stk, 0, playerEnd, false)) return ItemStack.EMPTY;
            }
        }
        if (stk.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        if (stk.getCount() == orig.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stk);
        return orig;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(level, blockEntity.getBlockPos()),
            player, ModBlocks.IMBUEMENT_TABLE.get());
    }

    private void addPlayerInventory(Inventory playerInventory) {
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
    }

    private void addPlayerHotbar(Inventory playerInventory) {
        for (int col = 0; col < 9; col++)
            addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
    }
}
