package br.com.murilo.liberthia.admin.controller;

import br.com.murilo.liberthia.admin.config.BackendConfig;
import br.com.murilo.liberthia.admin.config.BackendConfigService;
import br.com.murilo.liberthia.admin.mod.ModBridgeClient;
import br.com.murilo.liberthia.admin.mod.ModRegistry;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Endpoints admin pra inspecionar e atualizar a config persistida do mod
 * (atualmente, só o token). Rota {@code /api/admin/mod-config/*} — protegida
 * pelo {@link br.com.murilo.liberthia.admin.auth.AuthFilter} padrão (precisa de
 * Bearer JWT admin).
 *
 * <p>Resolve o problema histórico: o token do compose (env var MOD_TOKEN) e o
 * que o mod usa (gerado em {@code liberthia-server.toml}) divergem porque o
 * operador esquece de sincronizar. Agora o operador atualiza aqui sem precisar
 * editar compose nem reiniciar container.
 */
@RestController
@RequestMapping("/api/admin/mod-config")
public class ModConfigController {

    private static final Logger log = LoggerFactory.getLogger(ModConfigController.class);

    /** UUID v4 simplificado — 8-4-4-4-12 hex. Cobre o formato do mod. */
    private static final Pattern UUID_PATTERN = Pattern.compile(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"
    );

    private final BackendConfigService backendConfig;
    private final ModRegistry registry;
    private final ModBridgeClient bridge;

    public ModConfigController(BackendConfigService backendConfig, ModRegistry registry, ModBridgeClient bridge) {
        this.backendConfig = backendConfig;
        this.registry = registry;
        this.bridge = bridge;
    }

    @GetMapping("/token")
    public Map<String, Object> getToken() {
        Map<String, Object> m = new LinkedHashMap<>();
        String current = registry.getToken();
        String registered = backendConfig.getValue(BackendConfig.KEY_MOD_TOKEN);
        m.put("tokenFingerprint", fingerprint(current));
        m.put("source", registry.getTokenSource());
        m.put("lastRegisteredAt", asString(registry.getLastRegisteredAt()));
        m.put("lastUpdatedAt", asString(backendConfig.getUpdatedAt(BackendConfig.KEY_MOD_TOKEN)));
        // currentMatchesRegistered: true se o token EM USO bate com o que tá
        // persistido no DB. Frontend usa pra detectar quando alguém edita o env
        // var sem rebuild e o boot ainda carregou de lá.
        m.put("currentMatchesRegistered",
                registered != null && registered.equals(current));
        m.put("hasOverride", registered != null && !registered.isBlank());
        // v0.1.48+: lock é PERMANENTE — token só muda via este endpoint.
        // Mantemos o campo no payload pra compat com frontend existente.
        m.put("manualLock", true);
        return m;
    }

    /**
     * v0.1.48+: o "unlock" é OBSOLETO — o painel é a única fonte de token.
     * Endpoint mantido só pra compat de frontend (vira no-op + 410 Gone).
     */
    @PostMapping("/unlock")
    public ResponseEntity<Map<String, Object>> unlock() {
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("ok", false);
        resp.put("error", "Operação removida — o token agora SÓ muda via POST /api/admin/mod-config/token. " +
                "O auto-register do mod nunca sobrescreve mais.");
        resp.put("at", Instant.now().toString());
        return ResponseEntity.status(410).body(resp); // 410 Gone
    }

    @PostMapping("/token")
    public ResponseEntity<Map<String, Object>> setToken(@RequestBody Map<String, Object> body) {
        Object raw = body == null ? null : body.get("token");
        String token = raw == null ? "" : raw.toString().trim();
        if (token.isEmpty()) {
            return badRequest("token vazio");
        }
        if (!UUID_PATTERN.matcher(token).matches()) {
            return badRequest("token não é um UUID válido (esperado formato: xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx)");
        }
        try {
            registry.applyToken(token);
        } catch (Exception e) {
            log.error("[ModConfigController] falha ao aplicar token: {}", e.getMessage());
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("ok", false);
            err.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(err);
        }
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("ok", true);
        resp.put("tokenFingerprint", fingerprint(token));
        resp.put("source", "DB");
        resp.put("at", Instant.now().toString());
        return ResponseEntity.ok(resp);
    }

    /**
     * Faz uma chamada real {@code /api/server/info} no mod com o token atual e
     * retorna se deu certo. Útil pra validar que o novo token funcionou ANTES
     * do operador depender do fluxo normal (que tem cache + circuit breaker
     * mascarando falhas).
     */
    @PostMapping("/test-connection")
    public Map<String, Object> testConnection() {
        Map<String, Object> m = new LinkedHashMap<>();
        try {
            JsonNode info = bridge.getServerInfo();
            m.put("ok", true);
            m.put("tps", info.path("tps").asDouble(0));
            m.put("playerCount", info.path("playerCount").asInt(0));
            m.put("maxPlayers", info.path("maxPlayers").asInt(0));
            m.put("motd", info.path("motd").asText(""));
            m.put("tokenFingerprint", fingerprint(registry.getToken()));
        } catch (Exception e) {
            m.put("ok", false);
            m.put("error", e.getClass().getSimpleName() + ": " + (e.getMessage() == null ? "" : e.getMessage()));
            m.put("tokenFingerprint", fingerprint(registry.getToken()));
        }
        return m;
    }

    // ============ helpers ============

    /**
     * Retorna {@code primeiros8...últimos4} do token. NUNCA o token completo —
     * mesmo em endpoint admin, não tem motivo de devolver o segredo cru pro
     * cliente (que pode logar/printar).
     */
    private static String fingerprint(String token) {
        if (token == null || token.isBlank()) return "(vazio)";
        if (token.length() < 12) return token.substring(0, Math.min(4, token.length())) + "...";
        return token.substring(0, 8) + "..." + token.substring(token.length() - 4);
    }

    private static String asString(Instant i) {
        return i == null ? null : i.toString();
    }

    private static ResponseEntity<Map<String, Object>> badRequest(String msg) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("ok", false);
        m.put("error", msg);
        return ResponseEntity.badRequest().body(m);
    }
}
