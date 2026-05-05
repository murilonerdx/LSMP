package br.com.murilo.liberthia.menu;

import br.com.murilo.liberthia.registry.ModMenuTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import br.com.murilo.liberthia.init.SpiritualTradeData;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class SpiritualTradeConfigMenu extends AbstractContainerMenu {

    private static final int TRADE_CONTAINER_SIZE = 6;

    private final SimpleContainer tradeContainer;
    private final ItemStack connectionStack;

    public SpiritualTradeConfigMenu(int containerId, Inventory playerInventory, ItemStack connectionStack) {
        super(ModMenuTypes.SPIRITUAL_TRADE_CONFIG.get(), containerId);

        this.connectionStack = connectionStack;
        this.tradeContainer = new SimpleContainer(TRADE_CONTAINER_SIZE);

        loadTrades();
        addTradeSlots();
        addPlayerInventorySlots(playerInventory);
    }

    private void loadTrades() {
        List<ItemStack> stacks = SpiritualTradeData.loadTrades(connectionStack);

        for (int i = 0; i < TRADE_CONTAINER_SIZE; i++) {
            tradeContainer.setItem(i, stacks.get(i));
        }
    }

    private void saveTrades() {
        List<ItemStack> stacks = new ArrayList<>();

        for (int i = 0; i < TRADE_CONTAINER_SIZE; i++) {
            stacks.add(tradeContainer.getItem(i).copy());
        }

        SpiritualTradeData.saveTrades(connectionStack, stacks);
    }

    private void addTradeSlots() {
        this.addSlot(new GhostSlot(tradeContainer, 0, 35, 20));
        this.addSlot(new GhostSlot(tradeContainer, 1, 125, 20));

        this.addSlot(new GhostSlot(tradeContainer, 2, 35, 44));
        this.addSlot(new GhostSlot(tradeContainer, 3, 125, 44));

        this.addSlot(new GhostSlot(tradeContainer, 4, 35, 68));
        this.addSlot(new GhostSlot(tradeContainer, 5, 125, 68));
    }

    private void addPlayerInventorySlots(Inventory playerInventory) {
        int inventoryStartY = 102;

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                this.addSlot(new Slot(
                        playerInventory,
                        column + row * 9 + 9,
                        8 + column * 18,
                        inventoryStartY + row * 18
                ));
            }
        }

        int hotbarY = 160;

        for (int column = 0; column < 9; column++) {
            this.addSlot(new Slot(
                    playerInventory,
                    column,
                    8 + column * 18,
                    hotbarY
            ));
        }
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (slotId >= 0 && slotId < TRADE_CONTAINER_SIZE) {
            ItemStack carried = this.getCarried();

            if (carried.isEmpty()) {
                tradeContainer.setItem(slotId, ItemStack.EMPTY);
                saveTrades();
                broadcastChanges();
                return;
            }

            ItemStack copy = carried.copy();

            if (button == 1) {
                copy.setCount(1);
            }

            tradeContainer.setItem(slotId, copy);
            saveTrades();
            broadcastChanges();
            return;
        }

        super.clicked(slotId, button, clickType, player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        saveTrades();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return !connectionStack.isEmpty();
    }

    private static class GhostSlot extends Slot {

        public GhostSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public boolean mayPickup(Player player) {
            return false;
        }
    }
}