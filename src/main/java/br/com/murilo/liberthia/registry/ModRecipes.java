package br.com.murilo.liberthia.registry;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.recipe.AbyssalCoreCrystallizeRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Registro de RecipeSerializers customizados do Liberthia.
 */
public final class ModRecipes {

    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, LiberthiaMod.MODID);

    /** Núcleo do Abismo + Ametista → Núcleo do Vazio Cristalizado (special, copia NBT). */
    public static final RegistryObject<SimpleCraftingRecipeSerializer<AbyssalCoreCrystallizeRecipe>> ABYSSAL_CORE_CRYSTALLIZE =
            SERIALIZERS.register("abyssal_core_crystallize",
                    () -> new SimpleCraftingRecipeSerializer<>(AbyssalCoreCrystallizeRecipe::new));

    private ModRecipes() {}

    public static void register(IEventBus bus) {
        SERIALIZERS.register(bus);
    }
}
