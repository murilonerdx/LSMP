package br.com.murilo.liberthia.enchantment;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;

/**
 * r180b — <b>Dissonância</b> (encantamento anti-magia de armadura). Reduz em
 * <b>40%</b> o dano mágico/arcano recebido (vanilla MAGIC/INDIRECT_MAGIC + fontes
 * M&A/Ars/etc). Aplicado por {@code event/WardFieldHandler}. Vai em qualquer peça
 * de armadura; nível único. Faz parte da linha anti-magia (identidade ciano).
 */
public class WardEnchantment extends Enchantment {

    public WardEnchantment() {
        super(Rarity.RARE, EnchantmentCategory.ARMOR, new EquipmentSlot[]{
                EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET});
    }

    @Override public int getMinCost(int level) { return 15; }
    @Override public int getMaxCost(int level) { return 60; }
    @Override public int getMaxLevel() { return 1; }
}
