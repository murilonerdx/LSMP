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
 * r180: <b>Âncora Mental</b> — supressor dos impulsos da Matéria Escura. Enquanto o
 * portador o tiver (mão, inventário ou Curios), os sintomas de compulsão homicida
 * (tela vermelha + vozes mandando matar) são <b>silenciados</b>. Não cura a matéria —
 * só ancora a mente. Ver {@code DarkMatterCompulsionHandler}.
 */
public class MindAnchorItem extends Item {

    public MindAnchorItem(Properties props) {
        super(props.stacksTo(1).rarity(Rarity.RARE));
    }

    @Override public boolean isFoil(ItemStack stack) { return true; }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.literal("§bSilencia os impulsos da §5Matéria Escura").withStyle(ChatFormatting.AQUA));
        tip.add(Component.literal("§7Sem tela vermelha, sem vozes mandando matar."));
        tip.add(Component.literal("§8§oNão cura a matéria — só ancora a mente."));
    }
}
