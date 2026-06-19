package br.com.murilo.liberthia.item;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.registry.ModBlocks;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * Material da Dark Matter armor — durabilidade alta, defesa pesada, repair
 * via dark_matter_block. Set completo dá Strength + Resistance + Absorption
 * (ver MatterArmorEffectsHandler).
 */
public class DarkMatterArmorMaterial implements ArmorMaterial {
    public static final DarkMatterArmorMaterial INSTANCE = new DarkMatterArmorMaterial();

    private static final int[] DURABILITY = {407, 592, 555, 481}; // mais durável que clear
    private static final int[] DEFENSE = {3, 8, 6, 3};

    @Override public int getDurabilityForType(ArmorItem.Type type) { return DURABILITY[type.ordinal()]; }
    @Override public int getDefenseForType(ArmorItem.Type type) { return DEFENSE[type.ordinal()]; }
    @Override public int getEnchantmentValue() { return 18; }
    @Override public SoundEvent getEquipSound() { return SoundEvents.ARMOR_EQUIP_NETHERITE; }
    @Override public Ingredient getRepairIngredient() {
        return Ingredient.of(ModBlocks.DARK_MATTER_BLOCK.get().asItem());
    }
    @Override public String getName() { return LiberthiaMod.MODID + ":dark_matter"; }
    @Override public float getToughness() { return 3.0F; }
    @Override public float getKnockbackResistance() { return 0.15F; }
}
