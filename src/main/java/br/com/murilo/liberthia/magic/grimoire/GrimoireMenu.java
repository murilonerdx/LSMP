package br.com.murilo.liberthia.magic.grimoire;

import br.com.murilo.liberthia.registry.ModMenuTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;

/**
 * v0.1.151 r119: <b>GrimoireMenu</b> — 9 slots em fileira (estilo hotbar)
 * + player inventory.
 *
 * <p>O inventory live é do ItemStack — quando player fecha menu, salva pro NBT.
 */
public class GrimoireMenu extends AbstractContainerMenu {

    private final ItemStack grimoire;
    private final GrimoireInventory grimoireInv;
    private final Player player;

    public GrimoireMenu(int id, Inventory playerInv, ItemStack grimoire) {
        super(ModMenuTypes.GRIMOIRE.get(), id);
        this.grimoire = grimoire;
        this.player = playerInv.player;
        this.grimoireInv = GrimoireInventory.from(grimoire);

        // 9 scroll slots em linha horizontal no top
        for (int i = 0; i < 9; i++) {
            addSlot(new SlotItemHandler(grimoireInv, i, 8 + i * 18, 30));
        }

        // Player inventory (3 rows × 9 cols)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, 70 + row * 18));
            }
        }
        // Hotbar
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInv, col, 8 + col * 18, 128));
        }
    }

    public GrimoireMenu(int id, Inventory playerInv, FriendlyByteBuf buf) {
        this(id, playerInv, findGrimoire(playerInv.player, buf.readBoolean()));
    }

    private static ItemStack findGrimoire(Player p, boolean mainHand) {
        return mainHand ? p.getMainHandItem() : p.getOffhandItem();
    }

    public ItemStack getGrimoire() { return grimoire; }
    public int activeSlot() { return GrimoireBookItem.getActiveSlot(grimoire); }
    public void setActiveSlot(int s) { GrimoireBookItem.setActiveSlot(grimoire, s); }

    @Override
    public void removed(Player p) {
        super.removed(p);
        grimoireInv.saveTo(grimoire);
    }

    @Override
    public boolean stillValid(Player player) {
        return !grimoire.isEmpty() && grimoire.getItem() instanceof GrimoireBookItem;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;
        ItemStack origStack = slot.getItem();
        ItemStack copy = origStack.copy();

        // 0-8 = grimoire scroll slots; 9+ = player
        if (index < 9) {
            if (!moveItemStackTo(origStack, 9, this.slots.size(), true)) return ItemStack.EMPTY;
        } else {
            // r164: aceita Universal (great_spell, fireball, etc) E DynamicSpellItem (factory_spell_scroll)
            if (!GrimoireInventory.isSpellScroll(origStack)) {
                return ItemStack.EMPTY;
            }
            if (!moveItemStackTo(origStack, 0, 9, false)) return ItemStack.EMPTY;
        }
        if (origStack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }
}
