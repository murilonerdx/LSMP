package br.com.murilo.liberthia.admin.controller;

import br.com.murilo.liberthia.admin.mod.ModBridgeClient;
import br.com.murilo.liberthia.admin.snapshot.SnapshotCache;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Endpoint principal — proxia tudo pro mod. Frontend usa /api/*.
 */
@RestController
@RequestMapping("/api")
public class AdminController {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final ModBridgeClient mod;
    private final SnapshotCache snapshotCache;

    // ─────────────────────────────────────────────────────────────────────
    // Hot path cache pros 2 endpoints mais polados pelo frontend.
    //
    // PROBLEMA: o frontend chama /server/info e /players a cada 1-2s. Quando
    // o mod fica intermitente (lag, restart, DDNS muda IP), o backend joga
    // ModOfflineException → 503. Traefik propaga 503/502 pro cliente em
    // cascata, frontend dá spam de retry, surge IOException Broken pipe
    // porque o cliente desiste antes do backend terminar.
    //
    // FIX: cache em memória de ~5s pra resposta válida (lastValidServerInfo /
    // lastValidPlayers). Quando o mod tá offline ou lança exception, em vez
    // de propagar 503 retornamos:
    //   - Se temos cache fresco (< STALE_MAX_MS): payload cacheado com flag
    //     online=true (frontend não percebe a "falha")
    //   - Se cache velho: payload sintético com online=false (frontend mostra
    //     banner de servidor offline, mas resposta é 200 — Traefik não vê 502)
    //
    // Reduz 502/503 cascade pra ZERO no perceived-uptime do painel.
    // ─────────────────────────────────────────────────────────────────────
    private volatile JsonNode lastValidServerInfo;
    private volatile long lastValidServerInfoTs;
    private volatile JsonNode lastValidPlayers;
    private volatile long lastValidPlayersTs;
    /** Cache HIT janela — abaixo disso retorna direto sem nem tocar o mod. */
    private static final long CACHE_HIT_MS = 2_000L;
    /** Cache STALE janela — entre HIT_MS e STALE_MS, tenta mod mas fallback no cache. */
    private static final long CACHE_STALE_MAX_MS = 30_000L;

    public AdminController(ModBridgeClient mod, SnapshotCache snapshotCache) {
        this.mod = mod;
        this.snapshotCache = snapshotCache;
    }

    @GetMapping("/server/info")
    public JsonNode serverInfo() {
        long now = System.currentTimeMillis();
        // Cache fresco (< 2s) — retorna direto sem perturbar o mod
        if (lastValidServerInfo != null && (now - lastValidServerInfoTs) < CACHE_HIT_MS) {
            return lastValidServerInfo;
        }
        try {
            JsonNode fresh = mod.getServerInfo();
            lastValidServerInfo = fresh;
            lastValidServerInfoTs = now;
            return fresh;
        } catch (Exception e) {
            // Mod offline / circuito aberto / qualquer falha → cache stale (até 30s)
            // ou payload sintético offline. Retorna 200 sempre, NUNCA propaga 503/500.
            if (lastValidServerInfo != null && (now - lastValidServerInfoTs) < CACHE_STALE_MAX_MS) {
                return lastValidServerInfo;
            }
            return offlineServerInfo(e);
        }
    }

    @GetMapping("/players")
    public JsonNode players() {
        long now = System.currentTimeMillis();
        if (lastValidPlayers != null && (now - lastValidPlayersTs) < CACHE_HIT_MS) {
            return lastValidPlayers;
        }
        try {
            JsonNode fresh = mod.getPlayers();
            lastValidPlayers = fresh;
            lastValidPlayersTs = now;
            return fresh;
        } catch (Exception e) {
            if (lastValidPlayers != null && (now - lastValidPlayersTs) < CACHE_STALE_MAX_MS) {
                return lastValidPlayers;
            }
            return offlinePlayers();
        }
    }

    /**
     * Payload sintético quando o mod tá offline há mais que o cache stale window.
     * Frontend trata {@code online=false} mostrando banner — Traefik vê 200 OK
     * e não acumula 502/503 nas métricas de saúde.
     */
    private JsonNode offlineServerInfo(Exception cause) {
        ObjectNode n = MAPPER.createObjectNode();
        n.put("online", false);
        n.put("motd", "Servidor MC offline ou inalcançável");
        n.put("tickCount", 0);
        n.put("playerCount", 0);
        n.put("maxPlayers", 0);
        n.put("tps", 0);
        n.put("worldName", "");
        // dimensions vazio (ServerInfo no frontend espera array — null daria
        // TypeError no .map())
        n.putArray("dimensions");
        n.put("source", "backend-fallback");
        n.put("reason", cause.getClass().getSimpleName() + ": " + safe(cause.getMessage()));
        return n;
    }

    private JsonNode offlinePlayers() {
        ObjectNode n = MAPPER.createObjectNode();
        n.put("online", false);
        ArrayNode arr = n.putArray("players");
        // intencionalmente vazio
        n.put("source", "backend-fallback");
        return n;
    }

    private static String safe(String s) { return s == null ? "" : s; }

    @GetMapping("/items")
    public JsonNode items() {
        return mod.getItems();
    }

    @GetMapping("/enchantments")
    public JsonNode enchantments() {
        return mod.getEnchantments();
    }

    /*
     * NOTA: estes endpoints expõem o REGISTRY do servidor MC (vanilla + mods).
     * O prefixo /api/registry/ é necessário porque /api/sounds e /api/particles
     * já são usados por SoundsController/ParticlesController (resource pack
     * upload de .ogg/.png — outra funcionalidade).
     */

    @GetMapping("/registry/sounds")
    public JsonNode soundsRegistry() {
        return mod.getSounds();
    }

    @GetMapping("/registry/particles")
    public JsonNode particlesRegistry() {
        return mod.getParticles();
    }

    @GetMapping("/registry/effects")
    public JsonNode effectsRegistry() {
        return mod.getEffects();
    }

    @GetMapping("/registry/entities")
    public JsonNode entitiesRegistry() {
        return mod.getEntities();
    }

    @PostMapping("/command")
    public JsonNode command(@RequestBody Map<String, Object> body) {
        Object cmd = body.get("command");
        if (cmd == null) throw new IllegalArgumentException("missing 'command'");
        return mod.runCommand(cmd.toString());
    }

    @GetMapping("/player/{uuid}/inventory")
    public JsonNode inventory(@PathVariable String uuid) {
        return mod.getInventory(uuid);
    }

    @PostMapping("/player/{uuid}/give")
    public JsonNode giveItem(@PathVariable String uuid, @RequestBody Map<String, Object> body) {
        return mod.giveItem(uuid, body);
    }

    @PostMapping("/player/{uuid}/remove")
    public JsonNode removeItem(@PathVariable String uuid, @RequestBody Map<String, Object> body) {
        return mod.removeItem(uuid, body);
    }

    @PostMapping("/player/{uuid}/clear")
    public JsonNode clearInventory(@PathVariable String uuid) {
        return mod.clearInventory(uuid);
    }

    @PostMapping("/player/{uuid}/effect")
    public JsonNode effect(@PathVariable String uuid, @RequestBody Map<String, Object> body) {
        return mod.applyEffect(uuid, body);
    }

    @PostMapping("/player/{uuid}/teleport")
    public JsonNode teleport(@PathVariable String uuid, @RequestBody Map<String, Object> body) {
        return mod.teleport(uuid, body);
    }

    @PostMapping("/player/{uuid}/kick")
    public JsonNode kick(@PathVariable String uuid, @RequestBody(required = false) Map<String, Object> body) {
        return mod.kick(uuid, body == null ? Map.of() : body);
    }

    /** Congela o player (não pode se mover, atacar, abrir inventário; dano crescente ao tentar). */
    @PostMapping("/player/{uuid}/freeze")
    public JsonNode freeze(@PathVariable String uuid) { return mod.freeze(uuid); }

    /** Descongela o player. */
    @PostMapping("/player/{uuid}/unfreeze")
    public JsonNode unfreeze(@PathVariable String uuid) { return mod.unfreeze(uuid); }

    @GetMapping("/player/{uuid}/freeze-status")
    public JsonNode freezeStatus(@PathVariable String uuid) { return mod.freezeStatus(uuid); }

    @GetMapping("/matter/{uuid}")
    public JsonNode getMatter(@PathVariable String uuid) {
        return mod.getMatter(uuid);
    }

    @PostMapping("/matter/{uuid}")
    public JsonNode setMatter(@PathVariable String uuid, @RequestBody Map<String, Object> body) {
        return mod.setMatter(uuid, body);
    }

    // ===== Novos endpoints =====
    @PostMapping("/player/{uuid}/title")
    public JsonNode title(@PathVariable String uuid, @RequestBody Map<String, Object> body) { return mod.title(uuid, body); }

    @PostMapping("/player/{uuid}/sound")
    public JsonNode sound(@PathVariable String uuid, @RequestBody Map<String, Object> body) { return mod.sound(uuid, body); }

    @PostMapping("/player/{uuid}/lightning")
    public JsonNode lightning(@PathVariable String uuid) { return mod.lightning(uuid); }

    @PostMapping("/player/{uuid}/heal")
    public JsonNode heal(@PathVariable String uuid) { return mod.heal(uuid); }

    @PostMapping("/player/{uuid}/feed")
    public JsonNode feed(@PathVariable String uuid) { return mod.feed(uuid); }

    @PostMapping("/player/{uuid}/xp")
    public JsonNode xp(@PathVariable String uuid, @RequestBody Map<String, Object> body) { return mod.xp(uuid, body); }

    @PostMapping("/player/{uuid}/gamemode")
    public JsonNode gamemode(@PathVariable String uuid, @RequestBody Map<String, Object> body) { return mod.gamemode(uuid, body); }

    @PostMapping("/player/{uuid}/tp-to")
    public JsonNode tpTo(@PathVariable String uuid, @RequestBody Map<String, Object> body) { return mod.tpTo(uuid, body); }

    @PostMapping("/world/spawn-entity")
    public JsonNode spawnEntity(@RequestBody Map<String, Object> body) { return mod.spawnEntity(body); }

    /** Spawna ClonePlayerEntity (renderiza como player real com PlayerModel + skin). */
    @PostMapping("/world/spawn-player-clone")
    public JsonNode spawnPlayerClone(@RequestBody Map<String, Object> body) { return mod.spawnPlayerClone(body); }

    /** Mata todas as entidades com a tag dada (qualquer dim). */
    @PostMapping("/world/kill-by-tag")
    public JsonNode killByTag(@RequestBody Map<String, Object> body) { return mod.killByTag(body); }

    @PostMapping("/world/particle")
    public JsonNode particle(@RequestBody Map<String, Object> body) { return mod.particle(body); }

    @PostMapping("/world/explosion")
    public JsonNode explosion(@RequestBody Map<String, Object> body) { return mod.explosion(body); }

    @PostMapping("/world/whisper")
    public JsonNode whisper(@RequestBody Map<String, Object> body) { return mod.whisper(body); }

    @PostMapping("/players/heal-all")
    public JsonNode healAll() { return mod.healAll(); }

    @PostMapping("/server/backup")
    public JsonNode backup() { return mod.backup(); }

    // ===== World/server admin proxies =====
    @PostMapping("/world/time")
    public JsonNode worldTime(@RequestBody Map<String, Object> body) { return mod.worldTime(body); }

    @PostMapping("/world/weather")
    public JsonNode worldWeather(@RequestBody Map<String, Object> body) { return mod.worldWeather(body); }

    @PostMapping("/world/difficulty")
    public JsonNode worldDifficulty(@RequestBody Map<String, Object> body) { return mod.worldDifficulty(body); }

    @GetMapping("/server/operators")
    public JsonNode operators() { return mod.operators(); }

    @GetMapping("/server/bans")
    public JsonNode bans() { return mod.bans(); }

    @GetMapping("/server/whitelist")
    public JsonNode whitelist() { return mod.whitelist(); }

    @PostMapping("/server/save")
    public JsonNode saveAll() { return mod.saveAll(); }

    @PostMapping("/server/broadcast")
    public JsonNode broadcastMsg(@RequestBody Map<String, Object> body) { return mod.broadcast(body); }

    // ===== History =====
    @GetMapping("/history/chat")
    public JsonNode historyChat(@RequestParam(defaultValue = "") String uuid,
                                @RequestParam(defaultValue = "0") long since,
                                @RequestParam(defaultValue = "200") int limit) { return mod.historyChat(uuid, since, limit); }

    @GetMapping("/history/commands")
    public JsonNode historyCommands(@RequestParam(defaultValue = "") String uuid,
                                    @RequestParam(defaultValue = "0") long since,
                                    @RequestParam(defaultValue = "200") int limit) { return mod.historyCommands(uuid, since, limit); }

    // ===== Snapshots =====
    @PostMapping("/snapshot/run-now")
    public JsonNode snapshotRunNow() { return mod.snapshotRunNow(); }

    /**
     * Lista snapshots: prioriza mod (live), mas faz UNION com cache local pra
     * cobrir cenário onde MC tá offline. Frontend mostra todos.
     */
    @GetMapping("/snapshot/list/{uuid}")
    public Map<String, Object> snapshotList(@PathVariable String uuid) {
        java.util.Set<Long> all = new java.util.TreeSet<>();
        try {
            JsonNode r = mod.snapshotList(uuid);
            if (r.has("snapshots")) r.get("snapshots").forEach(n -> all.add(n.asLong()));
        } catch (Exception ignored) {}
        all.addAll(snapshotCache.list(uuid));
        return Map.of("snapshots", new java.util.ArrayList<>(all));
    }

    /**
     * Busca snapshot: tenta mod primeiro; se falhar/404, lê do cache local.
     * Cada hit do mod popula o cache (redundância).
     */
    @GetMapping("/snapshot/get/{uuid}/{ts}")
    public JsonNode snapshotGet(@PathVariable String uuid, @PathVariable long ts) {
        try {
            JsonNode snap = mod.snapshotGet(uuid, ts);
            if (snap != null && !snap.isNull() && snap.size() > 1) {
                snapshotCache.store(uuid, ts, snap);
                return snap;
            }
        } catch (Exception ignored) {}
        // Fallback cache
        JsonNode cached = snapshotCache.read(uuid, ts);
        if (cached != null) return cached;
        throw new ModBridgeClient.ModBridgeException("snapshot not found (mod offline e sem cache)");
    }

    @PostMapping("/player/{uuid}/restore")
    public JsonNode restore(@PathVariable String uuid, @RequestBody Map<String, Object> body) {
        // Se o mod não tem o snapshot mas o backend tem (cache), envia o JSON cheio.
        Object tsObj = body.get("ts");
        if (tsObj != null && !body.containsKey("snapshot")) {
            try {
                long ts = ((Number) tsObj).longValue();
                JsonNode cached = snapshotCache.read(uuid, ts);
                if (cached != null) {
                    body = new java.util.HashMap<>(body);
                    body.put("snapshot", cached);
                }
            } catch (Exception ignored) {}
        }
        return mod.snapshotRestore(uuid, body);
    }

    @GetMapping("/history/site-commands")
    public JsonNode historySiteCommands(@RequestParam(defaultValue = "0") long since,
                                         @RequestParam(defaultValue = "200") int limit) {
        return mod.historySiteCommands(since, limit);
    }

    // ===== Map =====
    @GetMapping("/map/chunks")
    public JsonNode mapChunks(@RequestParam(defaultValue = "") String dim) { return mod.mapChunks(dim); }

    @GetMapping(value = "/map/chunk/{cx}/{cz}", produces = org.springframework.http.MediaType.IMAGE_PNG_VALUE)
    public org.springframework.http.ResponseEntity<byte[]> mapChunkPng(
            @PathVariable int cx, @PathVariable int cz,
            @RequestParam(defaultValue = "") String dim) {
        byte[] png = mod.mapChunkPng(dim, cx, cz);
        if (png == null) return org.springframework.http.ResponseEntity.notFound().build();
        return org.springframework.http.ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CACHE_CONTROL, "max-age=10")
                .body(png);
    }

    // Healthcheck
    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok", "service", "liberthia-admin-backend");
    }
}
