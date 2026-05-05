package br.com.murilo.liberthia.menu;

import br.com.murilo.liberthia.init.SpiritualTradeData;
import br.com.murilo.liberthia.init.SpiritualTradeOffer;
import br.com.murilo.liberthia.registry.ModMenuTypes;
import net.minecraft.network.chat.Component;
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

public class SpiritualTradeMenu extends AbstractContainerMenu {

    private static final int DISPLAY_CONTAINER_SIZE = 27;

    private static final int COST_SLOT_1 = 9;
    private static final int RESULT_SLOT_1 = 11;

    private static final int COST_SLOT_2 = 12;
    private static final int RESULT_SLOT_2 = 14;

    private static final int COST_SLOT_3 = 15;
    private static final int RESULT_SLOT_3 = 17;

    private final SimpleContainer displayContainer = new SimpleContainer(DISPLAY_CONTAINER_SIZE);
    private final List<SpiritualTradeOffer> offers = new ArrayList<>();

    public SpiritualTradeMenu(int containerId, Inventory playerInventory, ItemStack ownerConnectionStack) {
        super(ModMenuTypes.SPIRITUAL_TRADE.get(), containerId);

        this.offers.addAll(SpiritualTradeData.loadOffers(ownerConnectionStack));

        setupDisplay();
        addDisplaySlots();
        addPlayerInventorySlots(playerInventory);
    }

    private void setupDisplay() {
        if (offers.size() > 0) {
            displayContainer.setItem(COST_SLOT_1, offers.get(0).cost().copy());
            displayContainer.setItem(RESULT_SLOT_1, offers.get(0).result().copy());
        }

        if (offers.size() > 1) {
            displayContainer.setItem(COST_SLOT_2, offers.get(1).cost().copy());
            displayContainer.setItem(RESULT_SLOT_2, offers.get(1).result().copy());
        }

        if (offers.size() > 2) {
            displayContainer.setItem(COST_SLOT_3, offers.get(2).cost().copy());
            displayContainer.setItem(RESULT_SLOT_3, offers.get(2).result().copy());
        }
    }

    private void addDisplaySlots() {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                int slotIndex = column + row * 9;
                int x = 8 + column * 18;
                int y = 18 + row * 18;

                this.addSlot(new DisplayOnlySlot(displayContainer, slotIndex, x, y));
            }
        }
    }

    private void addPlayerInventorySlots(Inventory playerInventory) {
        int inventoryStartY = 84;

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

        int hotbarY = 142;

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
        if (slotId == RESULT_SLOT_1) {
            tryTrade(player, 0);
            return;
        }

        if (slotId == RESULT_SLOT_2) {
            tryTrade(player, 1);
            return;
        }

        if (slotId == RESULT_SLOT_3) {
            tryTrade(player, 2);
            return;
        }

        super.clicked(slotId, button, clickType, player);
    }

    private void tryTrade(Player player, int offerIndex) {
        if (player.level().isClientSide) {
            return;
        }

        if (offerIndex < 0 || offerIndex >= offers.size()) {
            return;
        }

        SpiritualTradeOffer offer = offers.get(offerIndex);

        if (!offer.isValid()) {
            return;
        }

        ItemStack cost = offer.cost();
        ItemStack result = offer.result();

        if (!hasCost(player, cost)) {
            player.displayClientMessage(
                    Component.literal("Você não tem os itens necessários para essa troca."),
                    true
            );
            return;
        }

        consumeCost(player, cost);

        ItemStack reward = result.copy();

        if (!player.getInventory().add(reward)) {
            player.drop(reward, false);
        }

        player.displayClientMessage(
                Component.literal("Troca espiritual concluída."),
                true
        );
    }

    private boolean hasCost(Player player, ItemStack cost) {
        int found = 0;

        for (ItemStack stack : player.getInventory().items) {
            if (matchesCost(stack, cost)) {
                found += stack.getCount();

                if (found >= cost.getCount()) {
                    return true;
                }
            }
        }

        return false;
    }

    private void consumeCost(Player player, ItemStack cost) {
        int remaining = cost.getCount();

        for (ItemStack stack : player.getInventory().items) {
            if (remaining <= 0) {
                break;
            }

            if (!matchesCost(stack, cost)) {
                continue;
            }

            int remove = Math.min(stack.getCount(), remaining);
            stack.shrink(remove);
            remaining -= remove;
        }
    }

    private boolean matchesCost(ItemStack playerStack, ItemStack cost) {
        if (playerStack.isEmpty() || cost.isEmpty()) {
            return false;
        }

        return ItemStack.isSameItemSameTags(playerStack, cost);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    private static class DisplayOnlySlot extends Slot {

        public DisplayOnlySlot(Container container, int slot, int x, int y) {
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