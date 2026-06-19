package br.com.murilo.liberthia.cosmic.insanity;

import net.minecraft.world.entity.player.Player;

/**
 * v0.1.22 r40: <b>Insanity Data</b> — 6 stats psicológicas persistidas no
 * {@code player.getPersistentData()}.
 *
 * <h2>Stats (0-100 cada)</h2>
 * <ul>
 *   <li><b>insanity</b> — sanidade fragmentada (master stat). Drena com
 *       exposição a horror cósmico.</li>
 *   <li><b>corruption</b> — corrupção orgânica/dimensional. Ganha de items
 *       infectados, blocks corrompidos, entidades parasitas.</li>
 *   <li><b>paranoia</b> — paranóia situacional. Ganha de tempo sozinho,
 *       escuridão, encontros recentes.</li>
 *   <li><b>obsession</b> — obsessão por conhecimento proibido. Ganha de uso
 *       do grimório, leitura repetida de tomes.</li>
 *   <li><b>cosmicInfluence</b> — influência cósmica direta. Ganha de
 *       Loom Dimension, rituais cósmicos.</li>
 *   <li><b>forbiddenKnowledge</b> — conhecimento que não deveria ter.
 *       Ganha de Forbidden Tome, sigils raros, encontros com cosmic entities.</li>
 * </ul>
 *
 * <p>Cada stat afeta o jogo de forma diferente — algumas drenam a outra
 * (corruption drena insanity), outras desbloqueiam features (alto
 * forbiddenKnowledge = consegue ler runas/dialetos especiais).
 *
 * <h2>Aggregate "horror level"</h2>
 * {@link #aggregateHorror} retorna 0-100 combinando todas as stats com pesos.
 * Usado pra decidir intensidade de hallucinations/paranoia events.
 *
 * <h2>NBT keys</h2>
 * Todas prefixadas {@code liberthia.insanity.*} pra colisão zero com
 * outros mods.
 */
public final class InsanityData {

    public static final String NBT_INSANITY = "liberthia.insanity.value";
    public static final String NBT_CORRUPTION = "liberthia.insanity.corruption";
    public static final String NBT_PARANOIA = "liberthia.insanity.paranoia";
    public static final String NBT_OBSESSION = "liberthia.insanity.obsession";
    public static final String NBT_COSMIC = "liberthia.insanity.cosmic";
    public static final String NBT_KNOWLEDGE = "liberthia.insanity.knowledge";

    /** Limites. */
    public static final int MAX_STAT = 100;

    /** Thresholds simbólicos. */
    public static final int THRESHOLD_LOW = 25;
    public static final int THRESHOLD_MED = 50;
    public static final int THRESHOLD_HIGH = 75;
    public static final int THRESHOLD_BREAKING = 90;

    private InsanityData() {}

    // ────────── Getters / Setters ──────────

    public static int getInsanity(Player p)            { return p.getPersistentData().getInt(NBT_INSANITY); }
    public static int getCorruption(Player p)          { return p.getPersistentData().getInt(NBT_CORRUPTION); }
    public static int getParanoia(Player p)            { return p.getPersistentData().getInt(NBT_PARANOIA); }
    public static int getObsession(Player p)           { return p.getPersistentData().getInt(NBT_OBSESSION); }
    public static int getCosmicInfluence(Player p)     { return p.getPersistentData().getInt(NBT_COSMIC); }
    public static int getForbiddenKnowledge(Player p)  { return p.getPersistentData().getInt(NBT_KNOWLEDGE); }

    public static void setInsanity(Player p, int v)           { p.getPersistentData().putInt(NBT_INSANITY, clamp(v)); }
    public static void setCorruption(Player p, int v)         { p.getPersistentData().putInt(NBT_CORRUPTION, clamp(v)); }
    public static void setParanoia(Player p, int v)           { p.getPersistentData().putInt(NBT_PARANOIA, clamp(v)); }
    public static void setObsession(Player p, int v)          { p.getPersistentData().putInt(NBT_OBSESSION, clamp(v)); }
    public static void setCosmicInfluence(Player p, int v)    { p.getPersistentData().putInt(NBT_COSMIC, clamp(v)); }
    public static void setForbiddenKnowledge(Player p, int v) { p.getPersistentData().putInt(NBT_KNOWLEDGE, clamp(v)); }

    // ────────── Add / sub helpers ──────────

    public static void addInsanity(Player p, int delta)          { setInsanity(p, getInsanity(p) + delta); }
    public static void addCorruption(Player p, int delta)        { setCorruption(p, getCorruption(p) + delta); }
    public static void addParanoia(Player p, int delta)          { setParanoia(p, getParanoia(p) + delta); }
    public static void addObsession(Player p, int delta)         { setObsession(p, getObsession(p) + delta); }
    public static void addCosmicInfluence(Player p, int delta)   { setCosmicInfluence(p, getCosmicInfluence(p) + delta); }
    public static void addForbiddenKnowledge(Player p, int delta){ setForbiddenKnowledge(p, getForbiddenKnowledge(p) + delta); }

    /**
     * "Horror level" agregado (0-100). Pesos baseados em narrative impact:
     * insanity é dominante (×0.30), forbiddenKnowledge e cosmicInfluence
     * são potenciadores (×0.20 cada), corruption + paranoia + obsession
     * dividem o resto.
     */
    public static int aggregateHorror(Player p) {
        double agg = getInsanity(p) * 0.30
                + getForbiddenKnowledge(p) * 0.20
                + getCosmicInfluence(p) * 0.20
                + getCorruption(p) * 0.10
                + getParanoia(p) * 0.10
                + getObsession(p) * 0.10;
        return Math.max(0, Math.min(100, (int) Math.round(agg)));
    }

    /** Reseta TUDO (admin/death/prayer cure). */
    public static void resetAll(Player p) {
        var d = p.getPersistentData();
        d.remove(NBT_INSANITY);
        d.remove(NBT_CORRUPTION);
        d.remove(NBT_PARANOIA);
        d.remove(NBT_OBSESSION);
        d.remove(NBT_COSMIC);
        d.remove(NBT_KNOWLEDGE);
    }

    /** Snapshot pra debug/admin GUI. */
    public static String snapshot(Player p) {
        return String.format("insanity=%d corruption=%d paranoia=%d obsession=%d cosmic=%d knowledge=%d horror=%d",
                getInsanity(p), getCorruption(p), getParanoia(p),
                getObsession(p), getCosmicInfluence(p), getForbiddenKnowledge(p),
                aggregateHorror(p));
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(MAX_STAT, v));
    }
}
