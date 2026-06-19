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
 * v0.1.22 r23: Madness Aura — Aura da Loucura.
 *
 * <p>Item passivo. Quando holder está com ele no inv/mainhand, players num
 * raio de 20 blocos começam a TER ALUCINAÇÕES:
 * <ul>
 *   <li>Sons de mob aleatórios vindo de posições não-existentes</li>
 *   <li>Partículas formando vultos/criaturas que somem rápido</li>
 *   <li>Flashes de texto random na tela</li>
 *   <li>Sussurros incompreensíveis (whisper sounds)</li>
 * </ul>
 *
 * <p>Mecânica gerenciada por {@link br.com.murilo.liberthia.event.MadnessEvents}.
 */
public class MadnessAuraItem extends Item {

    public static final int RADIUS = 20;
    /** Ticks entre alucinações por player afetado. ~3-6s aleatório. */
    public static final int MIN_INTERVAL = 60;
    public static final int MAX_INTERVAL = 120;

    public MadnessAuraItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tip, flag);
        tip.add(Component.literal("§5§oAura da Loucura").withStyle(ChatFormatting.ITALIC));
        tip.add(Component.literal("§7Players num raio de §d20 blocos§7 começam a"));
        tip.add(Component.literal("§7§overem coisas, ouvirem vozes e enlouquecerem§r§7."));
        tip.add(Component.empty());
        tip.add(Component.literal("§8§oCarregue por sua conta e risco."));
    }
}
