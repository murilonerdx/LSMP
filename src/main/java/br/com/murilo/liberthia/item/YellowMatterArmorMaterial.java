package br.com.murilo.liberthia.item;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.registry.ModItems;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;

public final class YellowMatterArmorMaterial implements ArmorMaterial {
    public static final YellowMatterArmorMaterial INSTANCE = new YellowMatterArmorMaterial();

    private YellowMatterArmorMaterial() {
    }

    @Override
    public int getDurabilityForType(ArmorItem.Type type) {
        return switch (type) {
            case HELMET -> 242;
            case CHESTPLATE -> 352;
            case LEGGINGS -> 330;
            case BOOTS -> 286;
        };
    }

    @Override
    public int getDefenseForType(ArmorItem.Type type) {
        return switch (type) {
            case HELMET -> 3;
            case CHESTPLATE -> 7;
            case LEGGINGS -> 6;
            case BOOTS -> 3;
        };
    }

    @Override
    public int getEnchantmentValue() {
        return 17;
    }

    @Override
    public SoundEvent getEquipSound() {
        return SoundEvents.ARMOR_EQUIP_IRON;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return Ingredient.of(ModItems.YELLOW_MATTER_INGOT.get());
    }

    @Override
    public String getName() {
        return LiberthiaMod.MODID + ":yellow_matter";
    }

    @Override
    public float getToughness() {
        return 1.5F;
    }

    @Override
    public float getKnockbackResistance() {
        return 0.05F;
    }
}
