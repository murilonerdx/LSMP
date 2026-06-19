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
 * Refined Containment Glove — artifact pareado do
 * {@link RefinedContainmentPendantItem}.
 *
 * <h3>Lore</h3>
 * <p>Luva forrada com fragmentos de Clear Matter Ingot Purificado costurados
 * no couro. Cobre as palmas — onde o contato direto com matter crua faz mais
 * estrago. O ingot purificado age como uma "barreira inerte" pra contato
 * tátil específico: segurar dark/yellow matter shard sem se infectar.
 *
 * <h3>Mecânica</h3>
 * <ul>
 *   <li><b>Toque seguro</b>: enquanto equipado, BLOQUEIA o ganho de matter via
 *       contato com items na mão ({@code MatterContactHandler}). Você pode
 *       mexer com dark/clear/yellow matter raw sem subir o perfil.</li>
 *   <li><b>Sinérgico com o Pendant</b>: pendant cuida do AMBIENTE (proximidade,
 *       radiação, pisar). Glove cuida do CONTATO (mão segurando matter).
 *       Equipar os dois = imunidade quase total ao ganho passivo.</li>
 *   <li><b>Externo (não infecta)</b>: ingot purificado isolado por couro, sem
 *       contato com a pele.</li>
 *   <li><b>Durabilidade</b>: 9600 ticks (8 minutos). Decai só quando você
 *       efetivamente toca matter raw — usar a luva pra caminhar é grátis.</li>
 * </ul>
 *
 * <h3>Recipe</h3>
 * <p>1× Purified Clear Matter Ingot + 4× Leather + 2× String → 1 Refined
 * Containment Glove.
 */
public class RefinedContainmentGloveItem extends Item {

    /** Durabilidade em "toques" — cada interação com matter raw drena 1. */
    public static final int MAX_DURABILITY = 9600;

    public RefinedContainmentGloveItem(Properties props) {
        super(props);
    }

    @Override public boolean isFoil(ItemStack stack) { return true; }
    @Override public boolean isDamageable(ItemStack stack) { return true; }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                 List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("✦ Artifact de Matéria Branca Refinada")
                .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.ITALIC));
        tooltip.add(Component.empty());
        tooltip.add(Component.literal("Equipável em Curios (hands/bracelet)")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("§b• Bloqueia ganho de matter por CONTATO (na mão)"));
        tooltip.add(Component.literal("§b• Externo — não infecta"));
        tooltip.add(Component.literal("§b• Combine com o Pendant pra cobertura total"));
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
