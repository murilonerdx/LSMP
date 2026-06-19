package br.com.murilo.liberthia.admin.tester;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Endpoints novos pra processo seletivo + servidor de teste + sugestões.
 *
 * PÚBLICO (sem auth):
 *  - POST /api/tester/apply  → criar inscrição
 *
 * TESTER (Bearer tester-token):
 *  - GET  /api/tester/server-info
 *  - GET  /api/tester/suggestions             (com seu voto incluído)
 *  - POST /api/tester/suggestions             (criar)
 *  - GET  /api/tester/suggestions/mine
 *  - POST /api/tester/suggestions/{id}/vote   body: {vote: 1|0|-1}
 *
 * ADMIN:
 *  - GET  /api/admin/tester/applications      (?status=PENDING|APPROVED|REJECTED)
 *  - POST /api/admin/tester/applications/{id}/{approve,reject}
 *  - GET  /api/admin/tester/server-info
 *  - PUT  /api/admin/tester/server-info
 *  - GET  /api/admin/tester/suggestions
 *  - POST /api/admin/tester/suggestions/{id}/status   body: {status, note}
 */
@RestController
public class TesterApplyController {

    private final TesterApplyService svc;
    private final ModTesterService testerSvc;

    public TesterApplyController(TesterApplyService svc, ModTesterService testerSvc) {
        this.svc = svc;
        this.testerSvc = testerSvc;
    }

    public record ApplyRequest(String realName, String mcName, String contact,
                               boolean weeklyAvailability, String motivation) {}

    // Feature flag em memória — habilita/desabilita /api/tester/apply em runtime.
    // Persistido via KV (key = "tester.applications.enabled") pra sobreviver
    // restart do backend. Carregado no startup via PostConstruct (vê o método
    // logo abaixo).
    private static volatile boolean applicationsEnabled = true;

    @org.springframework.beans.factory.annotation.Autowired
    private br.com.murilo.liberthia.admin.engine.kv.KvConfigRepository kvRepo;

    @jakarta.annotation.PostConstruct
    public void loadConfigFlag() {
        if (kvRepo == null) return;
        try {
            kvRepo.findById("tester.applications.enabled").ifPresent(kv -> {
                applicationsEnabled = !"false".equalsIgnoreCase(kv.getDataJson());
            });
        } catch (Exception ignored) {}
    }

    /** Público: frontend lê isso antes de mostrar o form de apply. */
    @GetMapping("/api/tester/config")
    public Map<String, Object> publicConfig() {
        return Map.of("applicationsEnabled", applicationsEnabled);
    }

    /** Admin: toggle on/off do form de inscrição. */
    @PostMapping("/api/admin/tester/config/applications-enabled")
    public Map<String, Object> setApplicationsEnabled(@RequestBody Map<String, Boolean> body) {
        Boolean v = body.get("enabled");
        if (v == null) return Map.of("ok", false, "error", "field 'enabled' required");
        applicationsEnabled = v;
        try {
            br.com.murilo.liberthia.admin.engine.kv.KvConfig kv = kvRepo
                    .findById("tester.applications.enabled")
                    .orElse(new br.com.murilo.liberthia.admin.engine.kv.KvConfig("tester.applications.enabled", "true"));
            kv.setDataJson(String.valueOf(v));
            kvRepo.save(kv);
        } catch (Exception ignored) {}
        return Map.of("ok", true, "applicationsEnabled", applicationsEnabled);
    }

    @PostMapping("/api/tester/apply")
    public ResponseEntity<?> apply(@RequestBody ApplyRequest body) {
        if (!applicationsEnabled) {
            return ResponseEntity.status(403).body(Map.of(
                    "ok", false,
                    "error", "Inscrições estão fechadas no momento. Avise o admin pra reabrir."
            ));
        }
        try {
            TesterApplication a = new TesterApplication();
            a.setRealName(body.realName() == null ? "" : body.realName().trim());
            a.setMcName(body.mcName() == null ? "" : body.mcName().trim());
            a.setContact(body.contact());
            a.setWeeklyAvailability(body.weeklyAvailability());
            a.setMotivation(body.motivation() == null ? "" : body.motivation().trim());
            TesterApplication saved = svc.apply(a);
            return ResponseEntity.ok(Map.of(
                    "ok", true,
                    "id", saved.getId(),
                    "status", saved.getStatus().name(),
                    "message", "Inscrição recebida! Aguarde a análise do admin."
            ));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "error", e.getMessage()));
        }
    }

    // ============ TESTER ============

    @GetMapping("/api/tester/server-info")
    public ResponseEntity<?> testerServerInfo(HttpServletRequest req) {
        String mc = requireTester(req); if (mc == null) return unauth();
        return ResponseEntity.ok(serverInfoDto(svc.getServerInfo()));
    }

    @GetMapping("/api/tester/suggestions")
    public ResponseEntity<?> testerListSuggestions(HttpServletRequest req,
                                                   @RequestParam(required = false) String status) {
        String mc = requireTester(req); if (mc == null) return unauth();
        TesterSuggestion.Status st = parseStatus(status);
        List<TesterSuggestion> list = svc.listSuggestions(st);
        List<Map<String, Object>> out = new ArrayList<>();
        for (TesterSuggestion s : list) out.add(suggestionDto(s, mc));
        return ResponseEntity.ok(Map.of("suggestions", out, "count", out.size()));
    }

    public record CreateSuggestionRequest(String type, String title, String description,
                                          String technicalDetails, String referenceUrl,
                                          // v96 — campos completos
                                          String suggestedItemId, String iconUrl,
                                          String recipeJson, String effectsJson) {}

    @PostMapping("/api/tester/suggestions")
    public ResponseEntity<?> testerCreateSuggestion(@RequestBody CreateSuggestionRequest body,
                                                    HttpServletRequest req) {
        String mc = requireTester(req); if (mc == null) return unauth();
        try {
            TesterSuggestion s = new TesterSuggestion();
            s.setAuthorMcName(mc);
            s.setType(parseType(body.type()));
            s.setTitle(body.title());
            s.setDescription(body.description());
            s.setTechnicalDetails(body.technicalDetails());
            s.setReferenceUrl(body.referenceUrl());
            s.setSuggestedItemId(body.suggestedItemId());
            s.setIconUrl(body.iconUrl());
            s.setRecipeJson(body.recipeJson());
            s.setEffectsJson(body.effectsJson());
            TesterSuggestion saved = svc.createSuggestion(s);
            return ResponseEntity.ok(suggestionDto(saved, mc));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/api/tester/suggestions/mine")
    public ResponseEntity<?> testerListMySuggestions(HttpServletRequest req) {
        String mc = requireTester(req); if (mc == null) return unauth();
        List<TesterSuggestion> list = svc.listSuggestionsByAuthor(mc);
        List<Map<String, Object>> out = new ArrayList<>();
        for (TesterSuggestion s : list) out.add(suggestionDto(s, mc));
        return ResponseEntity.ok(Map.of("suggestions", out, "count", out.size()));
    }

    public record VoteRequest(int vote) {}

    @PostMapping("/api/tester/suggestions/{id}/vote")
    public ResponseEntity<?> testerVote(@PathVariable Long id, @RequestBody VoteRequest body,
                                        HttpServletRequest req) {
        String mc = requireTester(req); if (mc == null) return unauth();
        try {
            TesterSuggestion s = svc.vote(id, mc, body.vote());
            return ResponseEntity.ok(suggestionDto(s, mc));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ============ ADMIN — APPLICATIONS ============

    @GetMapping("/api/admin/tester/applications")
    public Map<String, Object> adminListApplications(@RequestParam(required = false) String status) {
        TesterApplication.Status st = null;
        if (status != null) {
            try { st = TesterApplication.Status.valueOf(status.toUpperCase()); }
            catch (Exception ignored) {}
        }
        List<TesterApplication> list = svc.listApplications(st);
        List<Map<String, Object>> out = new ArrayList<>();
        for (TesterApplication a : list) out.add(applicationDto(a));
        return Map.of("applications", out, "count", out.size());
    }

    public record ReviewRequest(String note) {}

    @PostMapping("/api/admin/tester/applications/{id}/approve")
    public Map<String, Object> adminApproveApplication(@PathVariable Long id, @RequestBody(required = false) ReviewRequest body) {
        TesterApplication a = svc.approve(id, body == null ? null : body.note(), "admin");
        Map<String, Object> r = applicationDto(a);
        r.put("ok", true);
        return r;
    }

    @PostMapping("/api/admin/tester/applications/{id}/reject")
    public Map<String, Object> adminRejectApplication(@PathVariable Long id, @RequestBody(required = false) ReviewRequest body) {
        TesterApplication a = svc.reject(id, body == null ? null : body.note(), "admin");
        Map<String, Object> r = applicationDto(a);
        r.put("ok", true);
        return r;
    }

    // ============ ADMIN — SERVER INFO ============

    @GetMapping("/api/admin/tester/server-info")
    public Map<String, Object> adminGetServerInfo() {
        return serverInfoDto(svc.getServerInfo());
    }

    @PutMapping("/api/admin/tester/server-info")
    public Map<String, Object> adminUpdateServerInfo(@RequestBody TestServerInfo in) {
        return serverInfoDto(svc.updateServerInfo(in, "admin"));
    }

    // ============ ADMIN — SUGGESTIONS ============

    @GetMapping("/api/admin/tester/suggestions")
    public Map<String, Object> adminListSuggestions(@RequestParam(required = false) String status) {
        TesterSuggestion.Status st = parseStatus(status);
        List<TesterSuggestion> list = svc.listSuggestions(st);
        List<Map<String, Object>> out = new ArrayList<>();
        for (TesterSuggestion s : list) out.add(suggestionDto(s, null));
        return Map.of("suggestions", out, "count", out.size());
    }

    public record UpdateStatusRequest(String status, String note, String developmentNote) {}

    @PostMapping("/api/admin/tester/suggestions/{id}/status")
    public Map<String, Object> adminUpdateSuggestionStatus(@PathVariable Long id,
                                                           @RequestBody UpdateStatusRequest body) {
        TesterSuggestion.Status st;
        try { st = TesterSuggestion.Status.valueOf(body.status().toUpperCase()); }
        catch (Exception e) { return Map.of("error", "status inválido: " + body.status()); }
        TesterSuggestion s = svc.adminUpdateStatus(id, st, body.note(), "admin", body.developmentNote());
        return suggestionDto(s, null);
    }

    // ============ HELPERS ============

    private String requireTester(HttpServletRequest req) {
        String h = req.getHeader("Authorization");
        if (h == null || !h.startsWith("Bearer ")) return null;
        return testerSvc.validateToken(h.substring(7));
    }

    private ResponseEntity<?> unauth() {
        return ResponseEntity.status(401).body(Map.of("error", "token tester inválido"));
    }

    private TesterSuggestion.Status parseStatus(String s) {
        if (s == null || s.isBlank()) return null;
        try { return TesterSuggestion.Status.valueOf(s.toUpperCase()); }
        catch (Exception e) { return null; }
    }

    private TesterSuggestion.Type parseType(String s) {
        if (s == null || s.isBlank()) return TesterSuggestion.Type.OTHER;
        try { return TesterSuggestion.Type.valueOf(s.toUpperCase()); }
        catch (Exception e) { return TesterSuggestion.Type.OTHER; }
    }

    private Map<String, Object> applicationDto(TesterApplication a) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", a.getId());
        m.put("realName", a.getRealName());
        m.put("mcName", a.getMcName());
        m.put("contact", a.getContact());
        m.put("weeklyAvailability", a.isWeeklyAvailability());
        m.put("motivation", a.getMotivation());
        m.put("status", a.getStatus().name());
        m.put("adminNote", a.getAdminNote());
        m.put("reviewedBy", a.getReviewedBy());
        m.put("reviewedAt", a.getReviewedAt() == null ? null : a.getReviewedAt().toString());
        m.put("generatedInviteCode", a.getGeneratedInviteCode());
        m.put("createdAt", a.getCreatedAt() == null ? null : a.getCreatedAt().toString());
        return m;
    }

    private Map<String, Object> serverInfoDto(TestServerInfo s) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("serverAddress", s.getServerAddress());
        m.put("mcVersion", s.getMcVersion());
        m.put("modVersion", s.getModVersion());
        m.put("connectionNotes", s.getConnectionNotes());
        m.put("ndaText", s.getNdaText());
        m.put("discordLink", s.getDiscordLink());
        m.put("voiceChatInfo", s.getVoiceChatInfo());
        m.put("active", s.isActive());
        m.put("updatedBy", s.getUpdatedBy());
        m.put("updatedAt", s.getUpdatedAt() == null ? null : s.getUpdatedAt().toString());
        return m;
    }

    private Map<String, Object> suggestionDto(TesterSuggestion s, String currentTester) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", s.getId());
        m.put("authorMcName", s.getAuthorMcName());
        m.put("type", s.getType().name());
        m.put("title", s.getTitle());
        m.put("description", s.getDescription());
        m.put("technicalDetails", s.getTechnicalDetails());
        m.put("referenceUrl", s.getReferenceUrl());
        m.put("status", s.getStatus().name());
        m.put("upvotes", s.getUpvotes());
        m.put("downvotes", s.getDownvotes());
        m.put("score", s.getUpvotes() - s.getDownvotes());
        m.put("adminNote", s.getAdminNote());
        m.put("triagedBy", s.getTriagedBy());
        m.put("triagedAt", s.getTriagedAt() == null ? null : s.getTriagedAt().toString());
        m.put("createdAt", s.getCreatedAt() == null ? null : s.getCreatedAt().toString());
        if (currentTester != null) m.put("myVote", svc.getUserVote(s, currentTester));
        return m;
    }
}
