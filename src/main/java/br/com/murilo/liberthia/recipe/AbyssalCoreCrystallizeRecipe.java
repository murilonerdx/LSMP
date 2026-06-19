package br.com.murilo.liberthia.recipe;

import br.com.murilo.liberthia.item.AbyssalCoreItem;
import br.com.murilo.liberthia.registry.ModItems;
import br.com.murilo.liberthia.registry.ModRecipes;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/**
 * Receita ESPECIAL (shapeless, sem ordem) que transforma um <b>Núcleo do Abismo</b>
 * + um <b>Caco de Ametista</b> no <b>Núcleo do Vazio Cristalizado</b> — preservando
 * o item (mesmo ID) mas setando a tag NBT {@code crystallized=true}.
 *
 * <p>Precisa ser uma {@link CustomRecipe} porque o {@code crafting_shapeless} vanilla
 * não copia NBT pro resultado. Aqui montamos o ItemStack de saída com a tag na mão.
 * Só aceita o núcleo na forma NÃO cristalizada (não dá pra cristalizar duas vezes).
 */
public class AbyssalCoreCrystallizeRecipe extends CustomRecipe {

    public AbyssalCoreCrystallizeRecipe(ResourceLocation id, CraftingBookCategory category) {
        super(id, category);
    }

    @Override
    public boolean matches(CraftingContainer inv, Level level) {
        boolean hasCore = false, hasAmethyst = false;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.isEmpty()) continue;
            if (s.is(ModItems.ABYSSAL_CORE.get())) {
                if (hasCore || AbyssalCoreItem.isCrystallized(s)) return false; // só 1 e só o não-cristalizado
                hasCore = true;
            } else if (s.is(Items.AMETHYST_SHARD)) {
                if (hasAmethyst) return false; // só 1 ametista
                hasAmethyst = true;
            } else {
                return false; // qualquer outro item invalida
            }
        }
        return hasCore && hasAmethyst;
    }

    @Override
    public ItemStack assemble(CraftingContainer inv, RegistryAccess access) {
        return AbyssalCoreItem.crystallized(ModItems.ABYSSAL_CORE.get());
    }

    @Override
    public boolean canCraftInDimensions(int w, int h) {
        return w * h >= 2; // precisa de 2 slots (núcleo + ametista)
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingContainer inv) {
        return NonNullList.withSize(inv.getContainerSize(), ItemStack.EMPTY);
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.ABYSSAL_CORE_CRYSTALLIZE.get();
    }
}
