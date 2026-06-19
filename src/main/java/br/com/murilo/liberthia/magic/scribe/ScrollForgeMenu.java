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
 * r166: Scroll Forge Container — Focus slot + Output slot + player inventory.
 *
 * <p>Layout (UI coordinates, batem com {@code ScrollForgeScreen}):
 * <pre>
 *  [Focus]  »»  [Output]
 *  (124,36)     (170,36)
 * </pre>
 *
 * <p>Inventário do player é adicionado <b>primeiro</b> (slots 0..35) e os
 * slots da mesa por último (36=Focus, 37=Output), batendo com os índices
 * usados em {@link #quickMoveStack}.
 */
public class ScrollForgeMenu extends AbstractContainerMenu {

    private final ScrollForgeBlockEntity be;
    private final Level level;

    /** Client-side constructor — called from factory via FriendlyByteBuf. */
    public ScrollForgeMenu(int id, Inventory playerInv, FriendlyByteBuf buf) {
        this(id, playerInv, playerInv.player.level().getBlockEntity(buf.readBlockPos()));
    }

    public ScrollForgeMenu(int id, Inventory playerInv, BlockEntity be) {
        super(br.com.murilo.liberthia.registry.ModMenuTypes.SCROLL_FORGE.get(), id);
        this.be    = (ScrollForgeBlockEntity) be;
        this.level = playerInv.player.level();

        addPlayerInventory(playerInv);   // slots 0..26
        addPlayerHotbar(playerInv);      // slots 27..35

        IItemHandler h = this.be.getItemHandler();

        // Focus input slot — only accepts Focus items (slot 36)
        addSlot(new SlotItemHandler(h, ScrollForgeBlockEntity.SLOT_FOCUS, 124, 36) {
            @Override public boolean mayPlace(ItemStack s) {
                return ScrollForgeBlockEntity.identifySchool(s) != null;
            }
        });

        // Output slot — no direct placement; taken via onTake (slot 37)
        addSlot(new SlotItemHandler(h, ScrollForgeBlockEntity.SLOT_OUTPUT, 170, 36) {
            @Override public boolean mayPlace(ItemStack s) { return false; }
            @Override public void onTake(Player p, ItemStack stack) {
                super.onTake(p, stack);
                ScrollForgeMenu.this.be.onOutputTaken();
            }
        });
    }

    public ScrollForgeBlockEntity getBE() { return be; }
    public net.minecraft.core.BlockPos getBlockPos() { return be.getBlockPos(); }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        Slot slot = this.slots.get(slotIndex);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack orig = slot.getItem().copy();
        ItemStack stk  = slot.getItem();
        int playerEnd   = 36;
        int tableStart  = 36;
        int tableEnd    = 38;
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
                player, br.com.murilo.liberthia.registry.ModBlocks.SCROLL_FORGE.get());
    }

    private void addPlayerInventory(Inventory inv) {
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 104 + row * 18));
    }
    private void addPlayerHotbar(Inventory inv) {
        for (int col = 0; col < 9; col++)
            addSlot(new Slot(inv, col, 8 + col * 18, 162));
    }
}
