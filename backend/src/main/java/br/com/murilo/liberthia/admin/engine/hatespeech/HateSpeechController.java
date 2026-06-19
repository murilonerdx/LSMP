package br.com.murilo.liberthia.admin.engine.hatespeech;

import br.com.murilo.liberthia.admin.mod.ModBridgeClient;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;

/**
 * Endpoints admin pra gerenciar alertas de hate speech.
 *
 * <ul>
 *   <li>GET  /api/admin/hate-speech/alerts        — lista (filtros opcionais)</li>
 *   <li>GET  /api/admin/hate-speech/stats         — contagens por severity/player</li>
 *   <li>POST /api/admin/hate-speech/alerts/{id}/review — marca como reviewed + ação</li>
 *   <li>GET  /api/admin/hate-speech/test-scan?text=... — testa quick scan (debug)</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/admin/hate-speech")
public class HateSpeechController {

    private final HateSpeechAlertRepository repo;
    private final HateSpeechDetectorService detector;
    private final ModBridgeClient mod;

    public HateSpeechController(HateSpeechAlertRepository repo,
                                 HateSpeechDetectorService detector,
                                 ModBridgeClient mod) {
        this.repo = repo;
        this.detector = detector;
        this.mod = mod;
    }

    @GetMapping("/alerts")
    public List<HateSpeechAlert> list(
            @RequestParam(required = false) Boolean reviewed,
            @RequestParam(required = false) String playerUuid,
            @RequestParam(required = false) String severity,
            @RequestParam(defaultValue = "100") int limit) {
        return repo.search(reviewed, playerUuid, severity,
                PageRequest.of(0, Math.min(500, Math.max(1, limit))));
    }

    @GetMapping("/stats")
    public Map<String, Object> stats() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("total", repo.count());
        m.put("pendingReview", repo.countByReviewedFalse());

        Map<String, Long> bySeverity = new LinkedHashMap<>();
        for (Object[] row : repo.countBySeverity()) {
            bySeverity.put((String) row[0], ((Number) row[1]).longValue());
        }
        m.put("bySeverity", bySeverity);

        List<Map<String, Object>> topPlayers = new ArrayList<>();
        for (Object[] row : repo.rankByPlayer(PageRequest.of(0, 10))) {
            Map<String, Object> p = new LinkedHashMap<>();
            p.put("uuid", row[0]);
            p.put("name", row[1]);
            p.put("count", row[2]);
            topPlayers.add(p);
        }
        m.put("topPlayers", topPlayers);
        return m;
    }

    /**
     * Marca alerta como reviewed + opcionalmente executa ação no servidor MC.
     *
     * <p>Body: <code>{"action":"IGNORE|WARN|MUTE|KICK|BAN","by":"admin","reason":"..."}</code>
     *
     * <p>Ações que disparam comando no mod:
     * <ul>
     *   <li>WARN — manda mensagem privada pro player</li>
     *   <li>MUTE — `/mute &lt;player&gt; 1h` (depende de plugin)</li>
     *   <li>KICK — `/kick &lt;player&gt; &lt;reason&gt;`</li>
     *   <li>BAN — `/ban &lt;player&gt; &lt;reason&gt;`</li>
     *   <li>IGNORE — só marca como reviewed sem comando</li>
     * </ul>
     */
    @PostMapping("/alerts/{id}/review")
    public Map<String, Object> review(@PathVariable Long id,
                                       @RequestBody Map<String, Object> body) {
        HateSpeechAlert a = repo.findById(id).orElseThrow(
                () -> new IllegalArgumentException("alert not found: " + id));
        String action = String.valueOf(body.getOrDefault("action", "IGNORE")).toUpperCase(Locale.ROOT);
        String by = String.valueOf(body.getOrDefault("by", "admin"));
        String reason = String.valueOf(body.getOrDefault("reason", a.getReason() == null ? "discurso de ódio" : a.getReason()));

        a.setReviewed(true);
        a.setActionTaken(action);
        a.setReviewedBy(by);
        a.setReviewedAt(Instant.now());

        // Executa comando no servidor MC se aplicável
        String modResult = null;
        try {
            switch (action) {
                case "WARN" -> modResult = runCommand("tellraw " + a.getPlayerName()
                        + " {\"text\":\"⚠ Aviso: " + escape(reason) + "\",\"color\":\"red\"}");
                case "MUTE" -> modResult = runCommand("mute " + a.getPlayerName() + " 1h " + reason);
                case "KICK" -> modResult = runCommand("kick " + a.getPlayerName() + " " + reason);
                case "BAN"  -> modResult = runCommand("ban " + a.getPlayerName() + " " + reason);
                case "IGNORE" -> modResult = "no-op";
                default -> modResult = "unknown action: " + action;
            }
        } catch (Exception e) {
            modResult = "ERRO ao executar: " + e.getMessage();
        }

        repo.save(a);
        return Map.of("ok", true, "alert", a, "modResult", modResult);
    }

    private String runCommand(String cmd) {
        try {
            mod.runCommand(cmd);
            return "executado: /" + cmd;
        } catch (Exception e) {
            return "falhou: " + e.getMessage();
        }
    }

    private static String escape(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    /**
     * Endpoint de debug — testa o quick scan numa string arbitrária.
     * Útil pra calibrar a lista de triggers sem precisar gerar transcrição.
     */
    @GetMapping("/test-scan")
    public Map<String, Object> testScan(@RequestParam String text) {
        HateSpeechDetectorService.QuickScanResult r = detector.quickScan(text);
        return Map.of(
                "text", text,
                "matchedTriggers", r.matchedTriggers,
                "categories", r.categories,
                "wouldAnalyze", !r.matchedTriggers.isEmpty()
        );
    }
}
