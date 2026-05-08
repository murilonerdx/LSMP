package br.com.murilo.liberthia.menu;

import br.com.murilo.liberthia.block.entity.DimensionalChestBlockEntity;
import br.com.murilo.liberthia.persistence.DimensionalStorage;
import br.com.murilo.liberthia.registry.ModMenuTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;

/**
 * Menu do Baú Dimensional — 54 slots (6×9, double chest layout). Liga
 * direto no handler do canal compartilhado em {@link DimensionalStorage}.
 */
public class DimensionalChestMenu extends AbstractContainerMenu {

    private final DimensionalChestBlockEntity be;
    private final String channel;

    /** Server constructor. */
    public DimensionalChestMenu(int id, Inventory playerInv, DimensionalChestBlockEntity be) {
        super(ModMenuTypes.DIMENSIONAL_CHEST.get(), id);
        this.be = be;
        this.channel = be.getChannel();

        var handler = be.getInventoryHandler();
        // 6 rows × 9 cols, top of GUI
        for (int row = 0; row < 6; row++) {
            for (int col = 0; col < 9; col++) {
                int slot = col + row * 9;
                this.addSlot(new SlotItemHandler(handler, slot, 8 + col * 18, 18 + row * 18));
            }
        }

        // Player inventory below
        int invY = 140;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInv, col + row * 9 + 9,
                        8 + col * 18, invY + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInv, col, 8 + col * 18, invY + 58));
        }
    }

    /** Client constructor. */
    public DimensionalChestMenu(int id, Inventory playerInv, FriendlyByteBuf buf) {
        this(id, playerInv,
                (DimensionalChestBlockEntity) playerInv.player.level().getBlockEntity(buf.readBlockPos()));
    }

    public String getChannel() { return channel; }
    public DimensionalChestBlockEntity getBlockEntity() { return be; }

    @Override
    public ItemStack quickMoveStack(Player player, int idx) {
        Slot src = this.slots.get(idx);
        if (src == null || !src.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = src.getItem();
        ItemStack copy = stack.copy();
        int chestEnd = 54;
        int totalSlots = chestEnd + 36;
        if (idx < chestEnd) {
            // From chest → player inv
            if (!moveItemStackTo(stack, chestEnd, totalSlots, true)) return ItemStack.EMPTY;
        } else {
            // From player inv → chest
            if (!moveItemStackTo(stack, 0, chestEnd, false)) return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) src.set(ItemStack.EMPTY);
        else src.setChanged();
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return be != null && !be.isRemoved();
    }
}
