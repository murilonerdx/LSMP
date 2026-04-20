package br.com.murilo.liberthia.event;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.registry.ModItems;
import br.com.murilo.liberthia.registry.ModPotions;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.brewing.BrewingRecipeRegistry;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

/**
 * Brewing recipes (registered on FMLCommonSetup).
 *
 *  AWKWARD + Blood Vial Filled → Hemorrhage
 *  AWKWARD + Congealed Blood   → Sanguine Vitality
 *  Sanguine Vitality + Rusted Dagger → Bloodlust
 *  Sanguine Vitality + fermented spider eye → Anemia
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class BrewingRecipeSetup {

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            BrewingRecipeRegistry.addRecipe(
                    Ingredient.of(PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.AWKWARD)),
                    Ingredient.of(ModItems.BLOOD_VIAL_FILLED.get()),
                    PotionUtils.setPotion(new ItemStack(Items.SPLASH_POTION), ModPotions.HEMORRHAGE.get()));

            BrewingRecipeRegistry.addRecipe(
                    Ingredient.of(PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.AWKWARD)),
                    Ingredient.of(ModItems.CONGEALED_BLOOD.get()),
                    PotionUtils.setPotion(new ItemStack(Items.POTION), ModPotions.SANGUINE_VITALITY.get()));

            BrewingRecipeRegistry.addRecipe(
                    Ingredient.of(PotionUtils.setPotion(new ItemStack(Items.POTION), ModPotions.SANGUINE_VITALITY.get())),
                    Ingredient.of(ModItems.RUSTED_DAGGER.get()),
                    PotionUtils.setPotion(new ItemStack(Items.POTION), ModPotions.BLOODLUST.get()));

            BrewingRecipeRegistry.addRecipe(
                    Ingredient.of(PotionUtils.setPotion(new ItemStack(Items.POTION), ModPotions.SANGUINE_VITALITY.get())),
                    Ingredient.of(Items.FERMENTED_SPIDER_EYE),
                    PotionUtils.setPotion(new ItemStack(Items.SPLASH_POTION), ModPotions.ANEMIA.get()));
        });
    }
}
