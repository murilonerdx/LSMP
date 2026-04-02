package br.com.murilo.liberthia.item;

import br.com.murilo.liberthia.registry.ModItems;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;

public class DarkMatterToolMaterial implements Tier {
    public static final DarkMatterToolMaterial INSTANCE = new DarkMatterToolMaterial();

    @Override
    public int getUses() {
        return 1800;
    }

    @Override
    public float getSpeed() {
        return 9.0F;
    }

    @Override
    public float getAttackDamageBonus() {
        return 4.0F;
    }

    @Override
    public int getLevel() {
        return 4;
    }

    @Override
    public int getEnchantmentValue() {
        return 15;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return Ingredient.of(ModItems.DARK_MATTER_SHARD.get());
    }
}
