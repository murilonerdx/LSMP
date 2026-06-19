package br.com.murilo.liberthia.item;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.registry.ModItems;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;

/** r194 — Armadura Parasítica (drop da Colmeia Rainha): defesa pesada, set dá regeneração/vida/resistência. */
public class ParasiticArmorMaterial implements ArmorMaterial {
    public static final ParasiticArmorMaterial INSTANCE = new ParasiticArmorMaterial();
    private static final int[] DUR = {480, 700, 660, 560};
    private static final int[] DEF = {3, 8, 6, 3};
    @Override public int getDurabilityForType(ArmorItem.Type t) { return DUR[t.ordinal()]; }
    @Override public int getDefenseForType(ArmorItem.Type t) { return DEF[t.ordinal()]; }
    @Override public int getEnchantmentValue() { return 20; }
    @Override public SoundEvent getEquipSound() { return SoundEvents.ARMOR_EQUIP_NETHERITE; }
    @Override public Ingredient getRepairIngredient() { return Ingredient.of(ModItems.HIVE_HEART.get()); }
    @Override public String getName() { return LiberthiaMod.MODID + ":parasitic"; }
    @Override public float getToughness() { return 3.0F; }
    @Override public float getKnockbackResistance() { return 0.15F; }
}
