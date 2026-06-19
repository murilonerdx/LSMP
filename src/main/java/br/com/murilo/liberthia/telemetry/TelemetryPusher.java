package br.com.murilo.liberthia.telemetry;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.config.LiberthiaConfig;
import br.com.murilo.liberthia.telemetry.session.PlayerSession;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Envia snapshots de telemetria pro backend Spring periodicamente.
 *
 * Endpoint do backend: POST /api/mod/telemetry/snapshot
 *   Header: X-Liberthia-Token: <mod_token>
 *   Body: { "snapshots": [PlayerSnapshot[]], "serverInfo": {...} }
 *
 * Configurável via:
 *   liberthia.telemetry.push_interval_seconds  (default 10s)
 *   liberthia.telemetry.backend_url            (default = adminBackendUrl)
 *   liberthia.telemetry.enabled                (default true)
 *
 * Performance:
 *  - Tudo em thread daemon separada (não toca main thread)
 *  - Timeout HTTP curto (5s) — se backend tá lento, pula esse tick
 *  - Falhas silenciosas (LOG.debug) — não polui console se backend cair
 *  - Throttle: máximo 1 push em vôo por vez
 */
public class TelemetryPusher {

    private final MinecraftServer server;
    private final HttpClient http;
    private final Gson gson = new GsonBuilder().disableHtmlEscaping().create();
    private final ScheduledExecutorService scheduler;
    private volatile boolean pushInFlight = false;
    private volatile long lastSuccessfulPush = 0;
    private volatile long lastErrorTs = 0;
    private volatile String lastError = null;

    public TelemetryPusher(MinecraftServer server) {
        this.server = server;
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "liberthia-telemetry-pusher");
            t.setDaemon(true);
            return t;
        });
    }

    public void start() {
        int interval = 10;
        try { interval = LiberthiaConfig.SERVER.telemetryPushIntervalSeconds.get(); }
        catch (Exception ignored) {}
        if (interval <= 0) {
            LiberthiaMod.LOGGER.info("[TelemetryPusher] push_interval=0 → não envia snapshots pro backend");
            return;
        }
        scheduler.scheduleWithFixedDelay(this::tick, interval, interval, TimeUnit.SECONDS);
        LiberthiaMod.LOGGER.info("[TelemetryPusher] iniciado — push a cada {}s pro backend", interval);
    }

    public void stop() {
        scheduler.shutdown();
        try { scheduler.awaitTermination(2, TimeUnit.SECONDS); }
        catch (InterruptedException ignored) {}
    }

    private void tick() {
        try {
            if (!LiberthiaConfig.isTelemetryEnabled()) return;
            TelemetryManager mgr = TelemetryManager.get();
            if (mgr == null) return;
            if (pushInFlight) return;
            String url = LiberthiaConfig.telemetryBackendUrl();
            if (url == null || url.isBlank()) return;

            List<Map<String, Object>> snapshots = new ArrayList<>();
            for (PlayerSession s : mgr.allSessions()) {
                Map<String, Object> snap = buildSnapshot(s);
                if (snap != null) snapshots.add(snap);
            }
            if (snapshots.isEmpty()) return; // ninguém online → nada a enviar

            Map<String, Object> body = new HashMap<>();
            body.put("snapshots", snapshots);
            body.put("ts", System.currentTimeMillis());
            body.put("serverInfo", Map.of(
                    "playerCount", server.getPlayerCount(),
                    "maxPlayers", server.getMaxPlayers()
            ));

            String payload = gson.toJson(body);
            String token = "";
            try { token = LiberthiaConfig.SERVER.adminApiToken.get(); }
            catch (Exception ignored) {}

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url.replaceAll("/+$", "") + "/api/mod/telemetry/snapshot"))
                    .timeout(Duration.ofSeconds(5))
                    .header("Content-Type", "application/json")
                    .header("X-Liberthia-Token", token)
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();

            pushInFlight = true;
            http.sendAsync(req, HttpResponse.BodyHandlers.discarding())
                    .whenComplete((r, ex) -> {
                        pushInFlight = false;
                        if (ex != null) {
                            lastErrorTs = System.currentTimeMillis();
                            lastError = ex.getMessage();
                            LiberthiaMod.LOGGER.debug("[TelemetryPusher] falha: {}", ex.getMessage());
                        } else if (r.statusCode() >= 200 && r.statusCode() < 300) {
                            lastSuccessfulPush = System.currentTimeMillis();
                        } else {
                            lastErrorTs = System.currentTimeMillis();
                            lastError = "HTTP " + r.statusCode();
                            LiberthiaMod.LOGGER.debug("[TelemetryPusher] HTTP {}", r.statusCode());
                        }
                    });
        } catch (Exception e) {
            pushInFlight = false;
            LiberthiaMod.LOGGER.debug("[TelemetryPusher] tick error: {}", e.getMessage());
        }
    }

    /** Constrói o JSON snapshot de um player. Null se faltar dado essencial. */
    private Map<String, Object> buildSnapshot(PlayerSession s) {
        Map<String, Object> snap = new HashMap<>();
        snap.put("uuid", s.getUuid().toString());

        // Nome: tenta resolver pelo player online no momento
        ServerPlayer p = server.getPlayerList().getPlayer(s.getUuid());
        snap.put("name", p != null ? p.getName().getString() : "");
        snap.put("online", p != null);
        snap.put("lastUpdate", System.currentTimeMillis());

        // Session
        Map<String, Object> session = new HashMap<>();
        session.put("startedAt", s.getStartedAt());
        session.put("durationMs", s.sessionDurationMs());
        session.put("idleMs", s.idleTimeMs());
        session.put("dimension", s.getLastDim());
        session.put("pos", Map.of("x", s.getLastX(), "y", s.getLastY(), "z", s.getLastZ()));
        session.put("rotation", Map.of("yaw", s.getLastYaw(), "pitch", s.getLastPitch()));
        snap.put("session", session);

        // Counters
        Map<String, Object> counters = new HashMap<>();
        counters.put("deaths", s.getDeathCount());
        counters.put("kills", s.getKillCount());
        counters.put("damageDealt", s.getDamageDealt());
        counters.put("damageTaken", s.getDamageTaken());
        counters.put("blocksBroken", s.getBlocksBroken());
        counters.put("blocksPlaced", s.getBlocksPlaced());
        counters.put("chatMessages", s.getChatMessages());
        counters.put("inventoryOpens", s.getInventoryOpens());
        counters.put("craftCount", s.getCraftCount());
        counters.put("totalDistanceXZ", s.getTotalDistanceXZ());
        snap.put("counters", counters);

        // Features (snapshot do último ciclo de análise)
        if (s.getFeatures() != null) snap.put("features", s.getFeatures().toMap());
        // Inference
        if (s.getInference() != null) snap.put("inference", s.getInference().toMap());

        // Timeline meta
        snap.put("timeline", Map.of(
                "size", s.getTimeline().size(),
                "capacity", s.getTimeline().capacity()
        ));
        return snap;
    }

    // Getters pra debug/status
    public long lastSuccessfulPush() { return lastSuccessfulPush; }
    public long lastErrorTs() { return lastErrorTs; }
    public String lastError() { return lastError; }
}
