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
 * White Matter Pendant — colar cosmético/funcional v0.1.13.
 *
 * <p><b>Conceito (lore):</b> uma lasca refinada de cristal de Matéria Clara,
 * estabilizada em uma estrutura externa. Diferente da Matéria Clara crua —
 * que causa perda de memória e teleportes — o pendant é processado e mantido
 * fora do corpo. O cristal "acalma" as três matérias no entorno do portador,
 * fazendo-as <em>comportar-se</em> mas sem zerar nem alterar o perfil.
 *
 * <p><b>Mecânica:</b>
 * <ul>
 *   <li>Equipado em slot Curios (necklace/charm) → suprime efeitos de
 *       proximidade de blocos de matéria (Dark/White/Yellow Matter blocks,
 *       variantes infectadas). O perfil de matéria do player <b>continua
 *       evoluindo normalmente</b> — só os efeitos colaterais ficam pausados.</li>
 *   <li>Durabilidade: 7200 (4 horas de uso, decai 1/segundo enquanto equipado).
 *       Quando chega a 0, o pendant se quebra silenciosamente — toda matéria
 *       acumulada volta a manifestar efeitos imediatamente.</li>
 *   <li>Right-click sem Curios mostra status no chat (debugging/info).</li>
 *   <li>Não empilha (1 por slot), foil ativo.</li>
 * </ul>
 *
 * <p>Recipe: 4× Clear Matter Shard + 1× Yellow Matter Ingot (refinado) +
 * 1× Dark Matter Shard (catalisador) + 3× Gold Ingot (estrutura) →
 * 1 White Matter Pendant.
 *
 * <p>Integração Curios: ver {@link br.com.murilo.liberthia.compat.CuriosCompat}.
 */
public class WhiteMatterPendantItem extends Item {

    /** Total de ticks que o pendant dura quando equipado. */
    public static final int MAX_DURABILITY = 7200;

    public WhiteMatterPendantItem(Properties props) {
        super(props);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public boolean isDamageable(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                 List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Equipável em slot Curios (necklace/charm)")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Suprime efeitos de proximidade de matéria")
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.literal("Não zera matéria — apenas a estabiliza")
                .withStyle(ChatFormatting.WHITE));
        tooltip.add(Component.empty());
        int remaining = stack.getMaxDamage() - stack.getDamageValue();
        int minutes = remaining / 1200;
        int seconds = (remaining / 20) % 60;
        ChatFormatting durabColor = remaining > MAX_DURABILITY / 2 ? ChatFormatting.GREEN
                : remaining > MAX_DURABILITY / 4 ? ChatFormatting.YELLOW
                : ChatFormatting.RED;
        tooltip.add(Component.literal(String.format("Tempo restante: %dm %ds", minutes, seconds))
                .withStyle(durabColor));
    }
}
