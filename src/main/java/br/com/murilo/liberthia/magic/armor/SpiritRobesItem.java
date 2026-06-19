package br.com.murilo.liberthia.magic.armor;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * v0.1.149 r117: <b>SpiritRobesItem</b> — peça de armadura mágica.
 *
 * <p>Por peça:
 * <ul>
 *   <li>+20 Source max (cap permanente enquanto equipado)</li>
 *   <li>+50% Source regen rate (cumulativo entre peças)</li>
 * </ul>
 *
 * <p>Set bonus (4 peças):
 * <ul>
 *   <li>+80 Source max total</li>
 *   <li>+200% regen rate (3× mais rápido base)</li>
 *   <li>-25% Source cost em todos os feitiços</li>
 *   <li>Imune a damage de spell da própria escola (TODO futuro)</li>
 * </ul>
 *
 * <p>Lógica de bonus aplicada via {@link ManaArmorEffects} (PlayerTickEvent).
 */
public class SpiritRobesItem extends ArmorItem {

    public SpiritRobesItem(ArmorItem.Type type, Properties props) {
        super(SpiritRobesMaterial.INSTANCE, type, props);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.literal("§d§o✦ Spirit Robe").withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.literal("§7+20 §bSource max"));
        tooltip.add(Component.literal("§7+50% §bRegen rate"));
        tooltip.add(Component.literal("§7§o(set 4 peças: §7§o-25% cost§r§7§o)"));
    }
}
