package br.com.murilo.liberthia.item.tech;

import net.minecraft.ChatFormatting;
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
 * r182 — <b>Armadura Bastião</b> (Tecno-Arcano, FE): resiste a dano mágico consumindo
 * energia (−15% por peça, até −60% no set) e, no set completo, aplica Antimagia em magos
 * que te atacam (ver {@code event/AntiMagicTechHandler}). Descarregada vira netherite normal.
 */
public class WardedArmorItem extends ArmorItem {
    public final int cap;

    public WardedArmorItem(ArmorItem.Type type, int cap) {
        super(WardedArmorMaterial.INSTANCE, type, new Item.Properties().rarity(Rarity.RARE).fireResistant());
        this.cap = cap;
    }

    @Override public @Nullable ICapabilityProvider initCapabilities(ItemStack s, @Nullable CompoundTag n) {
        return TechEnergy.provider(s, cap, 8000);
    }

    @Override public void appendHoverText(ItemStack s, @Nullable Level l, List<Component> tip, TooltipFlag f) {
        tip.add(Component.translatable("tooltip.liberthia.warded_armor").withStyle(ChatFormatting.AQUA));
        tip.add(Component.translatable("tooltip.liberthia.warded_armor2").withStyle(ChatFormatting.DARK_AQUA));
        tip.add(Component.literal(TechEnergy.energy(s) + " / " + cap + " FE").withStyle(ChatFormatting.GRAY));
    }
    @Override public boolean isBarVisible(ItemStack s) { return true; }
    @Override public int getBarWidth(ItemStack s) { return TechEnergy.barWidth(s, cap); }
    @Override public int getBarColor(ItemStack s) { return 0x35C0E0; }
    @Override public boolean isFoil(ItemStack s) { return TechEnergy.has(s); }
}
