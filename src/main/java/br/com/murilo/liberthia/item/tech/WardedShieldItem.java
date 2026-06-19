package br.com.murilo.liberthia.item.tech;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * r182 — <b>Escudo Dissonante</b> (Tecno-Arcano, FE): bloqueando, anula <b>90% de TODO
 * dano mágico</b> (qualquer escola, vanilla ou M&A), consumindo FE. Sem FE, age como
 * escudo comum. Lógica em {@code event/AntiMagicTechHandler}.
 */
public class WardedShieldItem extends ShieldItem {
    public static final int CAP = 300_000, COST_PER_DMG = 300;

    public WardedShieldItem() { super(new Item.Properties().durability(2000).rarity(Rarity.RARE)); }

    @Override public @Nullable ICapabilityProvider initCapabilities(ItemStack s, @Nullable CompoundTag n) {
        return TechEnergy.provider(s, CAP, 6000);
    }

    @Override public void appendHoverText(ItemStack s, @Nullable Level l, List<Component> tip, TooltipFlag f) {
        tip.add(Component.translatable("tooltip.liberthia.warded_shield").withStyle(ChatFormatting.AQUA));
        tip.add(Component.literal(TechEnergy.energy(s) + " / " + CAP + " FE").withStyle(ChatFormatting.GRAY));
    }
    @Override public boolean isBarVisible(ItemStack s) { return true; }
    @Override public int getBarWidth(ItemStack s) { return TechEnergy.barWidth(s, CAP); }
    @Override public int getBarColor(ItemStack s) { return 0x35C0E0; }
    @Override public boolean isFoil(ItemStack s) { return TechEnergy.has(s); }
}
