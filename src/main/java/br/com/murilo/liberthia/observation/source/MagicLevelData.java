package br.com.murilo.liberthia.observation.source;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

/**
 * r157: <b>Magic Level System v2</b> — leveling 1-10 via kills + spell-use.
 *
 * <h2>Progressão</h2>
 * <ul>
 *   <li>Threshold de kills por level: <code>level × 100</code></li>
 *   <li>Level 1→2: 100 kills | 2→3: 200 | 3→4: 300 | ... 9→10: 900</li>
 *   <li>Cap em level 10.</li>
 * </ul>
 *
 * <h2>Bonus de uso de spell</h2>
 * <p>Cada vez que usa um spell pela 30ª vez (e múltiplos de 30 daí em diante),
 * ganha <b>+20 kill credits</b> — uma forma de upgar usando os feitiços
 * favoritos.
 *
 * <h2>Bônus por level</h2>
 * <ul>
 *   <li>+5% max mana/Source por level (level 10 = +50%)</li>
 *   <li>−5% custo por level (level 10 = -50%)</li>
 *   <li>+0.5% damage por level (legacy compat com r71)</li>
 * </ul>
 */
public final class MagicLevelData {

    public static final int MAX_LEVEL = 10;

    public static final String NBT_LEVEL = "liberthia.magic_level";
    public static final String NBT_KILLS = "liberthia.magic_kills_this_level";
    public static final String NBT_SPELL_USES = "liberthia.spell_uses"; // CompoundTag: spellId → int
    public static final String NBT_LUCK = "liberthia.magic_luck";
    public static final String NBT_SOURCE_BONUS = "liberthia.magic_source_bonus";
    // Legacy r71 XP (mantido pra retrocompat; novos sistemas usam kills)
    public static final String NBT_XP = "liberthia.magic_xp";

    private MagicLevelData() {}

    public static int getLevel(Player p) {
        int lv = p.getPersistentData().getInt(NBT_LEVEL);
        // Players novos começam em level 1, não 0
        if (lv < 1) lv = 1;
        return Math.min(MAX_LEVEL, lv);
    }

    public static void setLevel(Player p, int level) {
        int clamped = Math.max(1, Math.min(MAX_LEVEL, level));
        p.getPersistentData().putInt(NBT_LEVEL, clamped);
        updateDerivedStats(p, clamped);
    }

    public static int getKillsThisLevel(Player p) {
        return p.getPersistentData().getInt(NBT_KILLS);
    }

    public static int getKillsRequired(int level) {
        return level * 100;
    }

    /**
     * Adiciona N kills ao progresso e auto-levelup quando passar do threshold.
     * Retorna o novo level (igual ou maior que o anterior).
     */
    public static int addKills(Player p, int count) {
        if (count <= 0) return getLevel(p);
        int level = getLevel(p);
        if (level >= MAX_LEVEL) return level;  // já no max
        int kills = getKillsThisLevel(p) + count;
        while (level < MAX_LEVEL && kills >= getKillsRequired(level)) {
            kills -= getKillsRequired(level);
            level++;
            updateDerivedStats(p, level);
            if (p.level() instanceof net.minecraft.server.level.ServerLevel sl) {
                p.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "§d§l✦ MAGIC LEVEL UP! §r§7Nível " + level + "/" + MAX_LEVEL
                                + " §a(+5% max mana/source, −5% custo)"));
                sl.playSound(null, p.blockPosition(),
                        net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP,
                        net.minecraft.sounds.SoundSource.PLAYERS, 1F, 1.3F);
            }
        }
        p.getPersistentData().putInt(NBT_LEVEL, level);
        p.getPersistentData().putInt(NBT_KILLS, kills);
        return level;
    }

    /**
     * Registra o uso de um spell. A cada 30 usos da mesma habilidade, ganha
     * <b>+20 kill credits</b>. Retorna true se hit milestone (30, 60, 90, ...).
     */
    public static boolean recordSpellUse(Player p, String spellId) {
        if (spellId == null || spellId.isEmpty()) return false;
        CompoundTag root = p.getPersistentData().getCompound(NBT_SPELL_USES);
        int n = root.getInt(spellId) + 1;
        root.putInt(spellId, n);
        p.getPersistentData().put(NBT_SPELL_USES, root);
        if (n > 0 && n % 30 == 0) {
            addKills(p, 20);
            return true;
        }
        return false;
    }

    public static int getSpellUseCount(Player p, String spellId) {
        return p.getPersistentData().getCompound(NBT_SPELL_USES).getInt(spellId);
    }

    /** Re-deriva luck, source bonus do level atual. */
    public static void updateDerivedStats(Player p, int level) {
        // Source max bonus: +5% por level (0..50%)
        float pct = level * 0.05f;
        int sourceBonus = (int) (SourceData.DEFAULT_MAX * pct);
        p.getPersistentData().putInt(NBT_SOURCE_BONUS, sourceBonus);
        // Luck: 1% por level (max 10)
        p.getPersistentData().putFloat(NBT_LUCK, level);
        // r164 BUG FIX: usa recálculo central em vez de overwriting blindo. Antes,
        // setMax(DEFAULT + level_bonus) NUKAVA o armor bonus, causando o bug do
        // "tirar e por armadura volta ao normal" — ManaArmorEffects diff-math
        // ficava dessincronizado e só re-equipar a armadura corrigia. Agora o
        // recompute é idempotente: soma level + armor + base, sem perder nada.
        SourceData.recomputeMax(p);
    }

    public static int getSourceBonus(Player p) {
        return p.getPersistentData().getInt(NBT_SOURCE_BONUS);
    }

    public static float getLuck(Player p) {
        return p.getPersistentData().getFloat(NBT_LUCK);
    }

    /** Multiplicador de mana/source MAX. 1.0 + (level × 0.05). */
    public static float getMaxMult(Player p) {
        return 1.0f + 0.05f * getLevel(p);
    }

    /**
     * Multiplicador de CUSTO de mana e source.
     * 1.0 - (level × 0.05). Level 10 = 0.5 (custo metade).
     */
    public static float getCostMult(Player p) {
        return Math.max(0.5f, 1.0f - 0.05f * getLevel(p));
    }

    /** Damage multiplier: 1.0 + 0.005 * level (legacy compat). */
    public static float getDamageMult(Player p) {
        return 1.0f + 0.005f * getLevel(p);
    }

    /** Progresso para próximo level (0.0 → 1.0). */
    public static float getProgress(Player p) {
        int level = getLevel(p);
        if (level >= MAX_LEVEL) return 1.0f;
        int kills = getKillsThisLevel(p);
        int needed = getKillsRequired(level);
        return Math.min(1.0f, (float) kills / (float) needed);
    }

    // ─── Legacy r71 compat ────────────────────────────────────────────────
    /** Legacy r71: XP path. Now interprets as kill credits. */
    public static boolean addXp(Player p, int delta) {
        if (delta <= 0) return false;
        int oldLevel = getLevel(p);
        int newLevel = addKills(p, Math.max(1, delta / 5));
        return newLevel > oldLevel;
    }

    public static int getXp(Player p) {
        // Legacy alias for kills
        return getKillsThisLevel(p);
    }

    public static int getXpThreshold(int level) {
        return getKillsRequired(level);
    }
}
