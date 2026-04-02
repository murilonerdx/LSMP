package br.com.murilo.liberthia.item;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.registry.ModBlocks;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.function.Supplier;

public class ClearMatterArmorMaterial implements ArmorMaterial {
    public static final ClearMatterArmorMaterial INSTANCE = new ClearMatterArmorMaterial();

    private static final int[] DURABILITY = {363, 528, 495, 429};
    private static final int[] DEFENSE = {3, 8, 6, 3};

    @Override
    public int getDurabilityForType(ArmorItem.Type type) {
        return DURABILITY[type.ordinal()];
    }

    @Override
    public int getDefenseForType(ArmorItem.Type type) {
        return DEFENSE[type.ordinal()];
    }

    @Override
    public int getEnchantmentValue() {
        return 20;
    }

    @Override
    public SoundEvent getEquipSound() {
        return SoundEvents.ARMOR_EQUIP_DIAMOND;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return Ingredient.of(ModBlocks.CLEAR_MATTER_BLOCK.get().asItem());
    }

    @Override
    public String getName() {
        return LiberthiaMod.MODID + ":clear_matter";
    }

    @Override
    public float getToughness() {
        return 2.5F;
    }

    @Override
    public float getKnockbackResistance() {
        return 0.1F;
    }
}
