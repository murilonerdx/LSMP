package br.com.murilo.liberthia.item;

import br.com.murilo.liberthia.LiberthiaMod;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

public final class OrderArmorMaterial implements ArmorMaterial {
    public static final OrderArmorMaterial INSTANCE = new OrderArmorMaterial();
    private OrderArmorMaterial() {}

    @Override public int getDurabilityForType(ArmorItem.Type type) {
        return switch (type) {
            case HELMET -> 700;
            case CHESTPLATE -> 950;
            case LEGGINGS -> 880;
            case BOOTS -> 780;
        };
    }
    @Override public int getDefenseForType(ArmorItem.Type type) {
        return switch (type) {
            case HELMET -> 4;
            case CHESTPLATE -> 8;
            case LEGGINGS -> 7;
            case BOOTS -> 4;
        };
    }
    @Override public int getEnchantmentValue() { return 30; }
    @Override public SoundEvent getEquipSound() { return SoundEvents.ARMOR_EQUIP_DIAMOND; }
    @Override public Ingredient getRepairIngredient() { return Ingredient.of(Items.DIAMOND); }
    @Override public String getName() { return LiberthiaMod.MODID + ":order"; }
    @Override public float getToughness() { return 3.5F; }
    @Override public float getKnockbackResistance() { return 0.2F; }
}
