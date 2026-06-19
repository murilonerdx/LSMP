package br.com.murilo.liberthia.item.lore;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.Random;

/**
 * Constrói {@link Items#WRITTEN_BOOK} válidos (vanilla minecraft:written_book)
 * com NBT completo — title, author, pages — pros outputs da Research Table.
 *
 * <p><b>Bug histórico:</b> {@code ResearchTableBlockEntity.getResult()} retornava
 * {@code new ItemStack(Items.WRITTEN_BOOK)} sem NBT nenhum. O client do MC,
 * ao abrir um written_book sem {@code pages}, mostra literalmente
 * "Invalid book tag" em vermelho. Pra ser um written_book válido, precisa:
 * <ul>
 *   <li>{@code title} (string, &lt;= 32 chars)</li>
 *   <li>{@code author} (string)</li>
 *   <li>{@code pages} (lista de strings — cada uma é um Component JSON-serializado,
 *       ou seja {@code Component.Serializer.toJson(Component.literal("..."))})</li>
 *   <li>{@code resolved} (byte 1) — opcional mas evita o client re-parsing
 *       legacy formatting toda vez que abre.</li>
 * </ul>
 *
 * <p>Cada chamada sorteia 1 variante de um pool — assim o jogador pesquisando
 * muitas vezes tem chance de pegar páginas diferentes (mais imersão).
 */
public final class ResearchBookFactory {

    private static final Random RNG = new Random();

    private ResearchBookFactory() {}

    // ============================================================
    // RESEARCH NOTES — dark matter shard + paper
    // ============================================================

    /** Variantes de Research Notes. Cada uma vira 1 livro com 3-4 páginas. */
    private static final List<List<String>> RESEARCH_NOTES_VARIANTS = List.of(
            // Variante 1: campo, primeiras impressões
            List.of(
                    "§l§4Notas de Campo I§r\n\n" +
                            "Encontrei o fragmento embaixo do deepslate. " +
                            "Pulsa quando minha mão chega perto. Não é magnetismo — " +
                            "é §oalgo vivo§r.\n\n" +
                            "Geiger marca 0.04 mSv/h. Acima do background.",
                    "§l§4Hipótese 1§r\n\n" +
                            "Dark Matter parece reagir a §opresença biológica§r. " +
                            "Os fragmentos perto do esqueleto ressoaram quando " +
                            "encostei. Os longe, não.\n\n" +
                            "Talvez §lconsciência§r seja o catalisador.",
                    "§l§4Aviso§r\n\n" +
                            "Não dormir perto do material por mais de 4h. " +
                            "Sintomas: §ovisões§r, perda de tempo, gosto metálico.\n\n" +
                            "Levar Clear Matter Pill no bolso. SEMPRE.",
                    "§l§4Pendente§r\n\n" +
                            "- Testar exposição a fogo\n" +
                            "- Cristalizar amostra\n" +
                            "- Pedir 2ª opinião pro Oseua\n\n" +
                            "Se eu sumir, queimem tudo isso."
            ),
            // Variante 2: notas técnicas
            List.of(
                    "§l§4Caderno Técnico§r\n\n" +
                            "Dark Matter Shard:\n" +
                            "  Densidade: ~∞ (não mensurável)\n" +
                            "  Massa: variável\n" +
                            "  Temp.: estável até 1200°C\n\n" +
                            "Forge derrete em ingot — não evapora.",
                    "§l§4Crafting§r\n\n" +
                            "Catalyst (recipe):\n" +
                            "  2x shard + blaze rod + redstone\n" +
                            "  → 1x Dark Matter Catalyst\n\n" +
                            "Usado em Auto Farmer pra produção em massa.",
                    "§l§4Cuidado§r\n\n" +
                            "Player próximo de 4+ blocos de Dark Matter por " +
                            "mais de 2 minutos: §oWither I§r aplicado.\n\n" +
                            "White Matter Pendant suprime. Recomendo equipar " +
                            "ANTES de chegar perto."
            ),
            // Variante 3: lore / superstição
            List.of(
                    "§l§4Sussurro§r\n\n" +
                            "Não escrevi por 3 dias. Acho que perdi tempo de " +
                            "novo. O frasco de Sample Vial tá vazio mas eu " +
                            "lembro de ter coletado ontem.\n\n" +
                            "§oOu foi sábado§r.",
                    "§l§4Sussurro§r\n\n" +
                            "Os outros não conseguem ler isso. Eles olham e " +
                            "veem páginas em branco.\n\n" +
                            "Talvez seja a §ldark matter§r me protegendo.\n\n" +
                            "Talvez eu esteja ficando louco.",
                    "§l§4Sussurro§r\n\n" +
                            "Se você está lendo isso e ainda enxerga letras, " +
                            "FUJA. Larga o jogo. Vai pra casa.\n\n" +
                            "§oNão é tarde.§r"
            )
    );

    public static ItemStack createResearchNotes() {
        return buildBook(
                "Research Notes",
                "Liberthia Researcher",
                pickRandom(RESEARCH_NOTES_VARIANTS)
        );
    }

    // ============================================================
    // LORE PAGES — purified essence + book
    // ============================================================

    /** Variantes de Lore Book. Cada uma vira 1 livro com 4-5 páginas. */
    private static final List<List<String>> LORE_PAGES_VARIANTS = List.of(
            // Variante 1: cronologia / queda
            List.of(
                    "§l§4A Queda§r\n\n" +
                            "Antes de Liberthia ter nome, havia §lluz§r. " +
                            "Pura, branca, sem sombra. White Matter cobria " +
                            "tudo como uma toalha de mesa.\n\n" +
                            "Aí a gente §oolhou pra ela.§r",
                    "§l§4O Olhar§r\n\n" +
                            "Quando um ser consciente observa White Matter " +
                            "pura, ela se §lparte§r em duas:\n\n" +
                            "  Metade vira §0Dark Matter§r\n" +
                            "  Metade fica §fbranca§r\n\n" +
                            "Equilíbrio quebrado. Pra sempre.",
                    "§l§4Yellow Matter§r\n\n" +
                            "Ninguém sabe de onde veio. Surgiu nas crateras " +
                            "deixadas quando Dark e White colidem.\n\n" +
                            "§oInstável§r. Muta o que toca. Causa " +
                            "Nausea só de ver.",
                    "§l§4Hoje§r\n\n" +
                            "Liberthia é o que sobrou. Um mundo de fragmentos.\n\n" +
                            "O Despertar começou quando o primeiro rift " +
                            "dimensional rasgou o céu. Está acelerando.\n\n" +
                            "Estamos no §lfim§r ou no §linício§r?"
            ),
            // Variante 2: bestiário
            List.of(
                    "§l§4Os Infectados§r\n\n" +
                            "Blocos comuns expostos a Dark Matter por muito " +
                            "tempo §omudam§r. Adquirem manchas pretas, " +
                            "irradiam.\n\n" +
                            "Pisar neles = Wither, Slowness, Blindness, Hunger.",
                    "§l§4White Bleached§r\n\n" +
                            "O oposto: blocos lavados por White Matter ficam " +
                            "§fbrancos como giz§r.\n\n" +
                            "Não machucam. Mas dão Weakness, Levitation, " +
                            "Glowing — interferem com sua percepção.",
                    "§l§4Yellow Unstable§r\n\n" +
                            "Os piores. Yellow Matter sobre dirt/sand/stone " +
                            "vira §earmadilha§r.\n\n" +
                            "Hunger, Nausea, Unluck. Você não sabe se é o " +
                            "bloco ou se é o jogo te traindo.",
                    "§l§4Resumo§r\n\n" +
                            "Geiger Counter = obrigatório.\n" +
                            "Clear Matter Pill = primeiro item do dia.\n" +
                            "White Matter Pendant = endgame, mas vale.\n\n" +
                            "Boa sorte."
            ),
            // Variante 3: receita ritual
            List.of(
                    "§l§4Receita§r\n\n" +
                            "Purificar Dark Matter:\n\n" +
                            "  1. Coloque shard no §lPurification Bench§r\n" +
                            "  2. Adicione Clear Matter\n" +
                            "  3. Aguarde 60s\n" +
                            "  4. Recolha a §oPurified Essence§r",
                    "§l§4Receita§r\n\n" +
                            "Cuidado: o processo §oexpõe§r você. Use armadura " +
                            "completa. Geiger vai gritar nos primeiros 20s.\n\n" +
                            "Se desmaiar no meio, perde tudo.",
                    "§l§4Receita§r\n\n" +
                            "Essence purified é a chave pra:\n" +
                            "  • Lore Pages (esse livro)\n" +
                            "  • White Matter Ingot\n" +
                            "  • Pendant\n\n" +
                            "Sem ela, não tem endgame.",
                    "§l§4Receita§r\n\n" +
                            "Última anotação: não tente purificar Yellow " +
                            "Matter. Sério. Vai §lexplodir§r.\n\n" +
                            "Já perdi 2 benches assim. E parte da sobrancelha."
            )
    );

    public static ItemStack createLorePages() {
        return buildBook(
                "Liberthia Codex",
                "The Order",
                pickRandom(LORE_PAGES_VARIANTS)
        );
    }

    // ============================================================
    // BUILDER COMUM
    // ============================================================

    private static ItemStack buildBook(String title, String author, List<String> pages) {
        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        CompoundTag tag = book.getOrCreateTag();

        // title: max 32 chars na vanilla GUI, mas o NBT aceita mais
        tag.putString("title", title);
        tag.putString("author", author);

        // pages: lista de strings, cada uma sendo um Component JSON serializado.
        // Component.Serializer.toJson(Component.literal(...)) gera o formato
        // correto. Se gravar texto cru ao invés de JSON-serializado, o client
        // também mostra "Invalid book tag" porque o parser de Component falha.
        ListTag pageList = new ListTag();
        for (String page : pages) {
            String json = Component.Serializer.toJson(Component.literal(page));
            pageList.add(StringTag.valueOf(json));
        }
        tag.put("pages", pageList);

        // resolved=1 evita que o client tente re-converter legacy color codes
        // (§4, §l, etc) toda vez que abre. Ele assume que já está em formato
        // Component JSON e renderiza direto. Sem isso, o client tenta parsear
        // ambos formatos e ocasionalmente choka — outra fonte do "Invalid".
        tag.putByte("resolved", (byte) 1);

        // generation=0 = original. 1 = copy of original, 2 = copy of copy,
        // 3 = tattered (não copiável). Deixa 0 pra permitir cópia normal.
        tag.putInt("generation", 0);

        return book;
    }

    private static <T> T pickRandom(List<T> options) {
        return options.get(RNG.nextInt(options.size()));
    }
}
