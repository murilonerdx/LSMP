package br.com.murilo.liberthia.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * r180b — peça da armadura ANTI-MAGIA. Marcador usado por {@code WardFieldHandler}:
 * cada peça vestida dá -10% dano mágico (conjunto = -40%) e marca o portador como
 * "anti-magia" (repulsão por proximidade de conjuradores).
 */
public class AntiMagicArmorItem extends ArmorItem {

    public AntiMagicArmorItem(ArmorMaterial material, Type type, Properties props) {
        super(material, type, props);
    }

    @Override public boolean isFoil(ItemStack stack) { return true; }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.literal("✦ Anti-magia").withStyle(ChatFormatting.AQUA));
        tip.add(Component.literal("§7-10% dano mágico por peça §8(conjunto: -40%)"));
        tip.add(Component.literal("§7Repele conjuradores que chegam perto"));
    }
}
