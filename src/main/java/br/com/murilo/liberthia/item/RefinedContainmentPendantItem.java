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
 * Refined Containment Pendant — colar artifact v0.1.30 (evolução do
 * {@link WhiteMatterPendantItem}).
 *
 * <h3>Lore (resposta direta ao pedido do user)</h3>
 * <p>Um pingente forjado em torno de uma lasca de <b>Clear Matter Ingot
 * PURIFICADO</b> (saída do Matter Purifier). Diferente da matéria clara
 * crua — que ao tocar a pele do portador causa perda de memória, teleportes
 * involuntários e dissolução do perfil — o cristal refinado é "domado":
 * mantido fora do corpo numa estrutura inerte, ele <em>acalma</em> as três
 * matérias no entorno do portador, fazendo-as <em>comportar-se</em>.
 *
 * <h3>Mecânica</h3>
 * <ul>
 *   <li><b>Conter, não curar</b>: enquanto equipado, BLOQUEIA o ganho passivo
 *       de matter de todas as fontes — proximidade de blocos, fluidos, ar
 *       infectado, radiação Dark Matter, pisar em variantes infectadas.
 *       O perfil de matter atual <b>não muda</b> — fica congelado no nível
 *       em que estava ao equipar.</li>
 *   <li><b>Reversível</b>: se você tirar o pendant, o fluxo volta normal —
 *       a matter ambient começa a entrar de novo. O artifact "pausa", não
 *       cura.</li>
 *   <li><b>Externo (não infecta)</b>: o cristal está purificado e isolado.
 *       Equipar não custa nada — sem perda de memória, sem teleportes
 *       randômicos. Diferente de comer pílulas crus.</li>
 *   <li><b>Durabilidade</b>: 14400 ticks (12 minutos de uso contínuo). Decai
 *       1/segundo enquanto exposto a matter ambient. Quando esgota, o cristal
 *       refinado se "satura" e se quebra silenciosamente — toda matter
 *       ambient acumulada começa a manifestar efeitos imediatamente.</li>
 * </ul>
 *
 * <h3>Recipe</h3>
 * <p>1× Purified Clear Matter Ingot + 4× Gold Ingot (estrutura) + 2×
 * Amethyst Shard (estabilização) + 1× String (corrente) → 1 Refined
 * Containment Pendant.
 *
 * <p>Integração Curios: aceita slots <code>necklace</code>, <code>charm</code>.
 * Sem Curios, funciona se mantido no hotbar/inv (igual o WHITE_MATTER_PENDANT).
 */
public class RefinedContainmentPendantItem extends Item {

    /** Durabilidade em ticks. 14400 = 12 minutos. */
    public static final int MAX_DURABILITY = 14400;

    public RefinedContainmentPendantItem(Properties props) {
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
        tooltip.add(Component.literal("Equipável em Curios (necklace/charm)")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("§b• Pausa o ganho de matter de TODAS as fontes"));
        tooltip.add(Component.literal("§b• Externo — não infecta o portador"));
        tooltip.add(Component.literal("§b• Reversível — retira pra voltar o fluxo"));
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
