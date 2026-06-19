package br.com.murilo.liberthia.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * r180: <b>Selo Nulo</b> — ward anti-magia focado no Mana and Artifice. Enquanto o
 * portador o tiver (mão, inventário ou Curios/colar), o dano de feitiços do M&A é quase
 * todo negado e os projéteis de feitiço são REFLETIDOS de volta. Lógica em
 * {@code compat/mna/AntiMagicWardHandler}.
 */
public class NullSealItem extends Item {

    public NullSealItem(Properties props) {
        super(props.stacksTo(1).rarity(Rarity.EPIC));
    }

    @Override public boolean isFoil(ItemStack stack) { return true; }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.literal("§bAnula a magia do §dMana and Artifice").withStyle(ChatFormatting.AQUA));
        tip.add(Component.literal("§7• Nega ~90% do dano de feitiço deles"));
        tip.add(Component.literal("§7• §lReflete§r§7 os projéteis de feitiço de volta"));
        tip.add(Component.literal("§8§oFunciona na mão, inventário ou colar (Curios)."));
    }
}
