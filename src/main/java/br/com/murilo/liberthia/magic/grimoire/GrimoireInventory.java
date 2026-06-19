package br.com.murilo.liberthia.magic.grimoire;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;

/**
 * v0.1.151 r119: <b>GrimoireInventory</b> — ItemStackHandler de 9 slots que
 * lê/escreve do ItemStack NBT do Grimoire book.
 *
 * <p>Não é um BlockEntity inventory — é per-stack. Cada Grimoire individual
 * tem seu próprio inventory persistido no NBT.
 */
public class GrimoireInventory extends ItemStackHandler {

    private static final String NBT_ITEMS = "Items";

    public GrimoireInventory() {
        super(GrimoireBookItem.SCROLL_SLOTS);
    }

    public static GrimoireInventory from(ItemStack grimoire) {
        GrimoireInventory inv = new GrimoireInventory();
        if (grimoire.hasTag()) {
            CompoundTag root = grimoire.getTag().getCompound(GrimoireBookItem.NBT_GRIMOIRE);
            if (root.contains(NBT_ITEMS)) {
                inv.deserializeNBT(root.getCompound(NBT_ITEMS));
            }
        }
        return inv;
    }

    public void saveTo(ItemStack grimoire) {
        CompoundTag root = grimoire.getOrCreateTagElement(GrimoireBookItem.NBT_GRIMOIRE);
        root.put(NBT_ITEMS, serializeNBT());
    }

    /**
     * r164 fix: aceita tanto {@link br.com.murilo.liberthia.magic.spell.UniversalSpellScrollItem}
     * (spells prontos como "great_spell", "fireball" etc) quanto
     * {@link br.com.murilo.liberthia.magic.factory.DynamicSpellItem}
     * (os 117 factory_spell_scroll criados via SpellFactory).
     */
    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return stack.isEmpty() || isSpellScroll(stack);
    }

    /** Utility: any spell scroll item (universal or factory). */
    public static boolean isSpellScroll(ItemStack stack) {
        var item = stack.getItem();
        return item instanceof br.com.murilo.liberthia.magic.spell.UniversalSpellScrollItem
                || item instanceof br.com.murilo.liberthia.magic.factory.DynamicSpellItem;
    }
}
