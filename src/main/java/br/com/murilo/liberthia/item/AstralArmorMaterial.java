package br.com.murilo.liberthia.item;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.registry.ModItems;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;

/** r194 — Armadura Astral (drop do Astrônomo Cego): defesa altíssima, set dá visão/queda lenta/resistência. */
public class AstralArmorMaterial implements ArmorMaterial {
    public static final AstralArmorMaterial INSTANCE = new AstralArmorMaterial();
    private static final int[] DUR = {520, 760, 720, 620};
    private static final int[] DEF = {4, 9, 7, 4};
    @Override public int getDurabilityForType(ArmorItem.Type t) { return DUR[t.ordinal()]; }
    @Override public int getDefenseForType(ArmorItem.Type t) { return DEF[t.ordinal()]; }
    @Override public int getEnchantmentValue() { return 22; }
    @Override public SoundEvent getEquipSound() { return SoundEvents.ARMOR_EQUIP_NETHERITE; }
    @Override public Ingredient getRepairIngredient() { return Ingredient.of(ModItems.CONSTELLATION_CORE.get()); }
    @Override public String getName() { return LiberthiaMod.MODID + ":astral"; }
    @Override public float getToughness() { return 4.0F; }
    @Override public float getKnockbackResistance() { return 0.2F; }
}
