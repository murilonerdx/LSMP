package br.com.murilo.liberthia.item;

import br.com.murilo.liberthia.LiberthiaMod;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

public final class BloodArmorMaterial implements ArmorMaterial {
    public static final BloodArmorMaterial INSTANCE = new BloodArmorMaterial();
    private BloodArmorMaterial() {}

    @Override public int getDurabilityForType(ArmorItem.Type type) {
        return switch (type) {
            case HELMET -> 650;
            case CHESTPLATE -> 900;
            case LEGGINGS -> 850;
            case BOOTS -> 750;
        };
    }
    @Override public int getDefenseForType(ArmorItem.Type type) {
        return switch (type) {
            case HELMET -> 4;
            case CHESTPLATE -> 9;
            case LEGGINGS -> 7;
            case BOOTS -> 4;
        };
    }
    @Override public int getEnchantmentValue() { return 25; }
    @Override public SoundEvent getEquipSound() { return SoundEvents.ARMOR_EQUIP_NETHERITE; }
    @Override public Ingredient getRepairIngredient() { return Ingredient.of(Items.NETHERITE_SCRAP); }
    @Override public String getName() { return LiberthiaMod.MODID + ":blood"; }
    @Override public float getToughness() { return 4.0F; }
    @Override public float getKnockbackResistance() { return 0.3F; }
}
