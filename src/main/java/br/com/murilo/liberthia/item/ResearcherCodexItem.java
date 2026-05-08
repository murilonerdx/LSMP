package br.com.murilo.liberthia.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Codex do Pesquisador — livro de referência da lore. Right-click cicla por
 * páginas no chat. Não consome durabilidade. Cada página é um trecho da lore
 * em prosa curta.
 */
public class ResearcherCodexItem extends Item {

    private static final String TAG_PAGE = "Page";
    private static final String[] PAGES = {
            "§l§5⌘ Codex do Anfitrião — Vol. I§r\n" +
                    "§7Liberthia foi a primeira ilha mapeada. Comporta as três matérias " +
                    "simultaneamente — escura, clara e amarela. Tornou-se o ponto central " +
                    "de pesquisa do Anfitrião-Chefe e família.",

            "§l§5II — Matéria Escura§r\n" +
                    "§5Distorce a realidade. Em pequenas escalas infecta hospedeiros, transformando-os " +
                    "em marionetes. Manipulada com precisão, gera vida do nada. Pura: agressiva. " +
                    "Combinada à clara: consciente, motivada, perigosa de outra forma.",

            "§l§5III — Matéria Clara§r\n" +
                    "§fEm isolado, parece inofensiva. Mas alimenta-se de memórias — sujeitos expostos " +
                    "demonstram lapsos cognitivos, teleportes involuntários. Em hospedeiros já infectados " +
                    "pela escura, desperta consciência maligna. Em yellow: contém o caos.",

            "§l§5IV — Matéria Amarela§r\n" +
                    "§eRepele matéria escura completamente. Sozinha provoca descontrole emocional, " +
                    "alucinações, crises. Combinada à clara: o caos é estabilizado, surgindo um " +
                    "ser estrategista, frio, capaz de planos elaborados sem mudar suas motivações.",

            "§l§5V — Ilha de Horus§r\n" +
                    "§4Acessível pelo Nether. Hostilidade total — apenas 10 dos 50 pesquisadores " +
                    "retornaram, poucos coerentes. Apenas matéria escura, em estado caótico. " +
                    "Possivelmente o ponto de origem do próprio Nether. §lPortal selado.",

            "§l§5VI — Ilha Equilibrium§r\n" +
                    "§eAcessível pela dimensão Twilight. Apenas matéria amarela e clara. 5 retornaram " +
                    "de 30 — os outros §oescolheram§r§e permanecer. Entidade solar, rotacional, com " +
                    "constelações orbitando. Persuasiva. Cuidadosa. Inacessível à matéria escura.",

            "§l§5VII — Mutações Compostas§r\n" +
                    "§7Um sujeito não é a soma de suas matérias. As proporções mudam tudo:\n" +
                    "  §dSelvagem§r §8(DM puro)§r — fúria, infectante\n" +
                    "  §fCognitiva§r §8(WM puro)§r — esquecimento, teleporte\n" +
                    "  §eErrática§r §8(YM puro)§r — alucinação, descontrole\n" +
                    "  §5Simbiótica§r §8(DM+WM)§r — manipulação, planos\n" +
                    "  §6Estrategista§r §8(YM+WM)§r — frieza calculada\n" +
                    "  §cInstável§r §8(DM+YM)§r — repulsão violenta",

            "§l§5VIII — Notas Finais§r\n" +
                    "§oNem sempre uma cobaia volta. Nem sempre um pesquisador escolhe voltar. " +
                    "Mantenha o controle, registre tudo, e nunca confie em nada que sussurra de " +
                    "trás de seus olhos.§r §8— Anfitrião-Chefe"
    };

    public ResearcherCodexItem(Properties props) { super(props); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.success(stack);

        var tag = stack.getOrCreateTag();
        int page = tag.getInt(TAG_PAGE);
        // Mostra a página atual no chat
        player.sendSystemMessage(
                Component.literal("§7━━━ Página " + (page + 1) + "/" + PAGES.length + " ━━━"));
        for (String line : PAGES[page].split("\n")) {
            player.sendSystemMessage(Component.literal(line));
        }
        // Avança pra próxima
        tag.putInt(TAG_PAGE, (page + 1) % PAGES.length);
        player.getCooldowns().addCooldown(this, 10);
        return InteractionResultHolder.success(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        int page = stack.hasTag() ? stack.getTag().getInt(TAG_PAGE) : 0;
        tooltip.add(Component.literal("Página " + (page + 1) + "/" + PAGES.length)
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Right-click pra ler/avançar")
                .withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override public boolean isFoil(ItemStack stack) { return true; }
}
