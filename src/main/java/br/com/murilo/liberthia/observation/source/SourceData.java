package br.com.murilo.liberthia.observation.source;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

/**
 * v0.1.22 r61: <b>Source Data</b> — per-player mana ("Source") storage.
 *
 * <p>Inspired by Ars Nouveau's {@code IManaCap} but stored via player
 * persistent data (NBT) instead of capability — simpler, fewer moving parts.
 *
 * <h2>Filosofia diferente de mana</h2>
 * Source não recarrega por tempo. Source recarrega quando você §lobserva§r —
 * estar parado, olhando o mundo, regenera Source. Andar/correndo NÃO regen.
 *
 * <h2>Source vs Sanity</h2>
 * <ul>
 *   <li>Source = combustível dos feitiços (regenera observando)</li>
 *   <li>Sanity = saúde mental (drena com horror, regenera dormindo)</li>
 * </ul>
 *
 * <p>Os dois são separados. Você pode ter Source 100 e Sanity 0.
 */
public final class SourceData {

    public static final String NBT_SOURCE = "liberthia.observation_source";
    public static final String NBT_MAX_SOURCE = "liberthia.observation_max_source";
    public static final int DEFAULT_MAX = 100;

    private SourceData() {}

    public static int getMax(Player p) {
        var data = p.getPersistentData();
        if (!data.contains(NBT_MAX_SOURCE)) {
            data.putInt(NBT_MAX_SOURCE, DEFAULT_MAX);
            return DEFAULT_MAX;
        }
        return data.getInt(NBT_MAX_SOURCE);
    }

    public static void setMax(Player p, int max) {
        p.getPersistentData().putInt(NBT_MAX_SOURCE, Math.max(1, max));
    }

    public static int get(Player p) {
        var data = p.getPersistentData();
        if (!data.contains(NBT_SOURCE)) {
            int max = getMax(p);
            data.putInt(NBT_SOURCE, max);
            return max;
        }
        return data.getInt(NBT_SOURCE);
    }

    public static void set(Player p, int value) {
        int clamped = Math.max(0, Math.min(getMax(p), value));
        p.getPersistentData().putInt(NBT_SOURCE, clamped);
    }

    public static void add(Player p, int delta) {
        set(p, get(p) + delta);
    }

    /** Tenta consumir source. Retorna true se conseguiu. */
    public static boolean consume(Player p, int amount) {
        if (amount <= 0) return true;
        if (br.com.murilo.liberthia.magic.antimagic.AntiMagic.isSuppressed(p)) {
            br.com.murilo.liberthia.magic.antimagic.AntiMagic.notifySuppressed(p);
            return false;
        }
        int cur = get(p);
        if (cur < amount) {
            br.com.murilo.liberthia.LiberthiaMod.LOGGER.warn(
                    "[Source] consume({}, {}) FAILED — has {}",
                    p.getName().getString(), amount, cur);
            return false;
        }
        add(p, -amount);
        br.com.murilo.liberthia.magic.antimagic.AntiMagic.markCast(p);
        br.com.murilo.liberthia.LiberthiaMod.LOGGER.debug(
                "[Source] {} consumed {} (was {}, now {})",
                p.getName().getString(), amount, cur, cur - amount);
        return true;
    }

    /** r153: força consumir — clamps a 0 se ultrapassar.
     *  Use quando o cast já EXECUTOU e o mana DEVE ser deduzido (mesmo que falte). */
    public static void forceConsume(Player p, int amount) {
        if (amount <= 0) return;
        int cur = get(p);
        int newVal = Math.max(0, cur - amount);
        set(p, newVal);
        br.com.murilo.liberthia.LiberthiaMod.LOGGER.debug(
                "[Source] {} FORCE consumed {} (was {}, now {})",
                p.getName().getString(), amount, cur, newVal);
    }

    /**
     * r164: <b>Recálculo central do max source</b>. Soma todas as fontes de bônus
     * idempotentemente em vez de aritmética diff (que ficava dessincronizada
     * quando MagicLevelData e ManaArmorEffects sobrescreviam max em sequência).
     *
     * <p><b>Composição</b>:
     * <ul>
     *   <li>DEFAULT_MAX (100)</li>
     *   <li>+ Magic Level bonus (5% × level × DEFAULT_MAX, até +50)</li>
     *   <li>+ Spirit Robes bonus (20 × peças equipadas, até +80)</li>
     * </ul>
     *
     * <p>Chamado por: PlayerTickEvent (1×/s via ManaArmorEffects),
     * MagicLevelData.updateDerivedStats, login, gain XP.
     *
     * <p>NÃO mexe no current source — só ajusta max. Se cur > newMax após
     * recompute, a próxima leitura de {@link #get(Player)} ainda retorna
     * cur (não auto-clampa pra preservar mana que o player já tem).
     */
    public static void recomputeMax(Player p) {
        int base = DEFAULT_MAX;
        // Level bonus (idempotente — lê do NBT, não acumula)
        int levelBonus = p.getPersistentData().getInt(
                br.com.murilo.liberthia.observation.source.MagicLevelData.NBT_SOURCE_BONUS);
        // Armor bonus (idempotente — conta robes equipadas agora)
        int armorBonus = 0;
        try {
            int pieces = br.com.murilo.liberthia.magic.armor.ManaArmorEffects.countSpiritRobes(p);
            armorBonus = pieces * br.com.murilo.liberthia.magic.armor.ManaArmorEffects.BONUS_PER_PIECE;
        } catch (Throwable ignored) {
            // SpiritRobes pode não estar carregado em alguns contextos
        }
        // r179: Affinity Ring CAPACITY (+50), conta inventário OU slot Curios.
        int ringBonus = 0;
        try {
            if (br.com.murilo.liberthia.magic.affinity.AffinityRings.isWorn(p,
                    br.com.murilo.liberthia.magic.affinity.AffinityRingItem.Type.CAPACITY)) {
                ringBonus = 50;
            }
        } catch (Throwable ignored) {
            // affinity module ausente em algum contexto
        }
        int newMax = base + levelBonus + armorBonus + ringBonus;
        setMax(p, Math.max(DEFAULT_MAX, newMax));
    }
}
