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
 * r180c — <b>Jetpack</b> (peitoral FE): segure PULAR para subir/voar, consumindo
 * energia. Movimento aplicado no cliente ({@code client/JetpackClientHandler}); o FE
 * é drenado no servidor ({@code network/packet/JetpackActiveC2SPacket}) — seguro p/
 * servidor dedicado. Também protege como peitoral netherite.
 */
public class JetpackItem extends ArmorItem {
    public static final int CAP = 600_000, COST_PER_TICK = 90;

    public JetpackItem() {
        super(PowerArmorMaterial.INSTANCE, ArmorItem.Type.CHESTPLATE,
                new Item.Properties().rarity(Rarity.RARE).fireResistant());
    }

    @Override public @Nullable ICapabilityProvider initCapabilities(ItemStack s, @Nullable CompoundTag n) {
        return TechEnergy.provider(s, CAP, 16_000);
    }

    @Override public void appendHoverText(ItemStack s, @Nullable Level l, List<Component> tip, TooltipFlag f) {
        tip.add(Component.translatable("tooltip.liberthia.jetpack").withStyle(net.minecraft.ChatFormatting.AQUA));
        tip.add(Component.literal(TechEnergy.energy(s) + " / " + CAP + " FE").withStyle(net.minecraft.ChatFormatting.GRAY));
    }
    @Override public boolean isBarVisible(ItemStack s) { return true; }
    @Override public int getBarWidth(ItemStack s) { return TechEnergy.barWidth(s, CAP); }
    @Override public int getBarColor(ItemStack s) { return TechEnergy.barColor(); }
    @Override public boolean isFoil(ItemStack s) { return TechEnergy.has(s); }
}
