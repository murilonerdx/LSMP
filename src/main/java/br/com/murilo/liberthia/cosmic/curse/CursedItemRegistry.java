package br.com.murilo.liberthia.cosmic.curse;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.HashSet;
import java.util.Set;

/**
 * v0.1.22 r45: Registry de items "amaldiçoados" — não podem ser dropados
 * por hand, e quando forçadamente removidos (death drops, despawn) causam
 * consequências de horror.
 *
 * <h2>Mecânica</h2>
 * <ul>
 *   <li>Player segura item amaldiçoado → não consegue dropar (Q ignorado)</li>
 *   <li>Cada item amaldiçoado equipado escala {@code corruption} ao longo do tempo</li>
 *   <li>Se item é removido (creative, comando, despawn), trigger {@code onForcedRemoval}</li>
 * </ul>
 */
public final class CursedItemRegistry {

    private static final Set<Item> CURSED = new HashSet<>();

    private CursedItemRegistry() {}

    public static void register(Item item) {
        CURSED.add(item);
    }

    public static boolean isCursed(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        return CURSED.contains(stack.getItem());
    }

    public static boolean isCursed(Item item) {
        return CURSED.contains(item);
    }
}
