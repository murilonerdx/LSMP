package br.com.murilo.liberthia.item.tech;

import br.com.murilo.liberthia.LiberthiaMod;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * r180c — material da <b>Power Armor</b> (linha tech FE, estilo Mekanism MekaSuit).
 * Defesa nível netherite; o diferencial é a redução EXTRA de dano consumindo FE,
 * aplicada por {@code event/PowerArmorHandler}. Repara com {@code energized_steel}.
 * Layers: {@code textures/models/armor/power_layer_*}.
 */
public class PowerArmorMaterial implements ArmorMaterial {
    public static final PowerArmorMaterial INSTANCE = new PowerArmorMaterial();

    private static final int[] DURABILITY = {550, 800, 750, 650};
    private static final int[] DEFENSE = {3, 8, 6, 3};

    @Override public int getDurabilityForType(ArmorItem.Type type) { return DURABILITY[type.ordinal()]; }
    @Override public int getDefenseForType(ArmorItem.Type type) { return DEFENSE[type.ordinal()]; }
    @Override public int getEnchantmentValue() { return 18; }
    @Override public SoundEvent getEquipSound() { return SoundEvents.ARMOR_EQUIP_NETHERITE; }
    @Override public Ingredient getRepairIngredient() {
        return Ingredient.of(br.com.murilo.liberthia.registry.ModTech.ENERGIZED_STEEL.get());
    }
    @Override public String getName() { return LiberthiaMod.MODID + ":power"; }
    @Override public float getToughness() { return 3.0F; }
    @Override public float getKnockbackResistance() { return 0.15F; }
}
