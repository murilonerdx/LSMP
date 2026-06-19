package br.com.murilo.liberthia.telemetry;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.telemetry.events.EventType;
import br.com.murilo.liberthia.telemetry.events.PlayerEvent;
import br.com.murilo.liberthia.telemetry.features.FeatureExtractor;
import br.com.murilo.liberthia.telemetry.features.PlayerFeatures;
import br.com.murilo.liberthia.telemetry.inference.Inferencer;
import br.com.murilo.liberthia.telemetry.inference.PlayerInference;
import br.com.murilo.liberthia.telemetry.reaction.ReactionSystem;
import br.com.murilo.liberthia.telemetry.session.PlayerSession;
import br.com.murilo.liberthia.telemetry.storage.TelemetryStorage;
import net.minecraft.server.MinecraftServer;

import java.util.Collection;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Hub central do sistema nervoso. Singleton por instância de servidor.
 *
 * Responsabilidades:
 *  1. Manter o registro de PlayerSessions ativas
 *  2. Receber eventos crus do EventCollector (Forge listeners)
 *  3. Repassar pra Timeline (memória curta) + Storage (persistência longa)
 *  4. Worker async (1-5s tick): recomputa features, infere goal/mood,
 *     dispara reactions
 *  5. Throttling do MOVE (capacita ~10Hz)
 *
 * Performance:
 *  - record() é hot path — chamado por TODO evento do MC. Mantém O(1).
 *  - O loop de features/inference roda em thread daemon SEPARADA do main.
 *  - Storage assíncrono em sua própria thread.
 *  - Nenhum I/O bloqueante na main thread MC.
 *
 * Ciclo de vida:
 *  - ServerStartedEvent     → TelemetryManager.start(server)
 *  - PlayerLoggedInEvent    → manager.onPlayerLogin(...)
 *  - PlayerLoggedOutEvent   → manager.onPlayerLogout(...)
 *  - ServerStoppingEvent    → TelemetryManager.stop()
 */
public class TelemetryManager {

    /** Intervalo do ciclo features→inference→reaction (ms). */
    private static final long ANALYSIS_INTERVAL_MS = 3_000;

    /** Throttle do MOVE — 1 evento a cada N ms por player. */
    private static final long MOVE_THROTTLE_MS = 100;

    private static TelemetryManager instance;

    private final MinecraftServer server;
    private final ConcurrentHashMap<UUID, PlayerSession> sessions = new ConcurrentHashMap<>();
    private final FeatureExtractor features = new FeatureExtractor();
    private final Inferencer inferencer = new Inferencer();
    private final ReactionSystem reactions;
    private final TelemetryStorage storage;
    private final TelemetryPusher pusher;
    private final ScheduledExecutorService analysisExecutor;

    private TelemetryManager(MinecraftServer server) {
        this.server = server;
        this.storage = new TelemetryStorage(server);
        this.reactions = new ReactionSystem(server);
        this.pusher = new TelemetryPusher(server);
        this.analysisExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "liberthia-telemetry-analysis");
            t.setDaemon(true);
            return t;
        });
    }

    // ===== Lifecycle =====

    public static synchronized void start(MinecraftServer server) {
        if (instance != null) {
            LiberthiaMod.LOGGER.warn("[Telemetry] já iniciado — ignorando start()");
            return;
        }
        instance = new TelemetryManager(server);
        instance.scheduleAnalysis();
        instance.pusher.start();
        LiberthiaMod.LOGGER.info("[Telemetry] sistema nervoso ON — análise a cada {}ms, throttle MOVE {}ms",
                ANALYSIS_INTERVAL_MS, MOVE_THROTTLE_MS);
    }

    public static synchronized void stop() {
        if (instance == null) return;
        instance.pusher.stop();
        instance.analysisExecutor.shutdown();
        try {
            instance.analysisExecutor.awaitTermination(2, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {}
        instance.storage.shutdown();
        instance.sessions.clear();
        instance = null;
        LiberthiaMod.LOGGER.info("[Telemetry] sistema nervoso OFF");
    }

    public static TelemetryManager get() { return instance; }
    public static boolean isActive() { return instance != null; }

    // ===== Session lifecycle =====

    public PlayerSession onPlayerLogin(UUID uuid) {
        PlayerSession s = new PlayerSession(uuid);
        sessions.put(uuid, s);
        return s;
    }

    public void onPlayerLogout(UUID uuid) {
        PlayerSession s = sessions.remove(uuid);
        if (s != null) {
            LiberthiaMod.LOGGER.info("[Telemetry] sessão {} encerrada — duração={}s, eventos={}",
                    uuid, s.sessionDurationMs() / 1000, s.getTimeline().size());
        }
    }

    public PlayerSession getSession(UUID uuid) { return sessions.get(uuid); }

    public Collection<PlayerSession> allSessions() {
        return Collections.unmodifiableCollection(sessions.values());
    }

    // ===== Event recording (hot path) =====

    /**
     * Registra um evento. Throttling de MOVE aplicado aqui pra reduzir
     * pressão na timeline e no storage.
     *
     * NO-OP global se config liberthia.telemetry.enabled=false — admin pode
     * desligar tudo via toml ou via endpoint do backend.
     */
    public void record(PlayerEvent e) {
        if (e == null) return;
        if (!br.com.murilo.liberthia.config.LiberthiaConfig.isTelemetryEnabled()) return;
        PlayerSession s = sessions.get(e.getPlayerUuid());
        if (s == null) return; // player não logado (ou login ainda não processado)

        // Throttle de eventos de alta frequência (MOVE)
        if (e.getType().isHighFrequency()) {
            long sinceLast = e.getTimestamp() - s.getLastMoveAt();
            if (sinceLast < MOVE_THROTTLE_MS) return; // descarta
        }

        s.onEvent(e);
        storage.enqueue(e);
    }

    public TelemetryPusher getPusher() { return pusher; }

    // ===== Analysis loop =====

    private void scheduleAnalysis() {
        analysisExecutor.scheduleWithFixedDelay(this::runAnalysisCycle,
                ANALYSIS_INTERVAL_MS, ANALYSIS_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    private void runAnalysisCycle() {
        try {
            for (PlayerSession s : sessions.values()) {
                PlayerFeatures f = features.extract(s);
                s.setFeatures(f);
                PlayerInference inf = inferencer.infer(s);
                s.setInference(inf);
                reactions.process(s);
            }
        } catch (Exception e) {
            LiberthiaMod.LOGGER.warn("[Telemetry] erro no ciclo de análise: {}", e.getMessage());
        }
    }

    // ===== Accessors =====

    public TelemetryStorage getStorage() { return storage; }
    public ReactionSystem getReactions() { return reactions; }
    public MinecraftServer getServer() { return server; }
}
