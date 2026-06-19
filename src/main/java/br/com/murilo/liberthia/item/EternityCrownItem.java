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
 * r180: <b>Coroa da Eternidade</b> — concede <b>regeneração divina</b>, mas EXIGE PUREZA:
 * só funciona com sanidade alta (≥ 60). Os impuros são rejeitados pela coroa.
 * Efeito aplicado por {@code DivineArtifactHandler} (vale segurando ou no slot Curios de cabeça).
 */
public class EternityCrownItem extends Item {

    public EternityCrownItem(Properties props) {
        super(props.stacksTo(1).rarity(Rarity.EPIC).fireResistant());
    }

    @Override
    public boolean isFoil(ItemStack stack) { return true; }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.literal("§eRegeneração divina").withStyle(ChatFormatting.YELLOW));
        tip.add(Component.literal("§7Exige §bpureza§7 (sanidade ≥ 60) — os impuros são rejeitados."));
        tip.add(Component.literal("§8§o\"A eternidade só acolhe os puros.\""));
    }
}
