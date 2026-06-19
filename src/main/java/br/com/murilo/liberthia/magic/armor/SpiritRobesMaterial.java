package br.com.murilo.liberthia.magic.armor;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * v0.1.149 r117: <b>SpiritRobesMaterial</b> — material custom pra Spirit Robes.
 *
 * <p>Defesa LEVE (mago é frágil) mas duradouro. Foco em utility magic, não tank.
 *
 * <p>Original code.
 */
public class SpiritRobesMaterial implements ArmorMaterial {

    public static final SpiritRobesMaterial INSTANCE = new SpiritRobesMaterial();

    private static final Map<ArmorItem.Type, Integer> DEFENSE = new EnumMap<>(ArmorItem.Type.class);
    static {
        DEFENSE.put(ArmorItem.Type.HELMET, 2);
        DEFENSE.put(ArmorItem.Type.CHESTPLATE, 4);
        DEFENSE.put(ArmorItem.Type.LEGGINGS, 3);
        DEFENSE.put(ArmorItem.Type.BOOTS, 1);
    }

    @Override public int getDurabilityForType(ArmorItem.Type type) {
        return switch (type) {
            case BOOTS -> 195;
            case LEGGINGS -> 225;
            case CHESTPLATE -> 240;
            case HELMET -> 165;
        };
    }
    @Override public int getDefenseForType(ArmorItem.Type type) { return DEFENSE.get(type); }
    @Override public int getEnchantmentValue() { return 25; }   // alto pra encantamentos
    @Override public SoundEvent getEquipSound() { return SoundEvents.ARMOR_EQUIP_LEATHER; }
    @Override public Ingredient getRepairIngredient() { return Ingredient.EMPTY; }  // repair via Imbuement
    @Override public String getName() { return "liberthia:spirit_robes"; }
    @Override public float getToughness() { return 0.5F; }
    @Override public float getKnockbackResistance() { return 0.0F; }
}
