package br.com.murilo.liberthia.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Desecrated Holy Relic — obtido ao matar A Mãe usando 2+ peças de armadura Order.
 * Reagente para transformar armas holy em versões corrompidas (receitas futuras).
 */
public class DesecratedHolyRelicItem extends Item {
    public DesecratedHolyRelicItem(Properties p) {
        super(p.stacksTo(4).rarity(net.minecraft.world.item.Rarity.EPIC).fireResistant());
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Manchado de sangue da Mãe.").withStyle(ChatFormatting.DARK_RED));
        tooltip.add(Component.literal("A luz não volta ao que foi dessacralizado.").withStyle(ChatFormatting.ITALIC, ChatFormatting.GRAY));
    }
}
