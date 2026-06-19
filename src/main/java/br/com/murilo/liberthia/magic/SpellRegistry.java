package br.com.murilo.liberthia.magic;

import net.minecraft.world.item.Rarity;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * v0.1.22 r36: Catálogo de TODOS os feitiços disponíveis no mod.
 *
 * <p>Cada spell é ensinado por uma entidade específica (ver {@code sourceEntity}).
 * O ritual de invocação dessa entidade tem chance de drop do Spell pro Grimoire
 * do caster.
 *
 * <h2>15 feitiços iniciais distribuídos em 5 escolas</h2>
 */
public final class SpellRegistry {

    public static final Map<String, Spell> ALL = new LinkedHashMap<>();

    static {
        // ────── COSMIC / HAUNTOLOGIA ──────
        reg(new Spell("void_bolt", "Lâmina do Vazio", SpellType.PROJECTILE,
                "cosmic", 15, 20, 8.0F, 24F, 0x6E2DA0, Rarity.UNCOMMON,
                "Projétil de matéria não-Euclidiana. Atravessa armaduras.",
                "Azathoth"));
        reg(new Spell("reality_tear", "Rasgar Realidade", SpellType.BEAM,
                "cosmic", 30, 80, 4.0F, 16F, 0xFF0099, Rarity.EPIC,
                "Beam de distorção dimensional. Cega + dano sustained.",
                "Nyarlathotep"));
        reg(new Spell("witness_curse", "Maldição da Testemunha", SpellType.CURSE,
                "cosmic", 20, 100, 0F, 12F, 0x440080, Rarity.RARE,
                "Alvo passa a ouvir todos os gritos do servidor.",
                "Whispering Veil"));

        // ────── GOETIA (demônios) ──────
        reg(new Spell("bael_invisibility", "Véu de Bael", SpellType.SELF_BUFF,
                "goetia", 25, 200, 0F, 0F, 0x1A0030, Rarity.RARE,
                "Invisibilidade total por 30s. Bael, Rei do Inferno.",
                "Bael"));
        reg(new Spell("lucifer_fire", "Chama de Lúcifer", SpellType.AOE,
                "goetia", 40, 100, 12.0F, 6F, 0xFFCC33, Rarity.EPIC,
                "Explosão de fogo divino-caído. Queima 10s.",
                "Lúcifer"));
        reg(new Spell("goetic_drain", "Drenagem Demoníaca", SpellType.DRAIN,
                "goetia", 18, 60, 4.0F, 8F, 0x880033, Rarity.RARE,
                "Drena 4HP de inimigos próximos pra caster.",
                "Asmodeus"));

        // ────── KABBALAH (anjos) ──────
        reg(new Spell("sandalphon_blessing", "Bênção de Sandalphon", SpellType.HEAL,
                "kabbalah", 20, 80, 12.0F, 0F, 0xFFD700, Rarity.RARE,
                "Cura 12HP + remove debuffs negativos.",
                "Sandalphon"));
        reg(new Spell("metatron_shield", "Escudo de Metatron", SpellType.SELF_BUFF,
                "kabbalah", 30, 200, 0F, 0F, 0xFFEEAA, Rarity.EPIC,
                "Resistance V + Fire Resistance por 60s.",
                "Metatron"));
        reg(new Spell("ophanim_judgment", "Julgamento dos Ophanim", SpellType.PROJECTILE,
                "kabbalah", 35, 60, 15.0F, 32F, 0xFFFFCC, Rarity.EPIC,
                "Raio dourado que ignora armor. +50% dmg vs undead.",
                "Sino dos Ophanim"));

        // ────── NECROMANCIA ──────
        reg(new Spell("raise_dead", "Levantar dos Mortos", SpellType.SUMMON,
                "necro", 25, 1200, 0F, 0F, 0x222222, Rarity.RARE,
                "Invoca 1 zumbi leal por 2 minutos.",
                "Necromante"));
        reg(new Spell("bone_spike", "Estaca de Osso", SpellType.PROJECTILE,
                "necro", 12, 30, 6.0F, 20F, 0xEEDDCC, Rarity.UNCOMMON,
                "Projétil ósseo perfurante. +100% dmg vs vivos.",
                "Necromante"));

        // ────── HERMETISMO / ESPÍRITOS ──────
        reg(new Spell("foliot_grasp", "Garra do Foliot", SpellType.CURSE,
                "hermetism", 10, 40, 2.0F, 10F, 0x554433, Rarity.COMMON,
                "Raízes prendem alvo (Slowness IV 5s).",
                "Foliot"));
        reg(new Spell("djinni_wind", "Vento do Djinni", SpellType.AOE,
                "hermetism", 22, 80, 6.0F, 10F, 0x9999FF, Rarity.RARE,
                "Empurra todos mobs num raio 10b + 6 dmg.",
                "Djinni"));
        reg(new Spell("astral_leap", "Salto Astral", SpellType.TELEPORT,
                "hermetism", 18, 60, 0F, 32F, 0xCCAAFF, Rarity.RARE,
                "Teleporta até 32b na direção do olhar.",
                "Salto Dimensional"));

        // ────── BANIMENTO ──────
        reg(new Spell("lbrp", "Banimento Menor (LBRP)", SpellType.AOE,
                "ceremonial", 45, 600, 0F, 32F, 0xFFFFFF, Rarity.EPIC,
                "Empurra todos hostis 40 blocos longe. Golden Dawn ritual.",
                "Banimento"));

        // ────── COSMIC COLLAPSE (r41 — skill cinematográfica) ──────
        reg(new Spell("cosmic_collapse", "Colapso Cósmico", SpellType.AOE,
                "cosmic", 95, 2400, 24F, 16F, 0x7723B0, Rarity.EPIC,
                "Rasga a realidade. 14s cinematográficos. 6 phases. Devastador.",
                "Azathoth"));
    }

    private static void reg(Spell s) { ALL.put(s.id, s); }

    public static Spell get(String id) { return ALL.get(id); }
    public static boolean exists(String id) { return ALL.containsKey(id); }
    public static java.util.Collection<Spell> all() { return ALL.values(); }
}
