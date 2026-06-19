package br.com.murilo.liberthia.magic.scribe;

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

/**
 * r164: Inscription Table Menu — 3 slots tabela + inventário do player +
 * recipe selector (não-slot, via packet).
 */
public class InscriptionTableMenu extends AbstractContainerMenu {

    private final InscriptionTableBlockEntity be;
    private final Level level;

    public InscriptionTableMenu(int id, Inventory playerInv, FriendlyByteBuf buf) {
        this(id, playerInv,
            playerInv.player.level().getBlockEntity(buf.readBlockPos()));
    }

    public InscriptionTableMenu(int id, Inventory playerInv, BlockEntity be) {
        super(br.com.murilo.liberthia.registry.ModMenuTypes.INSCRIPTION_TABLE.get(), id);
        this.be = (InscriptionTableBlockEntity) be;
        this.level = playerInv.player.level();

        addPlayerInventory(playerInv);
        addPlayerHotbar(playerInv);

        IItemHandler h = this.be.getItemHandler();
        // r166: slot positions match redesigned InscriptionTableScreen (coluna vertical)
        // Livro (input) — 180,34
        addSlot(new SlotItemHandler(h, InscriptionTableBlockEntity.SLOT_INPUT, 180, 34));
        // Tinta (catalyst) — 180,66
        addSlot(new SlotItemHandler(h, InscriptionTableBlockEntity.SLOT_INK, 180, 66));
        // Saída (output) — 180,98
        addSlot(new SlotItemHandler(h, InscriptionTableBlockEntity.SLOT_OUTPUT, 180, 98) {
            @Override public boolean mayPlace(ItemStack s) { return false; }
            @Override public void onTake(Player p, ItemStack stack) {
                super.onTake(p, stack);
                InscriptionTableMenu.this.be.onOutputTaken();
            }
        });
    }

    public InscriptionTableBlockEntity getBE() { return be; }
    public net.minecraft.core.BlockPos getBlockPos() { return be.getBlockPos(); }
    public int getSelectedRecipe() { return be.selectedRecipe; }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        Slot slot = this.slots.get(slotIndex);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack orig = slot.getItem().copy();
        ItemStack stk = slot.getItem();
        int playerEnd = 36;
        int tableStart = 36;
        int tableEnd = 39;
        if (slotIndex < playerEnd) {
            if (!moveItemStackTo(stk, tableStart, tableEnd, false)) return ItemStack.EMPTY;
        } else {
            if (!moveItemStackTo(stk, 0, playerEnd, false)) return ItemStack.EMPTY;
        }
        if (stk.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return orig;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(level, be.getBlockPos()),
                player, br.com.murilo.liberthia.registry.ModBlocks.INSCRIPTION_TABLE.get());
    }

    private void addPlayerInventory(Inventory inv) {
        // r166: imageHeight=212, inv rows em y=130/148/166, hotbar y=188
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 130 + row * 18));
    }
    private void addPlayerHotbar(Inventory inv) {
        for (int col = 0; col < 9; col++) addSlot(new Slot(inv, col, 8 + col * 18, 188));
    }
}
