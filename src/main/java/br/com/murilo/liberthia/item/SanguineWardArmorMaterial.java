package br.com.murilo.liberthia.item;

import br.com.murilo.liberthia.LiberthiaMod;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * Sanguine Ward — diamond-tier protective set tuned against Blood Infection.
 * Each piece (via {@link SanguineWardArmorItem}) gives a per-tick chance to
 * block fresh BloodInfection applications, and the full set grants periodic
 * cure pulses.
 */
public final class SanguineWardArmorMaterial implements ArmorMaterial {
    public static final SanguineWardArmorMaterial INSTANCE = new SanguineWardArmorMaterial();
    private SanguineWardArmorMaterial() {}

    @Override public int getDurabilityForType(ArmorItem.Type type) {
        return switch (type) {
            case HELMET -> 500;
            case CHESTPLATE -> 720;
            case LEGGINGS -> 680;
            case BOOTS -> 580;
        };
    }
    @Override public int getDefenseForType(ArmorItem.Type type) {
        return switch (type) {
            case HELMET -> 3;
            case CHESTPLATE -> 8;
            case LEGGINGS -> 6;
            case BOOTS -> 3;
        };
    }
    @Override public int getEnchantmentValue() { return 18; }
    @Override public SoundEvent getEquipSound() { return SoundEvents.ARMOR_EQUIP_DIAMOND; }
    @Override public Ingredient getRepairIngredient() { return Ingredient.of(Items.DIAMOND); }
    @Override public String getName() { return LiberthiaMod.MODID + ":sanguine_ward"; }
    @Override public float getToughness() { return 2.5F; }
    @Override public float getKnockbackResistance() { return 0.1F; }
}
