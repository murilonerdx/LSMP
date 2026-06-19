package br.com.murilo.liberthia.item;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.registry.ModItems;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * r180b — material da armadura ANTI-MAGIA (linha ciano "Dissonância"). Defesa
 * sólida (entre ferro e diamante); o diferencial é a resistência mágica aplicada
 * por {@code event/WardFieldHandler} (-10% dano mágico por peça). Repara com
 * {@code dark_matter_catalyst}. Layer = {@code textures/models/armor/antimagic_layer_*}.
 */
public class AntiMagicArmorMaterial implements ArmorMaterial {
    public static final AntiMagicArmorMaterial INSTANCE = new AntiMagicArmorMaterial();

    private static final int[] DURABILITY = {330, 480, 450, 390};
    private static final int[] DEFENSE = {3, 7, 6, 3};

    @Override public int getDurabilityForType(ArmorItem.Type type) { return DURABILITY[type.ordinal()]; }
    @Override public int getDefenseForType(ArmorItem.Type type) { return DEFENSE[type.ordinal()]; }
    @Override public int getEnchantmentValue() { return 16; }
    @Override public SoundEvent getEquipSound() { return SoundEvents.ARMOR_EQUIP_NETHERITE; }
    @Override public Ingredient getRepairIngredient() {
        return Ingredient.of(ModItems.DARK_MATTER_CATALYST.get());
    }
    @Override public String getName() { return LiberthiaMod.MODID + ":antimagic"; }
    @Override public float getToughness() { return 1.5F; }
    @Override public float getKnockbackResistance() { return 0.1F; }
}
