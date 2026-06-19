package br.com.murilo.liberthia.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * r180b — <b>Escudo Anti-magia</b> (linha ciano). Escudo forte (durabilidade alta).
 * Enquanto bloqueia, {@code WardFieldHandler} reduz o dano mágico em 80%; segurá-lo
 * marca o portador como anti-magia (repulsão por proximidade).
 */
public class AntiMagicShieldItem extends ShieldItem {

    public AntiMagicShieldItem(Properties props) {
        super(props.durability(1200));
    }

    @Override public boolean isFoil(ItemStack stack) { return true; }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.literal("✦ Escudo Anti-magia").withStyle(ChatFormatting.AQUA));
        tip.add(Component.literal("§7Bloqueando: §b-80% dano mágico"));
        tip.add(Component.literal("§7Segurá-lo te marca como anti-magia"));
    }
}
