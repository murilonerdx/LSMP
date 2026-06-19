package br.com.murilo.liberthia.admin.engine.telemetry;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Endpoints de telemetria.
 *
 * Recebe do mod:
 *   POST /api/mod/telemetry/snapshot  (X-Liberthia-Token)
 *
 * Servidos pro frontend (auth admin):
 *   GET  /api/telemetry/status          → status geral + enabled
 *   GET  /api/telemetry/players         → lista de players com snapshot
 *   GET  /api/telemetry/player/{uuid}   → detalhe completo
 *   PUT  /api/telemetry/config          → liga/desliga ({enabled: true/false})
 *
 * Quando admin desabilita via /api/telemetry/config, o mod ainda envia
 * snapshots (não consegue saber até o próximo push), mas o backend descarta.
 * O frontend mostra "DESABILITADO" no banner. Pra desligar mesmo: mexer
 * no liberthia.telemetry.enabled do toml do mod.
 */
@RestController
public class TelemetryController {

    private final TelemetryService svc;

    @Value("${mod.token:}")
    private String modToken;

    /**
     * Token público pro endpoint /api/public/telemetry/* — usado por scripts
     * de monitoramento externo. DEFINA via env TELEMETRY_PUBLIC_TOKEN (sem
     * default — endpoint fica desabilitado/negado se vazio).
     *
     * IMPORTANTE: é um secret de leitura — quem tem o token pode SOMENTE ler
     * os snapshots em memória (sem escrever, sem comando, sem JWT). Mesmo
     * assim, vaza posições e padrões de comportamento dos players. Trocar
     * em produção via env.
     */
    @Value("${telemetry.public-token:}")
    private String publicToken;

    public TelemetryController(TelemetryService svc) {
        this.svc = svc;
    }

    // ===== Mod → Backend (token-checked, sem JWT) =====
    @PostMapping("/api/mod/telemetry/snapshot")
    public ResponseEntity<?> ingest(@RequestBody Map<String, Object> body, HttpServletRequest req) {
        String tok = req.getHeader("X-Liberthia-Token");
        if (modToken != null && !modToken.isBlank() && !modToken.equals(tok)) {
            return ResponseEntity.status(401).body(Map.of("error", "bad token"));
        }
        if (!svc.isEnabled()) {
            // Admin desligou via painel — descarta silenciosamente, OK 200 pra
            // o mod não retentar.
            return ResponseEntity.ok(Map.of("ok", true, "ingested", 0, "note", "telemetry disabled"));
        }
        Object snaps = body.get("snapshots");
        if (!(snaps instanceof List<?> list)) {
            return ResponseEntity.badRequest().body(Map.of("error", "missing snapshots"));
        }
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> typed = (List<Map<String, Object>>) list;
        svc.ingestBatch(typed);
        return ResponseEntity.ok(Map.of("ok", true, "ingested", typed.size()));
    }

    // ===== Frontend → Backend (admin JWT) =====
    @GetMapping("/api/telemetry/status")
    public Map<String, Object> status() {
        return svc.status();
    }

    @GetMapping("/api/telemetry/players")
    public Map<String, Object> players() {
        List<Map<String, Object>> list = svc.listPlayers();
        return Map.of("players", list, "count", list.size(), "enabled", svc.isEnabled());
    }

    @GetMapping("/api/telemetry/player/{uuid}")
    public ResponseEntity<?> player(@PathVariable String uuid) {
        Map<String, Object> snap = svc.getPlayer(uuid);
        if (snap == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(snap);
    }

    @PutMapping("/api/telemetry/config")
    public Map<String, Object> updateConfig(@RequestBody Map<String, Object> body) {
        Object e = body.get("enabled");
        if (e instanceof Boolean b) svc.setEnabled(b);
        return Map.of("ok", true, "enabled", svc.isEnabled());
    }

    // ============================================================================
    // PÚBLICO (sem JWT, sem login) — auth via header X-Telemetry-Token
    // ============================================================================
    // Path /api/public/* já está whitelisted no AuthFilter, então não passa
    // pelo Bearer JWT. Validamos o token aqui dentro do controller.
    //
    // Uso típico (curl):
    //   curl -H "X-Telemetry-Token: $TELEMETRY_PUBLIC_TOKEN" \
    //     https://backend.astaroneremita.com/api/public/telemetry/players
    //
    // Headers aceitos:
    //   X-Telemetry-Token: <token>   ← preferido
    //   X-Public-Token:    <token>   ← alias
    //
    // Resposta: { "players": [snapshot...], "count": N, "ts": ms }
    // Cada snapshot tem o JSON exato:
    //   uuid, name, online, lastUpdate, session{...}, counters{...},
    //   features{...}, inference{...}, timeline{...}
    // ============================================================================

    private boolean checkPublicToken(HttpServletRequest req) {
        // Sem token configurado (env TELEMETRY_PUBLIC_TOKEN) = endpoint NEGADO
        // (fail-closed). Antes vazio liberava geral, o que vazava telemetria.
        if (publicToken == null || publicToken.isBlank()) return false;
        String got = req.getHeader("X-Telemetry-Token");
        if (got == null) got = req.getHeader("X-Public-Token");
        return publicToken.equals(got);
    }

    @GetMapping("/api/public/telemetry/players")
    public ResponseEntity<?> publicPlayers(HttpServletRequest req) {
        if (!checkPublicToken(req)) {
            return ResponseEntity.status(401).body(Map.of(
                    "error", "unauthorized",
                    "hint", "use header X-Telemetry-Token"
            ));
        }
        List<Map<String, Object>> list = svc.listPlayers();
        return ResponseEntity.ok(Map.of(
                "players", list,
                "count", list.size(),
                "enabled", svc.isEnabled(),
                "ts", System.currentTimeMillis()
        ));
    }

    @GetMapping("/api/public/telemetry/player/{uuid}")
    public ResponseEntity<?> publicPlayer(@PathVariable String uuid, HttpServletRequest req) {
        if (!checkPublicToken(req)) {
            return ResponseEntity.status(401).body(Map.of(
                    "error", "unauthorized",
                    "hint", "use header X-Telemetry-Token"
            ));
        }
        Map<String, Object> snap = svc.getPlayer(uuid);
        if (snap == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(snap);
    }

    @GetMapping("/api/public/telemetry/status")
    public ResponseEntity<?> publicStatus(HttpServletRequest req) {
        if (!checkPublicToken(req)) {
            return ResponseEntity.status(401).body(Map.of(
                    "error", "unauthorized",
                    "hint", "use header X-Telemetry-Token"
            ));
        }
        return ResponseEntity.ok(svc.status());
    }
}
