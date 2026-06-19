package br.com.murilo.liberthia.item.tech;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import org.jetbrains.annotations.Nullable;

/** r180c — Espada Energizada (FE: golpe sem gastar durabilidade enquanto carregada). */
public class EnergizedSwordItem extends SwordItem {
    public static final int CAP = 200_000, COST = 200;
    public EnergizedSwordItem() { super(Tiers.NETHERITE, 4, -2.4F, new Item.Properties().durability(2031).rarity(Rarity.RARE).fireResistant()); }
    @Override public @Nullable ICapabilityProvider initCapabilities(ItemStack s, @Nullable CompoundTag n) { return TechEnergy.provider(s, CAP, 2000); }
    @Override public boolean hurtEnemy(ItemStack s, LivingEntity t, LivingEntity a) {
        if (!TechEnergy.drain(s, COST, CAP)) s.hurtAndBreak(1, a, x -> x.broadcastBreakEvent(EquipmentSlot.MAINHAND));
        return true;
    }
    @Override public boolean isBarVisible(ItemStack s) { return true; }
    @Override public int getBarWidth(ItemStack s) { return TechEnergy.barWidth(s, CAP); }
    @Override public int getBarColor(ItemStack s) { return TechEnergy.barColor(); }
    @Override public boolean isFoil(ItemStack s) { return TechEnergy.has(s); }
}
