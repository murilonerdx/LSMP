package br.com.murilo.liberthia.admin.tester;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Endpoints REST das extensões:
 *   /api/tester/splashes         (tester cria/vota)
 *   /api/admin/tester/splashes   (admin lista/triagga)
 *   /api/tester/balance          (tester cria/lista)
 *   /api/admin/tester/balance    (admin lista/triagga)
 *   /api/wiki                    (público — lista entries publicadas)
 *   /api/admin/wiki              (admin escreve)
 *   /api/tester/notifications    (inbox do tester logado)
 *   /api/admin/tester/auto-create (admin gera conta com claim link)
 *   /api/tester/auth/claim       (tester finaliza claim com senha)
 */
@RestController
public class TesterExtensionsController {

    private final TesterExtensionsService svc;
    private final ModTesterService testerSvc;

    public TesterExtensionsController(TesterExtensionsService svc, ModTesterService testerSvc) {
        this.svc = svc;
        this.testerSvc = testerSvc;
    }

    // ============================================================
    // SPLASH SUGGESTIONS
    // ============================================================

    public record CreateSplashReq(String text, String colorHex, String category) {}

    @PostMapping("/api/tester/splashes")
    public ResponseEntity<?> createSplash(HttpServletRequest req, @RequestBody CreateSplashReq body) {
        String mc = extractMc(req);
        if (mc == null) return ResponseEntity.status(401).body(Map.of("error", "no auth"));
        try {
            SplashSuggestion.Category cat = body.category() == null ? null
                    : SplashSuggestion.Category.valueOf(body.category().toUpperCase());
            SplashSuggestion s = svc.createSplash(mc, body.text(), body.colorHex(), cat);
            return ResponseEntity.ok(splashDto(s, mc));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/api/tester/splashes")
    public Map<String, Object> listSplashes(HttpServletRequest req,
                                             @RequestParam(required = false) String status) {
        String mc = extractMc(req);
        SplashSuggestion.Status filter = null;
        if (status != null && !status.isBlank() && !status.equalsIgnoreCase("all")) {
            try { filter = SplashSuggestion.Status.valueOf(status.toUpperCase()); }
            catch (Exception ignored) {}
        }
        List<Map<String, Object>> out = new ArrayList<>();
        for (SplashSuggestion s : svc.listSplashes(filter)) out.add(splashDto(s, mc));
        return Map.of("splashes", out);
    }

    @PostMapping("/api/tester/splashes/{id}/vote")
    public ResponseEntity<?> voteSplash(HttpServletRequest req, @PathVariable Long id,
                                        @RequestBody Map<String, Object> body) {
        String mc = extractMc(req);
        if (mc == null) return ResponseEntity.status(401).body(Map.of("error", "no auth"));
        int v = body.get("vote") instanceof Number n ? n.intValue() : 0;
        try {
            SplashSuggestion s = svc.voteSplash(id, mc, v);
            return ResponseEntity.ok(splashDto(s, mc));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    public record TriageSplashReq(String status, String adminNote) {}

    @PostMapping("/api/admin/tester/splashes/{id}/triage")
    public ResponseEntity<?> triageSplash(@PathVariable Long id, @RequestBody TriageSplashReq body) {
        try {
            SplashSuggestion.Status st = SplashSuggestion.Status.valueOf(body.status().toUpperCase());
            SplashSuggestion s = svc.triageSplash(id, st, body.adminNote(), "admin");
            return ResponseEntity.ok(splashDto(s, null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/api/admin/tester/splashes/{id}")
    public Map<String, Object> deleteSplash(@PathVariable Long id) {
        svc.deleteSplash(id);
        return Map.of("ok", true);
    }

    /** Endpoint público do mod — lista splashes ativos pra mostrar in-game. */
    @GetMapping("/api/public/splashes")
    public Map<String, Object> publicSplashes() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (SplashSuggestion s : svc.activeSplashes()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("text", s.getText());
            m.put("colorHex", s.getColorHex());
            m.put("weight", s.getWeight());
            out.add(m);
        }
        return Map.of("splashes", out);
    }

    // ============================================================
    // BALANCE REQUESTS (buff/nerf)
    // ============================================================

    public record CreateBalanceReq(String type, String itemId, String itemDisplayName,
                                    String title, String description,
                                    String currentBehavior, String proposedBehavior,
                                    String evidenceUrl, String testContext) {}

    @PostMapping("/api/tester/balance")
    public ResponseEntity<?> createBalance(HttpServletRequest req, @RequestBody CreateBalanceReq body) {
        String mc = extractMc(req);
        if (mc == null) return ResponseEntity.status(401).body(Map.of("error", "no auth"));
        try {
            BalanceRequest b = new BalanceRequest();
            if (body.type() != null) b.setType(BalanceRequest.Type.valueOf(body.type().toUpperCase()));
            b.setItemId(body.itemId());
            b.setItemDisplayName(body.itemDisplayName());
            b.setTitle(body.title());
            b.setDescription(body.description());
            b.setCurrentBehavior(body.currentBehavior());
            b.setProposedBehavior(body.proposedBehavior());
            b.setEvidenceUrl(body.evidenceUrl());
            b.setTestContext(body.testContext());
            return ResponseEntity.ok(balanceDto(svc.createBalance(mc, b)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** Tester lista os PRÓPRIOS pedidos. */
    @GetMapping("/api/tester/balance/mine")
    public Map<String, Object> myBalances(HttpServletRequest req) {
        String mc = extractMc(req);
        if (mc == null) return Map.of("requests", List.of());
        List<Map<String, Object>> out = new ArrayList<>();
        for (BalanceRequest b : svc.listBalances(null, mc)) out.add(balanceDto(b));
        return Map.of("requests", out);
    }

    /** Lista todos (tester e admin) com filtros opcionais — útil pro roadmap. */
    @GetMapping("/api/tester/balance")
    public Map<String, Object> listBalances(@RequestParam(required = false) String status) {
        BalanceRequest.Status filter = null;
        if (status != null && !status.isBlank() && !status.equalsIgnoreCase("all")) {
            try { filter = BalanceRequest.Status.valueOf(status.toUpperCase()); }
            catch (Exception ignored) {}
        }
        List<Map<String, Object>> out = new ArrayList<>();
        for (BalanceRequest b : svc.listBalances(filter, null)) out.add(balanceDto(b));
        return Map.of("requests", out);
    }

    public record TriageBalanceReq(String status, String adminNote, Integer awardPoints) {}

    @PostMapping("/api/admin/tester/balance/{id}/triage")
    public ResponseEntity<?> triageBalance(@PathVariable Long id, @RequestBody TriageBalanceReq body) {
        try {
            BalanceRequest.Status st = BalanceRequest.Status.valueOf(body.status().toUpperCase());
            BalanceRequest b = svc.triageBalance(id, st, body.adminNote(), "admin", body.awardPoints());
            // Award pts pro tester (se houver)
            if (body.awardPoints() != null && body.awardPoints() > 0) {
                testerSvc.addPoints(b.getTesterMcName(), body.awardPoints());
            }
            return ResponseEntity.ok(balanceDto(b));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/api/admin/tester/balance/{id}")
    public Map<String, Object> deleteBalance(@PathVariable Long id) {
        svc.deleteBalance(id);
        return Map.of("ok", true);
    }

    // ============================================================
    // WIKI
    // ============================================================

    // NOTA: /api/wiki já é usado pelo WikiController existente (engine.wiki).
    // Usamos /api/feature-wiki aqui pra não conflitar — feature-wiki é a
    // tabela `feature_wiki_entries` (docs por item/bloco do mod do tester).
    @GetMapping("/api/feature-wiki")
    public Map<String, Object> listWiki(@RequestParam(required = false) String category) {
        FeatureWikiEntry.Category cat = null;
        if (category != null && !category.isBlank() && !category.equalsIgnoreCase("all")) {
            try { cat = FeatureWikiEntry.Category.valueOf(category.toUpperCase()); }
            catch (Exception ignored) {}
        }
        List<Map<String, Object>> out = new ArrayList<>();
        for (FeatureWikiEntry e : svc.listWiki(true, cat)) out.add(wikiDto(e, false));
        return Map.of("entries", out);
    }

    @GetMapping("/api/feature-wiki/{slug}")
    public ResponseEntity<?> getWiki(@PathVariable String slug) {
        return svc.getWikiBySlug(slug)
                .map(e -> ResponseEntity.ok((Object) wikiDto(e, true)))
                .orElse(ResponseEntity.status(404).body(Map.of("error", "not found")));
    }

    @GetMapping("/api/admin/feature-wiki")
    public Map<String, Object> adminListWiki() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (FeatureWikiEntry e : svc.listWiki(false, null)) out.add(wikiDto(e, false));
        return Map.of("entries", out);
    }

    @PostMapping("/api/admin/feature-wiki")
    public ResponseEntity<?> saveWiki(@RequestBody FeatureWikiEntry body) {
        try {
            return ResponseEntity.ok(wikiDto(svc.saveWiki(body, "admin"), true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/api/admin/feature-wiki/{id}")
    public Map<String, Object> deleteWiki(@PathVariable Long id) {
        svc.deleteWiki(id);
        return Map.of("ok", true);
    }

    // ============================================================
    // NOTIFICATIONS
    // ============================================================

    @GetMapping("/api/tester/notifications")
    public ResponseEntity<?> myNotifications(HttpServletRequest req) {
        String mc = extractMc(req);
        if (mc == null) return ResponseEntity.status(401).body(Map.of("error", "no auth"));
        List<Map<String, Object>> out = new ArrayList<>();
        for (TesterNotification n : svc.listNotifications(mc)) out.add(notifDto(n));
        return ResponseEntity.ok(Map.of(
                "notifications", out,
                "unread", svc.unreadCount(mc)
        ));
    }

    @PatchMapping("/api/tester/notifications/{id}/read")
    public ResponseEntity<?> markRead(HttpServletRequest req, @PathVariable Long id) {
        String mc = extractMc(req);
        if (mc == null) return ResponseEntity.status(401).body(Map.of("error", "no auth"));
        svc.markRead(id, mc);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @PostMapping("/api/tester/notifications/read-all")
    public ResponseEntity<?> markAllRead(HttpServletRequest req) {
        String mc = extractMc(req);
        if (mc == null) return ResponseEntity.status(401).body(Map.of("error", "no auth"));
        svc.markAllRead(mc);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @DeleteMapping("/api/tester/notifications/{id}")
    public ResponseEntity<?> deleteNotif(HttpServletRequest req, @PathVariable Long id) {
        String mc = extractMc(req);
        if (mc == null) return ResponseEntity.status(401).body(Map.of("error", "no auth"));
        svc.deleteNotification(id, mc);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    // ============================================================
    // AUTO-CREATE TESTER ACCOUNT
    // ============================================================

    public record AutoCreateReq(String mcName, String note) {}

    @PostMapping("/api/admin/tester/auto-create")
    public ResponseEntity<?> autoCreate(@RequestBody AutoCreateReq body) {
        try {
            ModTesterService.AutoCreateResult res = testerSvc.autoCreate(body.mcName(), "admin", body.note());
            String origin = "https://lsmp.astaroneremita.com"; // pode vir de config
            String claimUrl = origin + "/tester/claim?code=" + res.claimCode();
            return ResponseEntity.ok(Map.of(
                    "ok", true,
                    "mcName", res.tester().getMcName(),
                    "claimCode", res.claimCode(),
                    "claimUrl", claimUrl,
                    "note", "Envia esse link pro tester. Ele só precisa definir senha."
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "error", e.getMessage()));
        }
    }

    public record ClaimReq(String code, String password) {}

    @PostMapping("/api/tester/auth/claim")
    public ResponseEntity<?> claim(@RequestBody ClaimReq body) {
        try {
            ModTester t = testerSvc.claimAccount(body.code(), body.password());
            String token = testerSvc.issueToken(t.getMcName());
            // Map.of NÃO ACEITA value null — se algum getter retornar null
            // (createdAt antes do @PrePersist disparar, etc), o endpoint explode
            // com NPE. Usar LinkedHashMap pra ser tolerante.
            Map<String, Object> testerDto = new LinkedHashMap<>();
            testerDto.put("id", t.getId());
            testerDto.put("mcName", t.getMcName());
            testerDto.put("points", t.getPoints());
            testerDto.put("createdAt", t.getCreatedAt() == null ? null : t.getCreatedAt().toString());
            Map<String, Object> resp = new LinkedHashMap<>();
            resp.put("ok", true);
            resp.put("token", token);
            resp.put("tester", testerDto);
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "error",
                    e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage()));
        }
    }

    // ============================================================
    // DTO HELPERS
    // ============================================================

    private Map<String, Object> splashDto(SplashSuggestion s, String myMc) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", s.getId());
        m.put("authorMcName", s.getAuthorMcName());
        m.put("text", s.getText());
        m.put("colorHex", s.getColorHex());
        m.put("category", s.getCategory().name());
        m.put("status", s.getStatus().name());
        m.put("weight", s.getWeight());
        m.put("upvotes", s.getUpvotes());
        m.put("downvotes", s.getDownvotes());
        m.put("score", s.getUpvotes() - s.getDownvotes());
        m.put("adminNote", s.getAdminNote());
        m.put("createdAt", s.getCreatedAt());
        if (myMc != null) {
            Map<String, Integer> votes = TesterExtensionsService.parseVotes(s.getVotesJson());
            m.put("myVote", votes.getOrDefault(myMc, 0));
        }
        return m;
    }

    private Map<String, Object> balanceDto(BalanceRequest b) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", b.getId());
        m.put("testerMcName", b.getTesterMcName());
        m.put("type", b.getType().name());
        m.put("itemId", b.getItemId());
        m.put("itemDisplayName", b.getItemDisplayName());
        m.put("title", b.getTitle());
        m.put("description", b.getDescription());
        m.put("currentBehavior", b.getCurrentBehavior());
        m.put("proposedBehavior", b.getProposedBehavior());
        m.put("evidenceUrl", b.getEvidenceUrl());
        m.put("testContext", b.getTestContext());
        m.put("status", b.getStatus().name());
        m.put("adminNote", b.getAdminNote());
        m.put("pointsAwarded", b.getPointsAwarded());
        m.put("createdAt", b.getCreatedAt());
        m.put("triagedAt", b.getTriagedAt());
        return m;
    }

    private Map<String, Object> wikiDto(FeatureWikiEntry e, boolean full) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", e.getId());
        m.put("slug", e.getSlug());
        m.put("category", e.getCategory().name());
        m.put("title", e.getTitle());
        m.put("itemId", e.getItemId());
        m.put("imageUrl", e.getImageUrl());
        m.put("summary", e.getSummary());
        m.put("addedInVersion", e.getAddedInVersion());
        m.put("tags", e.getTags());
        m.put("published", e.isPublished());
        m.put("createdAt", e.getCreatedAt());
        m.put("updatedAt", e.getUpdatedAt());
        if (full) {
            m.put("contentMd", e.getContentMd());
            m.put("creditsJson", e.getCreditsJson());
            m.put("recipeJson", e.getRecipeJson());
        }
        return m;
    }

    private Map<String, Object> notifDto(TesterNotification n) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", n.getId());
        m.put("type", n.getType().name());
        m.put("title", n.getTitle());
        m.put("body", n.getBody());
        m.put("link", n.getLink());
        m.put("isRead", n.isRead());
        m.put("createdAt", n.getCreatedAt());
        return m;
    }

    // ============================================================
    // AUTH HELPER (extrai mcName do JWT do tester)
    // ============================================================

    private String extractMc(HttpServletRequest req) {
        String auth = req.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) return null;
        return testerSvc.validateToken(auth.substring(7));
    }
}
