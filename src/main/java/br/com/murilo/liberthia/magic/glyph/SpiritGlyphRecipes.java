package br.com.murilo.liberthia.magic.glyph;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * v0.1.148 r116: <b>SpiritGlyphRecipes</b> — registro de receitas reagent→spell.
 *
 * <p>Cada receita = um {@link Recipe} contendo:
 * <ul>
 *   <li>List de ingredients (Items + qty) que o player precisa ter no inventário</li>
 *   <li>Output: ItemStack (geralmente um spell scroll) + count</li>
 * </ul>
 *
 * <p>API:
 * <ul>
 *   <li>{@link #register} — registra nova receita (chamado static init)</li>
 *   <li>{@link #tryConsume} — verifica inv do player; se match, consome ingredients e retorna output</li>
 * </ul>
 *
 * <p>Original code. Estrutura simples Map → List intencionalmente (sem JSON
 * data-driven pra evitar complexity inicial).
 */
public final class SpiritGlyphRecipes {

    public static final class Recipe {
        public final List<Ingredient> ingredients;
        public final Supplier<ItemStack> output;
        public final String displayName;

        public Recipe(String displayName, Supplier<ItemStack> output, Ingredient... ings) {
            this.displayName = displayName;
            this.ingredients = Arrays.asList(ings);
            this.output = output;
        }
    }

    public static final class Ingredient {
        public final Supplier<Item> item;
        public final int count;

        public Ingredient(Supplier<Item> item, int count) {
            this.item = item;
            this.count = count;
        }
    }

    private static final List<Recipe> RECIPES = new ArrayList<>();

    private SpiritGlyphRecipes() {}

    public static void register(Recipe r) {
        RECIPES.add(r);
    }

    public static List<Recipe> all() {
        return RECIPES;
    }

    /** Helper construtor de ingredient. */
    public static Ingredient ing(Supplier<Item> item, int count) {
        return new Ingredient(item, count);
    }

    /**
     * Tenta consumir ingredientes do inventário do player pra essa receita.
     * Retorna a primeira receita que casa, ou null.
     */
    public static Recipe findMatching(Player player) {
        Inventory inv = player.getInventory();
        outer:
        for (Recipe r : RECIPES) {
            for (Ingredient ing : r.ingredients) {
                int have = 0;
                Item want = ing.item.get();
                for (int i = 0; i < inv.getContainerSize(); i++) {
                    ItemStack s = inv.getItem(i);
                    if (s.is(want)) have += s.getCount();
                }
                if (have < ing.count) continue outer;
            }
            return r;
        }
        return null;
    }

    /**
     * Executa a receita: consome ingredients do inv + retorna o output.
     * Retorna null se não tem ingredientes (não deveria ser chamado sem chamar
     * findMatching antes).
     */
    public static ItemStack tryConsume(Player player, Recipe r) {
        // Validate first (defensive)
        Inventory inv = player.getInventory();
        Map<Item, Integer> needed = new HashMap<>();
        for (Ingredient ing : r.ingredients) {
            needed.merge(ing.item.get(), ing.count, Integer::sum);
        }
        Map<Item, Integer> have = new HashMap<>();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.isEmpty()) continue;
            have.merge(s.getItem(), s.getCount(), Integer::sum);
        }
        for (Map.Entry<Item, Integer> e : needed.entrySet()) {
            if (have.getOrDefault(e.getKey(), 0) < e.getValue()) {
                return null;
            }
        }

        // Consume — iterate slots, remove count needed
        Map<Item, Integer> remaining = new HashMap<>(needed);
        for (int i = 0; i < inv.getContainerSize() && !remaining.isEmpty(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.isEmpty()) continue;
            Integer want = remaining.get(s.getItem());
            if (want == null || want <= 0) continue;
            int take = Math.min(want, s.getCount());
            s.shrink(take);
            int newRemaining = want - take;
            if (newRemaining <= 0) remaining.remove(s.getItem());
            else remaining.put(s.getItem(), newRemaining);
        }
        return r.output.get();
    }
}
