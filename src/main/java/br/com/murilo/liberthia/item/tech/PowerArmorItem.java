package br.com.murilo.liberthia.item.tech;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * r180c — <b>Power Armor</b> (FE): armadura tech que ABSORVE dano extra consumindo
 * energia (ver {@code event/PowerArmorHandler}). Carregável por carregador/bateria
 * (capability ENERGY). Barra de energia verde no inventário.
 */
public class PowerArmorItem extends ArmorItem {
    public final int cap;

    public PowerArmorItem(ArmorItem.Type type, int cap) {
        super(PowerArmorMaterial.INSTANCE, type, new Item.Properties().rarity(Rarity.RARE).fireResistant());
        this.cap = cap;
    }

    @Override public @Nullable ICapabilityProvider initCapabilities(ItemStack s, @Nullable CompoundTag n) {
        return TechEnergy.provider(s, cap, 8000);
    }

    @Override public void appendHoverText(ItemStack s, @Nullable Level l, List<Component> tip, TooltipFlag f) {
        tip.add(Component.translatable("tooltip.liberthia.power_armor").withStyle(net.minecraft.ChatFormatting.AQUA));
        tip.add(Component.literal(TechEnergy.energy(s) + " / " + cap + " FE").withStyle(net.minecraft.ChatFormatting.GRAY));
    }

    @Override public boolean isBarVisible(ItemStack s) { return true; }
    @Override public int getBarWidth(ItemStack s) { return TechEnergy.barWidth(s, cap); }
    @Override public int getBarColor(ItemStack s) { return TechEnergy.barColor(); }
    @Override public boolean isFoil(ItemStack s) { return TechEnergy.has(s); }
}
