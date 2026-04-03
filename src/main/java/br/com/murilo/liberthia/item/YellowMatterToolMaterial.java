package br.com.murilo.liberthia.item;

import br.com.murilo.liberthia.registry.ModItems;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;

public class YellowMatterToolMaterial implements Tier {
    public static final YellowMatterToolMaterial INSTANCE = new YellowMatterToolMaterial();

    private YellowMatterToolMaterial() {
    }

    @Override
    public int getUses() {
        return 1200;
    }

    @Override
    public float getSpeed() {
        return 8.0F;
    }

    @Override
    public float getAttackDamageBonus() {
        return 3.0F;
    }

    @Override
    public int getLevel() {
        return 3;
    }

    @Override
    public int getEnchantmentValue() {
        return 15;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return Ingredient.of(ModItems.YELLOW_MATTER_INGOT.get());
    }
}
