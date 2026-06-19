package br.com.murilo.liberthia.item.tech;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.TierSortingRegistry;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import org.jetbrains.annotations.Nullable;

/**
 * r180c — <b>Paxel</b>: ferramenta 3-em-1 (picareta + machado + pá) movida a FE.
 * Carregada minera qualquer um dos três tipos rápido e sem gastar durabilidade.
 */
public class PaxelItem extends PickaxeItem {
    public static final int CAP = 300_000, COST = 130;

    public PaxelItem() {
        super(Tiers.NETHERITE, 2, -2.6F, new Item.Properties().durability(2200).rarity(Rarity.RARE).fireResistant());
    }

    private static boolean diggable(BlockState st) {
        return st.is(BlockTags.MINEABLE_WITH_PICKAXE)
                || st.is(BlockTags.MINEABLE_WITH_AXE)
                || st.is(BlockTags.MINEABLE_WITH_SHOVEL);
    }

    @Override public @Nullable ICapabilityProvider initCapabilities(ItemStack s, @Nullable CompoundTag n) {
        return TechEnergy.provider(s, CAP, 3000);
    }

    @Override public float getDestroySpeed(ItemStack s, BlockState st) {
        if (diggable(st)) {
            float spd = Tiers.NETHERITE.getSpeed();
            return TechEnergy.has(s) ? spd * 1.8F : spd;
        }
        return super.getDestroySpeed(s, st);
    }

    @Override public boolean isCorrectToolForDrops(ItemStack s, BlockState st) {
        return (diggable(st) && TierSortingRegistry.isCorrectTierForDrops(Tiers.NETHERITE, st))
                || super.isCorrectToolForDrops(s, st);
    }

    @Override public boolean mineBlock(ItemStack s, Level l, BlockState st, BlockPos p, LivingEntity e) {
        if (!l.isClientSide && st.getDestroySpeed(l, p) != 0f && !TechEnergy.drain(s, COST, CAP))
            s.hurtAndBreak(1, e, x -> x.broadcastBreakEvent(EquipmentSlot.MAINHAND));
        return true;
    }

    @Override public boolean hurtEnemy(ItemStack s, LivingEntity t, LivingEntity a) {
        if (!TechEnergy.drain(s, COST * 2, CAP)) s.hurtAndBreak(2, a, x -> x.broadcastBreakEvent(EquipmentSlot.MAINHAND));
        return true;
    }

    @Override public boolean isBarVisible(ItemStack s) { return true; }
    @Override public int getBarWidth(ItemStack s) { return TechEnergy.barWidth(s, CAP); }
    @Override public int getBarColor(ItemStack s) { return TechEnergy.barColor(); }
    @Override public boolean isFoil(ItemStack s) { return TechEnergy.has(s); }
}
