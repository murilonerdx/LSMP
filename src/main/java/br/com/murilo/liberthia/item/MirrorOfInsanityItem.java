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
 * v0.1.22 r23: Mirror of Insanity — Espelho da Insanidade.
 *
 * <h2>Função</h2>
 * <p>Item passivo. Quando holder tem no inv, players num raio de §d15 blocos§r
 * recebem um efeito perceptual:
 * <ul>
 *   <li><b>Outros players</b>: aparecem com o NOME e SKIN distorcidos —
 *       tooltip + texto vermelho "§o(você?)§r" acima da cabeça</li>
 *   <li><b>Mobs vanilla</b>: trocam de aparência random a cada 10s
 *       (zumbi vira esqueleto, vaca vira sheep, etc — só visual)</li>
 *   <li><b>Tela do afetado</b>: brief overlay com "QUEM SOU EU?" piscando
 *       a cada 20s</li>
 * </ul>
 *
 * <p>Render override server-side dispara via {@link br.com.murilo.liberthia.event.MadnessEvents}
 * que envia packet MirrorInsanityS2C → client renderiza efeitos.
 */
public class MirrorOfInsanityItem extends Item {

    public static final int RADIUS = 15;
    /** Ticks entre updates do efeito (~10s). */
    public static final int UPDATE_INTERVAL = 200;

    public MirrorOfInsanityItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tip, flag);
        tip.add(Component.literal("§b§oEspelho da Insanidade").withStyle(ChatFormatting.ITALIC));
        tip.add(Component.literal("§7Players próximos veem distorções:"));
        tip.add(Component.literal("§7- Outros players parecem §o\"você mesmo\"§r"));
        tip.add(Component.literal("§7- Mobs trocam de aparência aleatoriamente"));
        tip.add(Component.literal("§7- Overlay §c\"QUEM SOU EU?\"§r§7 pisca"));
        tip.add(Component.empty());
        tip.add(Component.literal("§8§oReflete o que não devia ser refletido."));
    }
}
