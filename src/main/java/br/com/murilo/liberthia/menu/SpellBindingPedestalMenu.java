package br.com.murilo.liberthia.menu;

import br.com.murilo.liberthia.observation.block.SpellBindingPedestalBlockEntity;
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

public class SpellBindingPedestalMenu extends AbstractContainerMenu {
    private final SpellBindingPedestalBlockEntity blockEntity;
    private final Level level;

    public SpellBindingPedestalMenu(int containerId, Inventory playerInv, FriendlyByteBuf extraData) {
        this(containerId, playerInv,
            playerInv.player.level().getBlockEntity(extraData.readBlockPos()));
    }

    public SpellBindingPedestalMenu(int containerId, Inventory playerInv, BlockEntity be) {
        super(ModMenuTypes.SPELL_BINDING_PEDESTAL.get(), containerId);
        this.blockEntity = (SpellBindingPedestalBlockEntity) be;
        this.level = playerInv.player.level();
        addPlayerInventory(playerInv);
        addPlayerHotbar(playerInv);
        IItemHandler h = blockEntity.getItemHandler();
        addSlot(new SlotItemHandler(h, SpellBindingPedestalBlockEntity.SLOT_BOOK, 40, 40));
        addSlot(new SlotItemHandler(h, SpellBindingPedestalBlockEntity.SLOT_PARCHMENT, 80, 40));
        addSlot(new SlotItemHandler(h, SpellBindingPedestalBlockEntity.SLOT_OUTPUT, 130, 40) {
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
        int tableEnd = 39;
        if (slotIndex < playerEnd) {
            if (!moveItemStackTo(stk, tableStart, tableEnd, false)) return ItemStack.EMPTY;
        } else {
            if (slotIndex == 38) {
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
            player, ModBlocks.SPELL_BINDING_PEDESTAL.get());
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
