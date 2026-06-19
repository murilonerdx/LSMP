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
 * r180: <b>Selo da Ascensão</b> — marca o portador como "permitido" nas áreas divinas.
 * Enquanto o tiver (mão, inventário ou Curios), recebe <b>favor divino</b> (queda lenta +
 * regeneração sob o sol) e a flag {@code liberthia.ascended} fica ativa (gating de áreas
 * divinas). Efeito aplicado por {@code DivineArtifactHandler}.
 */
public class AscensionSealItem extends Item {

    public AscensionSealItem(Properties props) {
        super(props.stacksTo(1).rarity(Rarity.EPIC).fireResistant());
    }

    @Override
    public boolean isFoil(ItemStack stack) { return true; }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.literal("§6Permite entrar em §eáreas divinas§6.").withStyle(ChatFormatting.GOLD));
        tip.add(Component.literal("§7Favor divino: §fqueda lenta §7+ §aregeneração ao sol"));
        tip.add(Component.literal("§8§o\"Só os marcados podem subir.\""));
    }
}
