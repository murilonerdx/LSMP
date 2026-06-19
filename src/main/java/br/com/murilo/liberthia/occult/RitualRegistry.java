package br.com.murilo.liberthia.occult;

import br.com.murilo.liberthia.registry.ModItems;
import net.minecraft.world.item.Items;

import java.util.Arrays;
import java.util.List;

/**
 * v0.1.22 r32: catálogo de todos os rituais disponíveis. Sistema "Java-side
 * recipe" — sem JSON datapack, pra simplicidade. Cada ritual define:
 *
 * <ul>
 *   <li><b>sigil</b>: item Sigilo que o player segura ao ativar (chave do ritual)</li>
 *   <li><b>chalkColors</b>: cores de chalk requeridas num raio 3b</li>
 *   <li><b>candleColors</b>: cores de velas ACESAS requeridas num raio 3b</li>
 *   <li><b>requiredItems</b>: items soltos no chão (item entities) que são consumidos</li>
 *   <li><b>sacrificeRequired</b>: true se precisa ter mob marcado como sacrifício recente</li>
 *   <li><b>durationTicks</b>: duração do canto antes de completar</li>
 *   <li><b>result</b>: tipo de resultado (SUMMON_ENTITY, GIVE_ITEMS, APPLY_EFFECT, TELEPORT)</li>
 *   <li><b>resultData</b>: payload específico (entity ID, item list, effect, dimension)</li>
 * </ul>
 */
public final class RitualRegistry {

    public enum ResultType { SUMMON_ENTITY, GIVE_ITEMS, APPLY_EFFECT, TELEPORT, TEACH_SPELL }

    public static class Ritual {
        public final String name;
        public final String description;
        public final java.util.function.Supplier<net.minecraft.world.item.Item> sigilSupplier;
        public final List<OccultItems.ChalkColor> chalkColors;
        public final List<OccultItems.ChalkColor> candleColors;
        public final List<java.util.function.Supplier<net.minecraft.world.item.Item>> requiredItems;
        public final boolean sacrificeRequired;
        public final int durationTicks;
        public final ResultType resultType;
        public final String resultData;

        public Ritual(String name, String description,
                      java.util.function.Supplier<net.minecraft.world.item.Item> sigil,
                      List<OccultItems.ChalkColor> chalks,
                      List<OccultItems.ChalkColor> candles,
                      List<java.util.function.Supplier<net.minecraft.world.item.Item>> items,
                      boolean sacrifice, int duration,
                      ResultType resultType, String resultData) {
            this.name = name;
            this.description = description;
            this.sigilSupplier = sigil;
            this.chalkColors = chalks;
            this.candleColors = candles;
            this.requiredItems = items;
            this.sacrificeRequired = sacrifice;
            this.durationTicks = duration;
            this.resultType = resultType;
            this.resultData = resultData;
        }
    }

    public static final List<Ritual> ALL = Arrays.asList(
            // ═════════════ SUMMONING RITUALS ═════════════
            new Ritual("Convocação do Foliot",
                    "Espírito menor — auto-mineração",
                    () -> ModItems.SIGIL_FOLIOT.get(),
                    Arrays.asList(OccultItems.ChalkColor.PURPLE),
                    Arrays.asList(OccultItems.ChalkColor.PURPLE, OccultItems.ChalkColor.PURPLE),
                    Arrays.asList(() -> Items.LAPIS_LAZULI, () -> Items.REDSTONE),
                    false, 200,
                    ResultType.GIVE_ITEMS, "liberthia:bound_foliot_crystal x1"),

            new Ritual("Convocação do Djinni",
                    "Espírito médio — gerenciamento de inventário",
                    () -> ModItems.SIGIL_DJINNI.get(),
                    Arrays.asList(OccultItems.ChalkColor.PURPLE, OccultItems.ChalkColor.GOLDEN),
                    Arrays.asList(OccultItems.ChalkColor.GOLDEN, OccultItems.ChalkColor.PURPLE),
                    Arrays.asList(() -> Items.DIAMOND, () -> Items.GOLD_INGOT),
                    true, 400,
                    ResultType.GIVE_ITEMS, "liberthia:bound_djinni_crystal x1"),

            new Ritual("Convocação do Afrit",
                    "Espírito de fogo — combate e força",
                    () -> ModItems.SIGIL_AFRIT.get(),
                    Arrays.asList(OccultItems.ChalkColor.RED, OccultItems.ChalkColor.RED),
                    Arrays.asList(OccultItems.ChalkColor.RED, OccultItems.ChalkColor.RED),
                    Arrays.asList(() -> Items.BLAZE_ROD, () -> Items.FIRE_CHARGE),
                    true, 500,
                    ResultType.GIVE_ITEMS, "liberthia:bound_afrit_crystal x1"),

            // ═════════════ GOETIC PACTS ═════════════
            new Ritual("Pacto Goético — Bael",
                    "Receba dádivas do Rei da Invisibilidade",
                    () -> ModItems.SIGIL_BAEL.get(),
                    Arrays.asList(OccultItems.ChalkColor.RED),
                    Arrays.asList(OccultItems.ChalkColor.RED, OccultItems.ChalkColor.BLACK),
                    Arrays.asList(() -> Items.ENDER_PEARL, () -> Items.GHAST_TEAR),
                    true, 600,
                    ResultType.GIVE_ITEMS, "minecraft:potion x1; minecraft:enchanted_book x1"),

            new Ritual("Pacto Goético — Lúcifer",
                    "Estrela da Manhã — sabedoria proibida",
                    () -> ModItems.SIGIL_LUCIFER.get(),
                    Arrays.asList(OccultItems.ChalkColor.RED, OccultItems.ChalkColor.GOLDEN),
                    Arrays.asList(OccultItems.ChalkColor.RED, OccultItems.ChalkColor.GOLDEN,
                            OccultItems.ChalkColor.BLACK),
                    Arrays.asList(() -> Items.NETHER_STAR, () -> Items.DIAMOND),
                    true, 1200,
                    ResultType.APPLY_EFFECT, "minecraft:strength 6000 4; minecraft:fire_resistance 6000 0"),

            // ═════════════ ANGEL EVOCATIONS ═════════════
            new Ritual("Evocação Angelica — Sandalphon",
                    "Anjo das orações — restaura saúde + remove negativos",
                    () -> ModItems.SIGIL_SANDALPHON.get(),
                    Arrays.asList(OccultItems.ChalkColor.GOLDEN, OccultItems.ChalkColor.WHITE),
                    Arrays.asList(OccultItems.ChalkColor.GOLDEN, OccultItems.ChalkColor.GOLDEN),
                    Arrays.asList(() -> Items.GOLDEN_APPLE, () -> ModItems.PRAYER_BOOK.get()),
                    false, 300,
                    ResultType.APPLY_EFFECT, "minecraft:regeneration 600 4; minecraft:absorption 600 3"),

            new Ritual("Evocação Angelica — Metatron",
                    "Voz de Deus — protección total",
                    () -> ModItems.SIGIL_METATRON.get(),
                    Arrays.asList(OccultItems.ChalkColor.GOLDEN, OccultItems.ChalkColor.WHITE,
                            OccultItems.ChalkColor.GOLDEN),
                    Arrays.asList(OccultItems.ChalkColor.GOLDEN, OccultItems.ChalkColor.GOLDEN,
                            OccultItems.ChalkColor.GOLDEN, OccultItems.ChalkColor.GOLDEN),
                    Arrays.asList(() -> Items.ENCHANTED_GOLDEN_APPLE),
                    false, 800,
                    ResultType.GIVE_ITEMS, "liberthia:halo_of_light x1"),

            // ═════════════ NECROMANCY ═════════════
            new Ritual("Necromancia — Levantar dos Mortos",
                    "Invoca undead leal",
                    () -> ModItems.SIGIL_NECRO.get(),
                    Arrays.asList(OccultItems.ChalkColor.BLACK, OccultItems.ChalkColor.RED),
                    Arrays.asList(OccultItems.ChalkColor.BLACK, OccultItems.ChalkColor.BLACK),
                    Arrays.asList(() -> Items.BONE, () -> Items.ROTTEN_FLESH, () -> Items.SKELETON_SKULL),
                    true, 400,
                    ResultType.SUMMON_ENTITY, "minecraft:zombie"),

            // ═════════════ BANISHING ═════════════
            new Ritual("Ritual de Banimento",
                    "LBRP — banishing radius 32b",
                    () -> ModItems.SIGIL_BANISHING.get(),
                    Arrays.asList(OccultItems.ChalkColor.WHITE, OccultItems.ChalkColor.WHITE,
                            OccultItems.ChalkColor.WHITE),
                    Arrays.asList(OccultItems.ChalkColor.WHITE, OccultItems.ChalkColor.WHITE,
                            OccultItems.ChalkColor.WHITE, OccultItems.ChalkColor.WHITE),
                    Arrays.asList(() -> ModItems.HOLY_WATER_BUCKET.get()),
                    false, 200,
                    ResultType.APPLY_EFFECT, "BANISH_AREA"),

            // ═════════════ TELEPORT ═════════════
            new Ritual("Salto Dimensional — Spirit World",
                    "Alternativa ao Soul Sever",
                    () -> ModItems.SIGIL_DIMENSIONAL.get(),
                    Arrays.asList(OccultItems.ChalkColor.PURPLE, OccultItems.ChalkColor.PURPLE,
                            OccultItems.ChalkColor.PURPLE),
                    Arrays.asList(OccultItems.ChalkColor.PURPLE, OccultItems.ChalkColor.PURPLE,
                            OccultItems.ChalkColor.PURPLE, OccultItems.ChalkColor.PURPLE),
                    Arrays.asList(() -> Items.AMETHYST_SHARD, () -> Items.AMETHYST_SHARD),
                    false, 300,
                    ResultType.TELEPORT, "liberthia:spirit_world"),

            // ═════════════ r36: TEACH SPELL RITUALS ═════════════
            // Re-invocar entidades mas com objetivo de aprender feitiço
            new Ritual("Pacto Goético — Aprender Lâmina do Vazio",
                    "Bael ensina sua arma de matéria não-Euclidiana",
                    () -> ModItems.SIGIL_BAEL.get(),
                    Arrays.asList(OccultItems.ChalkColor.RED, OccultItems.ChalkColor.RED),
                    Arrays.asList(OccultItems.ChalkColor.RED, OccultItems.ChalkColor.BLACK),
                    Arrays.asList(() -> Items.ENDER_PEARL, () -> Items.NETHER_STAR),
                    true, 800,
                    ResultType.TEACH_SPELL, "void_bolt"),
            new Ritual("Pacto Goético — Aprender Véu de Bael",
                    "Invisibilidade total ensinada por Bael",
                    () -> ModItems.SIGIL_BAEL.get(),
                    Arrays.asList(OccultItems.ChalkColor.RED, OccultItems.ChalkColor.BLACK),
                    Arrays.asList(OccultItems.ChalkColor.BLACK, OccultItems.ChalkColor.BLACK),
                    Arrays.asList(() -> Items.ENDER_EYE, () -> Items.SPIDER_EYE),
                    true, 600,
                    ResultType.TEACH_SPELL, "bael_invisibility"),
            new Ritual("Evocação — Aprender Bênção de Sandalphon",
                    "Cura divina ensinada pelo arcanjo das orações",
                    () -> ModItems.SIGIL_SANDALPHON.get(),
                    Arrays.asList(OccultItems.ChalkColor.GOLDEN, OccultItems.ChalkColor.WHITE),
                    Arrays.asList(OccultItems.ChalkColor.GOLDEN, OccultItems.ChalkColor.GOLDEN),
                    Arrays.asList(() -> Items.GOLDEN_APPLE),
                    false, 400,
                    ResultType.TEACH_SPELL, "sandalphon_blessing"),
            new Ritual("Evocação — Aprender Escudo de Metatron",
                    "Proteção máxima do escriba divino",
                    () -> ModItems.SIGIL_METATRON.get(),
                    Arrays.asList(OccultItems.ChalkColor.GOLDEN, OccultItems.ChalkColor.GOLDEN,
                            OccultItems.ChalkColor.WHITE),
                    Arrays.asList(OccultItems.ChalkColor.GOLDEN, OccultItems.ChalkColor.GOLDEN,
                            OccultItems.ChalkColor.GOLDEN),
                    Arrays.asList(() -> Items.ENCHANTED_GOLDEN_APPLE),
                    false, 1000,
                    ResultType.TEACH_SPELL, "metatron_shield"),
            new Ritual("Necromancia — Aprender Levantar dos Mortos",
                    "Convoca undead leal — necromante ensina",
                    () -> ModItems.SIGIL_NECRO.get(),
                    Arrays.asList(OccultItems.ChalkColor.BLACK, OccultItems.ChalkColor.RED),
                    Arrays.asList(OccultItems.ChalkColor.BLACK, OccultItems.ChalkColor.BLACK),
                    Arrays.asList(() -> Items.BONE, () -> Items.ROTTEN_FLESH, () -> Items.SKELETON_SKULL),
                    true, 600,
                    ResultType.TEACH_SPELL, "raise_dead"),
            new Ritual("Banimento — Aprender LBRP",
                    "Banishing Ritual da Golden Dawn",
                    () -> ModItems.SIGIL_BANISHING.get(),
                    Arrays.asList(OccultItems.ChalkColor.WHITE, OccultItems.ChalkColor.WHITE,
                            OccultItems.ChalkColor.WHITE),
                    Arrays.asList(OccultItems.ChalkColor.WHITE, OccultItems.ChalkColor.WHITE,
                            OccultItems.ChalkColor.WHITE, OccultItems.ChalkColor.WHITE),
                    Arrays.asList(() -> ModItems.HOLY_WATER_BUCKET.get(), () -> Items.GOLDEN_APPLE),
                    false, 1200,
                    ResultType.TEACH_SPELL, "lbrp")
    );

    public static Ritual findBySigil(net.minecraft.world.item.Item sigil) {
        for (Ritual r : ALL) {
            if (r.sigilSupplier.get() == sigil) return r;
        }
        return null;
    }
}
