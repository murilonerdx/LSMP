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
 * Botas Mercuriais — equipável no slot Curios {@code feet}.
 *
 * <ul>
 *   <li>Enquanto equipada: permite voo creative-like via {@code mayfly}.</li>
 *   <li>Durabilidade 4800 ticks (4 min). Drena 1/tick voando.</li>
 *   <li>Drena +1 por hit causado em outra entidade.</li>
 *   <li>Ao tirar / esgotar, restaura mayfly false.</li>
 * </ul>
 */
public class BotasMercuriaisItem extends Item {

    public static final int MAX_DURABILITY = 4800;

    public BotasMercuriaisItem(Properties props) {
        super(props);
    }

    @Override public boolean isFoil(ItemStack stack) { return true; }
    @Override public boolean isDamageable(ItemStack stack) { return true; }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                 List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("✦ Botas Mercuriais")
                .withStyle(ChatFormatting.AQUA, ChatFormatting.ITALIC));
        tooltip.add(Component.empty());
        tooltip.add(Component.literal("Asas alquímicas para voo livre.")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.empty());
        tooltip.add(Component.literal("Slot Curios: feet")
                .withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.literal("§b• Permite voo (creative-like)"));
        tooltip.add(Component.literal("§b• Drena enquanto voa"));
        tooltip.add(Component.literal("§b• Drena por hit em inimigo"));
        tooltip.add(Component.empty());
        int remaining = stack.getMaxDamage() - stack.getDamageValue();
        int seconds = remaining / 20;
        int mm = seconds / 60;
        int ss = seconds % 60;
        ChatFormatting color = remaining > MAX_DURABILITY / 2 ? ChatFormatting.GREEN
                : remaining > MAX_DURABILITY / 4 ? ChatFormatting.YELLOW
                : ChatFormatting.RED;
        tooltip.add(Component.literal(String.format("Carga restante: %dm %ds", mm, ss))
                .withStyle(color));
    }
}
