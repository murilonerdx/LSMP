package br.com.murilo.liberthia.admin.mod;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Faz HTTP requests pro mod (Forge HTTP server). Usa WebClient.
 * Cache pesado em /items e /enchantments.
 *
 * <h3>Circuit Breaker</h3>
 * Quando o mod fica offline (timeout/refused/etc), o WebClient demora ~30s
 * pra cada request — empilhando threads e dando 503 em cascata pro frontend.
 *
 * Solução: depois de {@link #FAIL_THRESHOLD} falhas consecutivas, marcar o
 * circuito como <b>OPEN</b> por {@link #OPEN_DURATION_MS}ms. Durante esse
 * tempo, qualquer chamada lança {@link ModOfflineException} <em>imediatamente</em>
 * sem tocar a rede. Depois do timeout, volta pra HALF_OPEN (próxima tentativa
 * decide). Sucesso → CLOSED, falha → OPEN de novo.
 *
 * Frontend vê 503 mod_offline rapidamente e mostra banner, sem travar UI.
 */
@Service
public class ModBridgeClient {

    private static final Logger LOG = LoggerFactory.getLogger(ModBridgeClient.class);

    /** Após N falhas consecutivas, abre o circuito. */
    private static final int FAIL_THRESHOLD = 3;
    /** Quanto tempo manter o circuito aberto antes de tentar de novo. */
    private static final long OPEN_DURATION_MS = 15_000;

    private final ModRegistry registry;
    private final ObjectMapper mapper = new ObjectMapper();

    // Estado do circuit breaker
    private final AtomicInteger consecutiveFails = new AtomicInteger(0);
    private final AtomicLong openUntilMs = new AtomicLong(0);
    private volatile String lastFailReason;
    private volatile Instant lastSuccessAt;
    private final AtomicLong totalCalls = new AtomicLong(0);
    private final AtomicLong totalFails = new AtomicLong(0);

    @Autowired
    public ModBridgeClient(ModRegistry registry) {
        this.registry = registry;
    }

    private WebClient client() { return registry.client(); }

    // ===================== CIRCUIT BREAKER =====================

    public boolean isCircuitOpen() {
        return System.currentTimeMillis() < openUntilMs.get();
    }

    /**
     * Snapshot do estado interno pro endpoint /api/mod-status mostrar
     * pro frontend sem fazer nenhuma chamada de rede.
     */
    public Map<String, Object> bridgeStatus() {
        Map<String, Object> m = new LinkedHashMap<>();
        boolean open = isCircuitOpen();
        m.put("circuitOpen", open);
        m.put("consecutiveFails", consecutiveFails.get());
        long ms = openUntilMs.get() - System.currentTimeMillis();
        m.put("openForMs", ms > 0 ? ms : 0);
        m.put("openForSec", ms > 0 ? (ms / 1000) : 0);
        m.put("lastFailReason", lastFailReason);
        m.put("lastSuccessAt", lastSuccessAt == null ? null : lastSuccessAt.toString());
        m.put("totalCalls", totalCalls.get());
        m.put("totalFails", totalFails.get());
        return m;
    }

    private void recordSuccess() {
        if (consecutiveFails.get() > 0) {
            LOG.info("[ModBridge] reconectou após {} falhas", consecutiveFails.get());
        }
        consecutiveFails.set(0);
        openUntilMs.set(0);
        lastSuccessAt = Instant.now();
        lastFailReason = null;
    }

    private void recordFailure(String reason) {
        totalFails.incrementAndGet();
        int fails = consecutiveFails.incrementAndGet();
        lastFailReason = reason;
        if (fails == FAIL_THRESHOLD) {
            // Borda — só logamos uma vez quando abrimos
            long openUntil = System.currentTimeMillis() + OPEN_DURATION_MS;
            openUntilMs.set(openUntil);
            LOG.warn("[ModBridge] CIRCUIT OPEN por {}s após {} falhas. Última razão: {}",
                    OPEN_DURATION_MS / 1000, fails, reason);
        } else if (fails > FAIL_THRESHOLD) {
            // Mantém o circuito aberto, atualiza o deadline
            openUntilMs.set(System.currentTimeMillis() + OPEN_DURATION_MS);
        }
    }

    /**
     * Lança {@link ModOfflineException} sem tocar a rede se o circuito tá aberto.
     * Frontend recebe 503 mod_offline em ~1ms em vez de esperar 30s timeout.
     */
    private void checkCircuit() {
        if (isCircuitOpen()) {
            long remainingMs = openUntilMs.get() - System.currentTimeMillis();
            throw new ModOfflineException(
                    "Mod offline (circuito aberto por mais " + (remainingMs / 1000) + "s)",
                    lastFailReason
            );
        }
    }

    public JsonNode getServerInfo() {
        return get("/api/server/info");
    }

    public JsonNode getPlayers() {
        return get("/api/players");
    }

    public JsonNode getInventory(String uuid) {
        return get("/api/player/" + uuid + "/inventory");
    }

    @Cacheable("items")
    public JsonNode getItems() {
        return get("/api/items");
    }

    @Cacheable("enchantments")
    public JsonNode getEnchantments() {
        return get("/api/enchantments");
    }

    @Cacheable("sounds")
    public JsonNode getSounds() {
        return get("/api/sounds");
    }

    @Cacheable("particles")
    public JsonNode getParticles() {
        return get("/api/particles");
    }

    @Cacheable("effects")
    public JsonNode getEffects() {
        return get("/api/effects");
    }

    @Cacheable("entities")
    public JsonNode getEntities() {
        return get("/api/entities");
    }

    public JsonNode getMatter(String uuid) {
        return get("/api/matter/" + uuid);
    }

    public JsonNode setMatter(String uuid, Map<String, Object> body) {
        return post("/api/matter/" + uuid, body);
    }

    public JsonNode giveItem(String uuid, Map<String, Object> body) {
        return post("/api/player/" + uuid + "/give", body);
    }

    public JsonNode removeItem(String uuid, Map<String, Object> body) {
        return post("/api/player/" + uuid + "/remove", body);
    }

    public JsonNode clearInventory(String uuid) {
        return post("/api/player/" + uuid + "/clear", Map.of());
    }

    public JsonNode applyEffect(String uuid, Map<String, Object> body) {
        return post("/api/player/" + uuid + "/effect", body);
    }

    public JsonNode teleport(String uuid, Map<String, Object> body) {
        return post("/api/player/" + uuid + "/teleport", body);
    }

    public JsonNode kick(String uuid, Map<String, Object> body) {
        return post("/api/player/" + uuid + "/kick", body);
    }

    public JsonNode freeze(String uuid) {
        return post("/api/player/" + uuid + "/freeze", Map.of());
    }

    /** Manda o mod tocar um clipe de voz no mundo. */
    public JsonNode voicePlay(Map<String, Object> body) {
        return post("/api/voice/play", body);
    }

    public JsonNode unfreeze(String uuid) {
        return post("/api/player/" + uuid + "/unfreeze", Map.of());
    }

    public JsonNode freezeStatus(String uuid) {
        return get("/api/player/" + uuid + "/freeze-status");
    }

    public JsonNode runCommand(String command) {
        return post("/api/command", Map.of("command", command));
    }

    // ===== World/server administrativos =====
    public JsonNode worldTime(Map<String, Object> body) { return post("/api/world/time", body); }
    public JsonNode worldWeather(Map<String, Object> body) { return post("/api/world/weather", body); }
    public JsonNode worldDifficulty(Map<String, Object> body) { return post("/api/world/difficulty", body); }
    public JsonNode operators() { return get("/api/server/operators"); }
    public JsonNode bans() { return get("/api/server/bans"); }
    public JsonNode whitelist() { return get("/api/server/whitelist"); }
    public JsonNode saveAll() { return post("/api/server/save", Map.of()); }
    public JsonNode broadcast(Map<String, Object> body) { return post("/api/server/broadcast", body); }

    // ===== Novos endpoints (player-level) =====
    public JsonNode title(String uuid, Map<String, Object> body) { return post("/api/player/" + uuid + "/title", body); }
    public JsonNode sound(String uuid, Map<String, Object> body) { return post("/api/player/" + uuid + "/sound", body); }
    public JsonNode lightning(String uuid) { return post("/api/player/" + uuid + "/lightning", Map.of()); }
    public JsonNode heal(String uuid) { return post("/api/player/" + uuid + "/heal", Map.of()); }
    public JsonNode feed(String uuid) { return post("/api/player/" + uuid + "/feed", Map.of()); }
    public JsonNode xp(String uuid, Map<String, Object> body) { return post("/api/player/" + uuid + "/xp", body); }
    public JsonNode gamemode(String uuid, Map<String, Object> body) { return post("/api/player/" + uuid + "/gamemode", body); }
    public JsonNode tpTo(String uuid, Map<String, Object> body) { return post("/api/player/" + uuid + "/tp-to", body); }

    // ===== Novos endpoints (server/world) =====
    public JsonNode spawnEntity(Map<String, Object> body) { return post("/api/world/spawn-entity", body); }
    public JsonNode spawnPlayerClone(Map<String, Object> body) { return post("/api/world/spawn-player-clone", body); }
    public JsonNode killByTag(Map<String, Object> body) { return post("/api/world/kill-by-tag", body); }
    public JsonNode particle(Map<String, Object> body) { return post("/api/world/particle", body); }
    public JsonNode explosion(Map<String, Object> body) { return post("/api/world/explosion", body); }
    public JsonNode whisper(Map<String, Object> body) { return post("/api/world/whisper", body); }
    public JsonNode healAll() { return post("/api/players/heal-all", Map.of()); }
    public JsonNode backup() { return post("/api/server/backup", Map.of()); }

    // ===== History + Snapshots =====
    public JsonNode historyChat(String uuid, long since, int limit) {
        return get("/api/history/chat?uuid=" + (uuid == null ? "" : uuid) + "&since=" + since + "&limit=" + limit);
    }
    public JsonNode historyCommands(String uuid, long since, int limit) {
        return get("/api/history/commands?uuid=" + (uuid == null ? "" : uuid) + "&since=" + since + "&limit=" + limit);
    }
    public JsonNode snapshotRunNow() { return post("/api/snapshot/run-now", Map.of()); }
    public JsonNode snapshotList(String uuid) { return get("/api/snapshot/list/" + uuid); }
    public JsonNode snapshotGet(String uuid, long ts) { return get("/api/snapshot/get/" + uuid + "/" + ts); }
    public JsonNode snapshotRestore(String uuid, Map<String, Object> body) {
        return post("/api/player/" + uuid + "/restore", body);
    }
    public JsonNode historySiteCommands(long since, int limit) {
        return get("/api/history/site-commands?since=" + since + "&limit=" + limit);
    }

    // ===== Map =====
    public JsonNode mapChunks(String dim) {
        String q = (dim == null || dim.isBlank()) ? "" : "?dim=" + dim;
        return get("/api/map/chunks" + q);
    }

    /** Retorna PNG bytes do chunk; null se 404. */
    public byte[] mapChunkPng(String dim, int cx, int cz) {
        try {
            String q = (dim == null || dim.isBlank()) ? "" : "?dim=" + dim;
            return client().get().uri("/api/map/chunk/" + cx + "/" + cz + q)
                    .accept(org.springframework.http.MediaType.IMAGE_PNG)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, r -> r.bodyToMono(String.class)
                            .map(b -> new ModBridgeException("MOD GET chunk failed: " + b)))
                    .bodyToMono(byte[].class).block();
        } catch (Exception e) {
            throw new ModBridgeException("chunk fetch error: " + e.getMessage(), e);
        }
    }

    // ===================== helpers =====================

    private JsonNode get(String path) {
        checkCircuit(); // falha rápido se mod tá conhecidamente offline
        totalCalls.incrementAndGet();
        try {
            String body = client().get().uri(path).retrieve()
                    .onStatus(HttpStatusCode::isError, r -> r.bodyToMono(String.class)
                            .map(b -> new ModBridgeException("MOD GET " + path + " failed: " + b)))
                    .bodyToMono(String.class).block();
            recordSuccess();
            return mapper.readTree(body == null ? "{}" : body);
        } catch (ModOfflineException e) {
            // Já é nossa exception — propaga sem record (checkCircuit já cobriu)
            throw e;
        } catch (Exception e) {
            // v0.1.23 DEBUG: se a falha é "invalid token", loga o token QUE FOI ENVIADO
            // pra comparação manual com o config do mod.
            String msg = e.getMessage() == null ? "" : e.getMessage();
            if (msg.contains("invalid token")) {
                String sentToken = registry.getToken();
                if (LOG.isWarnEnabled()) {
                    LOG.warn("[ModBridge] AUTH FAIL em GET {} — token ENVIADO (len={}): '{}'",
                            path, sentToken == null ? 0 : sentToken.length(),
                            sentToken == null ? "(null)" : sentToken);
                }
            }
            recordFailure(e.getClass().getSimpleName() + ": " + e.getMessage());
            throw new ModBridgeException("error GET " + path + ": " + e.getMessage(), e);
        }
    }

    private JsonNode post(String path, Object body) {
        checkCircuit();
        totalCalls.incrementAndGet();
        try {
            String response = client().post().uri(path)
                    .bodyValue(body)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, r -> r.bodyToMono(String.class)
                            .map(b -> new ModBridgeException("MOD POST " + path + " failed: " + b)))
                    .bodyToMono(String.class).block();
            recordSuccess();
            return mapper.readTree(response == null ? "{}" : response);
        } catch (ModOfflineException e) {
            throw e;
        } catch (Exception e) {
            recordFailure(e.getClass().getSimpleName() + ": " + e.getMessage());
            throw new ModBridgeException("error POST " + path + ": " + e.getMessage(), e);
        }
    }

    /**
     * Exception genérica de bridge: timeout, status code 4xx/5xx, JSON inválido.
     * Tratada como 503 mod_offline pelo GlobalExceptionHandler.
     */
    public static class ModBridgeException extends RuntimeException {
        public ModBridgeException(String msg) { super(msg); }
        public ModBridgeException(String msg, Throwable cause) { super(msg, cause); }
    }

    /**
     * Subtype: circuito está aberto (mod sabidamente offline). Joga sem tocar
     * a rede, retorna pro frontend em ms em vez de esperar 30s de timeout.
     * GlobalExceptionHandler trata com 503 + reason específico.
     */
    public static class ModOfflineException extends ModBridgeException {
        private final String details;
        public ModOfflineException(String msg, String details) {
            super(msg);
            this.details = details;
        }
        public String getDetails() { return details; }
    }
}
