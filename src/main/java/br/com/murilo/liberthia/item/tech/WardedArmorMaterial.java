package br.com.murilo.liberthia.item.tech;

import br.com.murilo.liberthia.LiberthiaMod;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * r182 — material da <b>Armadura Bastião</b> (Tecno-Arcano anti-magia, FE). Defesa
 * nível netherite; o diferencial é a resistência a dano mágico (consumindo FE) e o nerf
 * a magos atacantes — em {@code event/AntiMagicTechHandler}. Repara com warded_core.
 */
public class WardedArmorMaterial implements ArmorMaterial {
    public static final WardedArmorMaterial INSTANCE = new WardedArmorMaterial();

    private static final int[] DURABILITY = {550, 800, 750, 650};
    private static final int[] DEFENSE = {3, 8, 6, 3};

    @Override public int getDurabilityForType(ArmorItem.Type type) { return DURABILITY[type.ordinal()]; }
    @Override public int getDefenseForType(ArmorItem.Type type) { return DEFENSE[type.ordinal()]; }
    @Override public int getEnchantmentValue() { return 20; }
    @Override public SoundEvent getEquipSound() { return SoundEvents.ARMOR_EQUIP_NETHERITE; }
    @Override public Ingredient getRepairIngredient() {
        return Ingredient.of(br.com.murilo.liberthia.registry.ModTech.WARDED_CORE.get());
    }
    @Override public String getName() { return LiberthiaMod.MODID + ":warded"; }
    @Override public float getToughness() { return 3.0F; }
    @Override public float getKnockbackResistance() { return 0.2F; }
}
