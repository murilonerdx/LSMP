package br.com.murilo.liberthia.magic.scribe;

import br.com.murilo.liberthia.registry.ModItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.function.Supplier;

/**
 * r164: Lista estática de recipes da Inscription Table.
 *
 * <p>Cada recipe = (inputPredicate, ink/catalyst Item, result, displayName).
 * Mostradas no panel esquerdo da Screen; clicar uma seleciona ela.
 */
public final class InscriptionRecipes {

    private InscriptionRecipes() {}

    public static class Recipe {
        public final String displayName;
        public final String description;
        public final Supplier<Item> inputItem;
        public final Supplier<Item> inkItem;
        public final Supplier<ItemStack> resultStack;

        public Recipe(String displayName, String description,
                      Supplier<Item> inputItem, Supplier<Item> inkItem,
                      Supplier<ItemStack> resultStack) {
            this.displayName = displayName;
            this.description = description;
            this.inputItem = inputItem;
            this.inkItem = inkItem;
            this.resultStack = resultStack;
        }

        public boolean matches(ItemStack input, ItemStack ink) {
            try {
                return !input.isEmpty() && !ink.isEmpty()
                        && input.is(inputItem.get())
                        && ink.is(inkItem.get());
            } catch (Throwable ignored) {
                return false;
            }
        }

        public ItemStack result() {
            try { return resultStack.get(); } catch (Throwable t) { return ItemStack.EMPTY; }
        }
    }

    /** Wrapper field — recipes acessadas via index. */
    public static final List<Recipe> LIST = List.of(
            new Recipe("Pergaminho Vazio",
                    "Extrai pergaminho de um livro genérico. Tinta = pena.",
                    () -> Items.BOOK,
                    () -> Items.FEATHER,
                    () -> new ItemStack(ModItems.SPELL_PARCHMENT.get())),

            new Recipe("Pergaminho de Glifo",
                    "Extrai pergaminho de um livro de spell. Tinta = lapis.",
                    () -> Items.WRITABLE_BOOK,
                    () -> Items.LAPIS_LAZULI,
                    () -> new ItemStack(ModItems.SPELL_PARCHMENT.get(), 2)),

            new Recipe("Tinta de Sangue",
                    "Converte sangue + ink em magia. Catalyst = blood vial.",
                    () -> Items.RED_DYE,
                    () -> Items.GHAST_TEAR,
                    () -> new ItemStack(ModItems.SPELL_PARCHMENT.get(), 4)),

            new Recipe("Tomo Aprendiz",
                    "Cria Spell Tome básico. Catalyst = ink sac.",
                    () -> Items.ENCHANTED_BOOK,
                    () -> Items.INK_SAC,
                    () -> safeStack(() -> ModItems.SPELL_PARCHMENT.get(), 3)),

            new Recipe("Pergaminho Místico",
                    "Pergaminho com poder extra. Catalyst = nether star.",
                    () -> Items.PAPER,
                    () -> Items.NETHER_STAR,
                    () -> new ItemStack(ModItems.SPELL_PARCHMENT.get(), 8))
    );

    private static ItemStack safeStack(Supplier<Item> sup, int count) {
        try { return new ItemStack(sup.get(), count); }
        catch (Throwable t) { return ItemStack.EMPTY; }
    }
}
