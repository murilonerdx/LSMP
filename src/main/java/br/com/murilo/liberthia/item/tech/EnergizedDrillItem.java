package br.com.murilo.liberthia.item.tech;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import org.jetbrains.annotations.Nullable;

/** r180c — Furadeira (Drill): picareta turbinada — minera MUITO rápido com FE, custo alto. */
public class EnergizedDrillItem extends PickaxeItem {
    public static final int CAP = 400_000, COST = 220;
    public EnergizedDrillItem() { super(Tiers.NETHERITE, 2, -2.6F, new Item.Properties().durability(2200).rarity(Rarity.EPIC).fireResistant()); }
    @Override public @Nullable ICapabilityProvider initCapabilities(ItemStack s, @Nullable CompoundTag n) { return TechEnergy.provider(s, CAP, 4000); }
    @Override public float getDestroySpeed(ItemStack s, BlockState st) { float b = super.getDestroySpeed(s, st); return TechEnergy.has(s) ? b * 2.6F : b * 0.7F; }
    @Override public boolean mineBlock(ItemStack s, Level l, BlockState st, BlockPos p, LivingEntity e) {
        if (!l.isClientSide && st.getDestroySpeed(l, p) != 0f && !TechEnergy.drain(s, COST, CAP))
            s.hurtAndBreak(1, e, x -> x.broadcastBreakEvent(EquipmentSlot.MAINHAND));
        return true;
    }
    @Override public boolean hurtEnemy(ItemStack s, LivingEntity t, LivingEntity a) {
        if (!TechEnergy.drain(s, COST, CAP)) s.hurtAndBreak(2, a, x -> x.broadcastBreakEvent(EquipmentSlot.MAINHAND));
        return true;
    }
    @Override public boolean isBarVisible(ItemStack s) { return true; }
    @Override public int getBarWidth(ItemStack s) { return TechEnergy.barWidth(s, CAP); }
    @Override public int getBarColor(ItemStack s) { return TechEnergy.barColor(); }
    @Override public boolean isFoil(ItemStack s) { return TechEnergy.has(s); }
}
