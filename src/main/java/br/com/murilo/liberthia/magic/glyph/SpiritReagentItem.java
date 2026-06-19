package br.com.murilo.liberthia.magic.glyph;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * v0.1.148 r116: Spirit Reagent — item base pros drops do Spirit World que
 * o player usa pra forjar glyphs/spells na Scribes Table.
 *
 * <p>Original code. Tooltip indica o uso geral; pra ver receitas específicas
 * usar livro de receitas (futuro) ou comando admin.
 */
public class SpiritReagentItem extends Item {

    private final String dropSource;

    public SpiritReagentItem(Properties props, String dropSource) {
        super(props);
        this.dropSource = dropSource;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§5§o✦ Spirit Reagent").withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.literal("§7Drop: §8" + dropSource));
        tooltip.add(Component.literal("§7Use em uma §dScribes Table §7com outros reagentes"));
        tooltip.add(Component.literal("§7pra forjar um §6Pergaminho de Feitiço§7."));
    }
}
