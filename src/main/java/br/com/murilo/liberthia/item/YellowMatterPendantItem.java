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
 * Yellow Matter Pendant — bloqueia o ganho de YELLOW matter ambient.
 */
public class YellowMatterPendantItem extends Item {

    public static final int MAX_DURABILITY = 7200;

    public YellowMatterPendantItem(Properties props) {
        super(props);
    }

    @Override public boolean isFoil(ItemStack stack) { return true; }
    @Override public boolean isDamageable(ItemStack stack) { return true; }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                 List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("✦ Pendant de Matéria Amarela")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC));
        tooltip.add(Component.empty());
        tooltip.add(Component.literal("Equipável em Curios (necklace)")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("§e• Bloqueia ganho de matter YELLOW ambient"));
        tooltip.add(Component.literal("§e• Quebra mais rápido quanto mais YM você tem"));
        tooltip.add(Component.empty());
        int remaining = stack.getMaxDamage() - stack.getDamageValue();
        int minutes = remaining / 1200;
        int seconds = (remaining / 20) % 60;
        ChatFormatting color = remaining > MAX_DURABILITY / 2 ? ChatFormatting.GREEN
                : remaining > MAX_DURABILITY / 4 ? ChatFormatting.YELLOW
                : ChatFormatting.RED;
        tooltip.add(Component.literal(String.format("Carga restante: %dm %ds", minutes, seconds))
                .withStyle(color));
    }
}
