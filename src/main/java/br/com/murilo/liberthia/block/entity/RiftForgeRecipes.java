package br.com.murilo.liberthia.block.entity;

import br.com.murilo.liberthia.registry.ModBlocks;
import br.com.murilo.liberthia.registry.ModItems;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.items.IItemHandler;

/**
 * r185 — receita da Adaga Corta-Fendas III (nível ALL_DIMENSIONS), o craft mais difícil do jogo:
 * 12 ingredientes posicionais na Forja de Fendas (slots 0–11) → adaga no slot 12.
 */
public final class RiftForgeRecipes {
    private RiftForgeRecipes() {}

    /** Ingredientes posicionais por slot (0–11). */
    public static final Ingredient[] T3 = new Ingredient[]{
            Ingredient.of(ModItems.RIFT_CUTTER_T2.get()),                 // 0
            Ingredient.of(ModBlocks.DARK_MATTER_BLOCK.get()),            // 1
            Ingredient.of(ModBlocks.CLEAR_MATTER_BLOCK.get()),          // 2
            Ingredient.of(ModBlocks.YELLOW_MATTER_BLOCK.get()),         // 3
            Ingredient.of(ModBlocks.VOID_SCAR.get()),                   // 4
            Ingredient.of(ModBlocks.DIMENSIONAL_FLUX.get()),            // 5
            Ingredient.of(ModBlocks.RIFT_RESIDUE.get()),                // 6
            Ingredient.of(ModBlocks.WARPED_SPACE.get()),                // 7
            Ingredient.of(Items.NETHER_STAR),                            // 8
            Ingredient.of(Items.END_CRYSTAL),                            // 9
            Ingredient.of(ModItems.DARK_MATTER_SHARD.get()),            // 10
            Ingredient.of(ModItems.ASCENSION_SEAL.get()),               // 11
    };

    /** true se os 12 slots de entrada batem com a receita. */
    public static boolean matches(IItemHandler inv) {
        for (int i = 0; i < 12; i++) {
            if (!T3[i].test(inv.getStackInSlot(i))) return false;
        }
        return true;
    }

    public static ItemStack result() {
        return new ItemStack(ModItems.RIFT_CUTTER_T3.get());
    }
}
