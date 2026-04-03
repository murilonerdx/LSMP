package br.com.murilo.liberthia.item;

import br.com.murilo.liberthia.registry.ModBlocks;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;

public class ClearMatterToolMaterial implements Tier {
    public static final ClearMatterToolMaterial INSTANCE = new ClearMatterToolMaterial();

    private ClearMatterToolMaterial() {
    }

    @Override
    public int getUses() {
        return 800;
    }

    @Override
    public float getSpeed() {
        return 7.0F;
    }

    @Override
    public float getAttackDamageBonus() {
        return 2.5F;
    }

    @Override
    public int getLevel() {
        return 3;
    }

    @Override
    public int getEnchantmentValue() {
        return 18;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return Ingredient.of(ModBlocks.CLEAR_MATTER_BLOCK.get().asItem());
    }
}
