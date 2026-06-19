package br.com.murilo.liberthia.admin.engine.voice;

import br.com.murilo.liberthia.admin.engine.kv.KvConfig;
import br.com.murilo.liberthia.admin.engine.kv.KvRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Config de runtime do Whisper — persistido em {@code kv_configs} pela chave
 * {@code whisper.config}. Mudanças aplicam IMEDIATAMENTE no próximo tick do
 * scheduler, sem precisar restart do backend.
 *
 * <p>Campos:
 * <ul>
 *   <li><b>enabled</b> — liga/desliga o scheduler. Quando false, ticks pulam.</li>
 *   <li><b>scheduleEnabled</b> — se true, só processa em horários específicos</li>
 *   <li><b>scheduleHourFrom/To</b> — janela 0..24 (timezone do servidor). Ex:
 *       From=22, To=6 = "só rodar entre 22h e 6h da manhã" (madrugada).</li>
 *   <li><b>threads</b> — threads por inferência whisper-cli (1..8)</li>
 *   <li><b>beamSize</b> — hipóteses do decoder (1..10)</li>
 *   <li><b>bestOf</b> — variações antes de escolher (1..10)</li>
 *   <li><b>workers</b> — clipes paralelos (1..3, cada um carrega ~1.6GB do modelo)</li>
 *   <li><b>cpuPriority</b> — nice level 0..19 (19 = baixíssima prioridade)</li>
 * </ul>
 *
 * <p>Cache em memória de 5s — evita martelar o banco a cada tick (5s) e a
 * cada call no scheduler. Mudanças via PUT invalidam o cache imediatamente.
 */
@Service
public class WhisperRuntimeConfig {

    private static final Logger LOG = LoggerFactory.getLogger(WhisperRuntimeConfig.class);
    public static final String KV_KEY = "whisper.config";
    private static final long CACHE_TTL_MS = 5_000;

    private final KvRepository kvRepo;
    private final ObjectMapper json = new ObjectMapper();

    private volatile Snapshot cache = null;
    private volatile long cacheLoadedAt = 0L;

    public WhisperRuntimeConfig(KvRepository kvRepo) {
        this.kvRepo = kvRepo;
    }

    /** Snapshot imutável da config — passado entre métodos. */
    public static class Snapshot {
        public final boolean enabled;
        public final boolean scheduleEnabled;
        public final int scheduleHourFrom;
        public final int scheduleHourTo;
        public final int threads;
        public final int beamSize;
        public final int bestOf;
        public final int workers;
        public final int cpuPriority;
        public final Instant updatedAt;

        Snapshot(boolean enabled, boolean scheduleEnabled, int from, int to,
                 int threads, int beam, int bestOf, int workers, int nice, Instant updatedAt) {
            this.enabled = enabled;
            this.scheduleEnabled = scheduleEnabled;
            this.scheduleHourFrom = from;
            this.scheduleHourTo = to;
            this.threads = threads;
            this.beamSize = beam;
            this.bestOf = bestOf;
            this.workers = workers;
            this.cpuPriority = nice;
            this.updatedAt = updatedAt;
        }

        /** Decide se o Whisper deve processar AGORA (considera enable + schedule). */
        public boolean shouldProcessNow() {
            if (!enabled) return false;
            if (!scheduleEnabled) return true;
            int hour = LocalTime.now(ZoneId.systemDefault()).getHour();
            // Janela "fromHour..toHour" — suporta cross-midnight (ex: 22..6)
            if (scheduleHourFrom == scheduleHourTo) return false; // janela zero = nunca
            if (scheduleHourFrom < scheduleHourTo) {
                return hour >= scheduleHourFrom && hour < scheduleHourTo;
            } else {
                // wraps midnight (ex: from=22, to=6)
                return hour >= scheduleHourFrom || hour < scheduleHourTo;
            }
        }

        /** Razão textual pra logar/exibir quando shouldProcessNow=false. */
        public String pausedReason() {
            if (!enabled) return "manually disabled";
            if (scheduleEnabled) {
                int hour = LocalTime.now(ZoneId.systemDefault()).getHour();
                return String.format("outside schedule (now=%dh, window=%dh-%dh)",
                        hour, scheduleHourFrom, scheduleHourTo);
            }
            return "active";
        }

        public Map<String, Object> toMap() {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("enabled", enabled);
            m.put("scheduleEnabled", scheduleEnabled);
            m.put("scheduleHourFrom", scheduleHourFrom);
            m.put("scheduleHourTo", scheduleHourTo);
            m.put("threads", threads);
            m.put("beamSize", beamSize);
            m.put("bestOf", bestOf);
            m.put("workers", workers);
            m.put("cpuPriority", cpuPriority);
            m.put("updatedAt", updatedAt == null ? null : updatedAt.toString());
            m.put("shouldProcessNow", shouldProcessNow());
            m.put("pausedReason", pausedReason());
            return m;
        }
    }

    /**
     * Get com cache de 5s. Se ainda não tem nada no DB, retorna defaults
     * sensíveis (enabled=true, sem schedule, threads=2, beam=3, bestOf=3,
     * workers=1, nice=15).
     */
    public Snapshot get() {
        long now = System.currentTimeMillis();
        if (cache != null && (now - cacheLoadedAt) < CACHE_TTL_MS) {
            return cache;
        }
        Snapshot fresh = loadFromDb();
        cache = fresh;
        cacheLoadedAt = now;
        return fresh;
    }

    /** Força reload no próximo get() — chamado depois de PUT. */
    public void invalidate() {
        cache = null;
        cacheLoadedAt = 0L;
    }

    /**
     * Atualiza um ou mais campos via body parcial. Validação básica de ranges.
     * Retorna o novo snapshot pra resposta da API.
     */
    public Snapshot update(Map<String, Object> patch) {
        Snapshot current = get();
        boolean enabled         = boolOr(patch.get("enabled"),         current.enabled);
        boolean scheduleEnabled = boolOr(patch.get("scheduleEnabled"), current.scheduleEnabled);
        int scheduleHourFrom    = intOr(patch.get("scheduleHourFrom"), current.scheduleHourFrom, 0, 24);
        int scheduleHourTo      = intOr(patch.get("scheduleHourTo"),   current.scheduleHourTo, 0, 24);
        int threads             = intOr(patch.get("threads"),          current.threads, 1, 8);
        int beamSize            = intOr(patch.get("beamSize"),         current.beamSize, 1, 10);
        int bestOf              = intOr(patch.get("bestOf"),           current.bestOf, 1, 10);
        int workers             = intOr(patch.get("workers"),          current.workers, 1, 3);
        int cpuPriority         = intOr(patch.get("cpuPriority"),      current.cpuPriority, 0, 19);

        ObjectNode node = json.createObjectNode();
        node.put("enabled", enabled);
        node.put("scheduleEnabled", scheduleEnabled);
        node.put("scheduleHourFrom", scheduleHourFrom);
        node.put("scheduleHourTo", scheduleHourTo);
        node.put("threads", threads);
        node.put("beamSize", beamSize);
        node.put("bestOf", bestOf);
        node.put("workers", workers);
        node.put("cpuPriority", cpuPriority);

        KvConfig kv = kvRepo.findById(KV_KEY).orElse(new KvConfig(KV_KEY, ""));
        kv.setDataJson(node.toString());
        kvRepo.save(kv);

        invalidate();
        Snapshot fresh = get();
        LOG.info("[WhisperConfig] atualizado: enabled={} schedule={}({}h-{}h) threads={} beam={} bestOf={} workers={} nice={}",
                fresh.enabled, fresh.scheduleEnabled, fresh.scheduleHourFrom, fresh.scheduleHourTo,
                fresh.threads, fresh.beamSize, fresh.bestOf, fresh.workers, fresh.cpuPriority);
        return fresh;
    }

    // ============================================================
    // Helpers
    // ============================================================

    private Snapshot loadFromDb() {
        KvConfig kv = kvRepo.findById(KV_KEY).orElse(null);
        if (kv == null || kv.getDataJson() == null || kv.getDataJson().isBlank()) {
            return new Snapshot(true, false, 0, 24, 2, 3, 3, 1, 15, null);
        }
        try {
            JsonNode root = json.readTree(kv.getDataJson());
            return new Snapshot(
                    root.path("enabled").asBoolean(true),
                    root.path("scheduleEnabled").asBoolean(false),
                    clamp(root.path("scheduleHourFrom").asInt(0), 0, 24),
                    clamp(root.path("scheduleHourTo").asInt(24), 0, 24),
                    clamp(root.path("threads").asInt(2), 1, 8),
                    clamp(root.path("beamSize").asInt(3), 1, 10),
                    clamp(root.path("bestOf").asInt(3), 1, 10),
                    clamp(root.path("workers").asInt(1), 1, 3),
                    clamp(root.path("cpuPriority").asInt(15), 0, 19),
                    kv.getUpdatedAt()
            );
        } catch (Exception e) {
            LOG.warn("[WhisperConfig] falha ao parsear KV {} — usando defaults: {}", KV_KEY, e.getMessage());
            return new Snapshot(true, false, 0, 24, 2, 3, 3, 1, 15, null);
        }
    }

    private static boolean boolOr(Object v, boolean fallback) {
        if (v instanceof Boolean b) return b;
        if (v instanceof String s) return Boolean.parseBoolean(s);
        return fallback;
    }

    private static int intOr(Object v, int fallback, int min, int max) {
        if (v == null) return fallback;
        int i;
        if (v instanceof Number n) i = n.intValue();
        else { try { i = Integer.parseInt(v.toString()); } catch (Exception e) { return fallback; } }
        return clamp(i, min, max);
    }

    private static int clamp(int v, int min, int max) {
        if (v < min) return min;
        if (v > max) return max;
        return v;
    }
}
