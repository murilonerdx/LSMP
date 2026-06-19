package br.com.murilo.liberthia.cosmic.living;

import net.minecraft.world.level.ChunkPos;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * v0.1.22 r47: <b>Observation Pressure</b> — tracker que mede o quanto
 * players estão DANDO ATENÇÃO a um chunk anômalo.
 *
 * <h2>Filosofia</h2>
 * <i>Quanto mais você olha, mais forte fica. Quanto mais você fala,
 * mais real ele se torna.</i>
 *
 * <h2>O que aumenta pressure</h2>
 * <ul>
 *   <li>Player estar olhando pro chunk (dot &gt; 0.5 do look angle)</li>
 *   <li>Mensagem de chat mencionando coordenadas do chunk</li>
 *   <li>Player parado no chunk &gt; 30s</li>
 * </ul>
 *
 * <h2>Como afeta</h2>
 * O pressure score influencia o {@link MemoryReplayManager} —
 * chance de replay é multiplicada por (1 + pressure/100).
 *
 * <h2>Decay</h2>
 * Pressure decai 1/min se ninguém olha (volta a "dormir").
 */
public final class ObservationPressure {

    /** Pressure por chunk (long key). */
    private static final Map<Long, Integer> PRESSURE = new ConcurrentHashMap<>();

    /** Pressure de pico (não decay rápido pra eventos importantes). */
    private static final Map<Long, Integer> PEAK = new ConcurrentHashMap<>();

    private ObservationPressure() {}

    public static int get(ChunkPos pos) {
        return PRESSURE.getOrDefault(pos.toLong(), 0);
    }

    public static int getPeak(ChunkPos pos) {
        return PEAK.getOrDefault(pos.toLong(), 0);
    }

    public static void add(ChunkPos pos, int delta) {
        long key = pos.toLong();
        int newVal = Math.min(100, PRESSURE.getOrDefault(key, 0) + delta);
        PRESSURE.put(key, newVal);
        if (newVal > PEAK.getOrDefault(key, 0)) {
            PEAK.put(key, newVal);
        }
    }

    public static void set(ChunkPos pos, int v) {
        PRESSURE.put(pos.toLong(), Math.max(0, Math.min(100, v)));
    }

    /** Decay tick — chamado periodicamente pelo LivingServerEvents. */
    public static void decayTick() {
        var iter = PRESSURE.entrySet().iterator();
        while (iter.hasNext()) {
            var entry = iter.next();
            int v = entry.getValue();
            if (v > 0) {
                entry.setValue(v - 1);
                if (v - 1 == 0) iter.remove();
            }
        }
    }

    /** Multiplier pra usar em other systems (ex: replay chance). */
    public static float multiplier(ChunkPos pos) {
        return 1.0F + get(pos) / 100.0F;
    }

    public static void clearAll() {
        PRESSURE.clear();
        PEAK.clear();
    }
}
