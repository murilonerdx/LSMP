package br.com.murilo.liberthia.admin.tester;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Endpoints consolidados de MVP-2~5:
 *
 *  TESTER (Bearer tester-token):
 *   - GET  /api/tester/packages           lista pacotes habilitados
 *   - GET  /api/tester/packages/{id}/dl   download do ZIP
 *   - GET  /api/tester/beta-items         lista items beta habilitados
 *   - POST /api/tester/bugs               criar bug report
 *   - GET  /api/tester/bugs/mine          meus bug reports
 *   - GET  /api/tester/rewards            lista recompensas
 *   - POST /api/tester/rewards/{id}/redeem  resgatar
 *   - GET  /api/tester/redemptions/mine   meus resgates
 *
 *  ADMIN (AuthFilter já valida via Bearer admin):
 *   - POST /api/admin/tester/packages     upload multipart
 *   - GET  /api/admin/tester/packages
 *   - DELETE /api/admin/tester/packages/{id}
 *   - POST /api/admin/tester/packages/{id}/toggle
 *   - GET  /api/admin/tester/packages/{id}/dl  (admin pode baixar também)
 *
 *   - GET/POST/PUT/DELETE /api/admin/tester/beta-items
 *
 *   - GET  /api/admin/tester/bugs                   lista todos
 *   - GET  /api/admin/tester/bugs/pending           só pendentes
 *   - POST /api/admin/tester/bugs/{id}/confirm
 *   - POST /api/admin/tester/bugs/{id}/reject
 *
 *   - GET/POST/PUT/DELETE /api/admin/tester/rewards
 *   - GET  /api/admin/tester/redemptions
 *   - POST /api/admin/tester/redemptions/{id}/deliver
 */
@RestController
public class TesterContentController {

    private final TesterContentService svc;
    private final ModTesterService testerSvc;

    public TesterContentController(TesterContentService svc, ModTesterService testerSvc) {
        this.svc = svc;
        this.testerSvc = testerSvc;
    }

    // ============================================================ //
    // TESTER ENDPOINTS
    // ============================================================ //

    @GetMapping("/api/tester/packages")
    public ResponseEntity<?> testerListPackages(HttpServletRequest req) {
        String mc = requireTester(req); if (mc == null) return unauth();
        List<Map<String, Object>> out = new ArrayList<>();
        // v0.1.27: FILTRA packages com arquivo perdido pro tester. Antes,
        // mostrava com badge "⚠ Arquivo perdido" feio — confundia o usuário,
        // que via 10 packages "perdidos" e achava que o sistema tava bugado.
        // Realidade: aqueles ZIPs sumiram nos redeploys ANTERIORES ao fix dos
        // volumes persistentes. Pro tester, melhor sumir com eles. O admin
        // continua vendo todos no painel admin (com flag fileExists) pra
        // decidir limpar/re-uploadar.
        for (ModPackage p : svc.listPackages(true)) {
            if (!svc.packageFileExists(p)) continue; // skip fantasmas
            out.add(pkgDto(p));
        }
        return ResponseEntity.ok(Map.of("packages", out, "count", out.size()));
    }

    @GetMapping("/api/tester/packages/{id}/dl")
    public ResponseEntity<?> testerDownloadPackage(@PathVariable Long id, HttpServletRequest req) throws IOException {
        String mc = requireTester(req); if (mc == null) return unauth();
        return downloadPackageResponse(id);
    }

    @GetMapping("/api/tester/beta-items")
    public ResponseEntity<?> testerListBetaItems(HttpServletRequest req) {
        String mc = requireTester(req); if (mc == null) return unauth();
        List<BetaItem> items = svc.listBetaItems(true);
        // Diagnóstico: contagem de items habilitados vs total no DB. Quando o
        // tester reportar "tá vazio mas a gente cadastrou", admin abre
        // /api/admin/tester/beta-items pra ver tudo e bate as contagens.
        int totalInDb = svc.listBetaItems(false).size();
        if (items.isEmpty() && totalInDb > 0) {
            // Aconteceu na v105: items no DB mas todos enabled=false. Loga pro
            // admin diagnosticar via tail -f. Sem isso o sintoma era silencioso.
            org.slf4j.LoggerFactory.getLogger(TesterContentController.class)
                    .warn("[BetaItems] tester={} viu lista vazia mas DB tem {} items (todos disabled?)",
                          mc, totalInDb);
        }
        List<Long> ids = items.stream().map(BetaItem::getId).toList();
        Map<Long, Map<String, Object>> votes = svc.bulkVoteCounts(BetaVote.Target.BETA_ITEM, ids, mc);
        List<Map<String, Object>> out = new ArrayList<>();
        for (BetaItem i : items) {
            Map<String, Object> dto = betaItemDto(i);
            // BUG histórico: Map.of() NÃO ACEITA null. Quando ninguém tinha votado
            // ainda, votes.getOrDefault retornava Map.of(..., "myVote", null) → NPE.
            // Fix: usar LinkedHashMap que aceita null pra myVote.
            Map<String, Object> fallback = new LinkedHashMap<>();
            fallback.put("likes", 0);
            fallback.put("dislikes", 0);
            fallback.put("myVote", null);
            dto.put("votes", votes.getOrDefault(i.getId(), fallback));
            out.add(dto);
        }
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("items", out);
        resp.put("count", out.size());
        // Hint pro frontend: se totalInDb > count, tem items desabilitados.
        // Frontend pode mostrar "X items aguardando ativação do admin".
        resp.put("totalInDb", totalInDb);
        resp.put("disabledCount", Math.max(0, totalInDb - out.size()));
        return ResponseEntity.ok(resp);
    }

    public record CreateBugRequest(Long betaItemId, String title, String description,
                                   String stepsToReproduce, String severity,
                                   // novos campos (v90+) — identifica o item, contexto, evidência
                                   String itemId, String howFound, String modVersion,
                                   String mcVersion, String worldContext, String screenshotUrl,
                                   // v96+: triage detalhada
                                   String frequency, String priority,
                                   Boolean canReplicate, Boolean affectsOthers,
                                   String expectedBehavior, String workaround, String tags) {}

    @PostMapping("/api/tester/bugs")
    public ResponseEntity<?> testerCreateBug(@RequestBody CreateBugRequest body, HttpServletRequest req) {
        String mc = requireTester(req); if (mc == null) return unauth();
        if (body.title() == null || body.title().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "título obrigatório"));
        }
        if (body.description() == null || body.description().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "descrição obrigatória"));
        }
        BugReport b = new BugReport();
        b.setTesterMcName(mc);
        b.setBetaItemId(body.betaItemId());
        b.setTitle(body.title().trim());
        b.setDescription(body.description().trim());
        b.setStepsToReproduce(body.stepsToReproduce());
        b.setSeverity(body.severity());
        // novos campos
        b.setItemId(body.itemId());
        b.setHowFound(body.howFound());
        b.setModVersion(body.modVersion());
        b.setMcVersion(body.mcVersion());
        b.setWorldContext(body.worldContext());
        b.setScreenshotUrl(body.screenshotUrl());
        // v96 — triage detalhada
        b.setFrequency(body.frequency());
        b.setPriority(body.priority());
        if (body.canReplicate() != null) b.setCanReplicate(body.canReplicate());
        if (body.affectsOthers() != null) b.setAffectsOthers(body.affectsOthers());
        b.setExpectedBehavior(body.expectedBehavior());
        b.setWorkaround(body.workaround());
        b.setTags(body.tags());
        BugReport saved = svc.createBug(b);
        return ResponseEntity.ok(bugDto(saved));
    }

    /** Outros testers podem CONFIRMAR que conseguiram replicar o bug.
     *  Ajuda admin a priorizar bugs com mais confirmações. */
    @PostMapping("/api/tester/bugs/{id}/confirm-replication")
    public ResponseEntity<?> confirmReplication(@PathVariable Long id, HttpServletRequest req) {
        String mc = requireTester(req); if (mc == null) return unauth();
        BugReport b = svc.confirmBugReplication(id, mc);
        if (b == null) return ResponseEntity.status(404).body(Map.of("error", "bug não existe"));
        return ResponseEntity.ok(bugDto(b));
    }

    /**
     * Tester edita o próprio bug. Só funciona se o bug ainda está PENDING
     * (depois de admin triar, vira read-only — preserva integridade da nota
     * de admin + pontos). Body é o mesmo formato do CreateBugRequest, todos
     * campos opcionais (NULL = mantém o atual). Service valida ownership.
     */
    @PatchMapping("/api/tester/bugs/{id}")
    public ResponseEntity<?> testerUpdateBug(@PathVariable Long id,
                                              @RequestBody CreateBugRequest body,
                                              HttpServletRequest req) {
        String mc = requireTester(req); if (mc == null) return unauth();
        try {
            BugReport patch = new BugReport();
            patch.setTitle(body.title());
            patch.setDescription(body.description());
            patch.setStepsToReproduce(body.stepsToReproduce());
            patch.setSeverity(body.severity());
            patch.setItemId(body.itemId());
            patch.setHowFound(body.howFound());
            patch.setModVersion(body.modVersion());
            patch.setMcVersion(body.mcVersion());
            patch.setWorldContext(body.worldContext());
            patch.setScreenshotUrl(body.screenshotUrl());
            patch.setFrequency(body.frequency());
            patch.setPriority(body.priority());
            if (body.canReplicate() != null) patch.setCanReplicate(body.canReplicate());
            if (body.affectsOthers() != null) patch.setAffectsOthers(body.affectsOthers());
            patch.setExpectedBehavior(body.expectedBehavior());
            patch.setWorkaround(body.workaround());
            patch.setTags(body.tags());
            BugReport saved = svc.updateBugFromTester(id, mc, patch);
            return ResponseEntity.ok(bugDto(saved));
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** Tester deleta o próprio bug (só se PENDING). */
    @DeleteMapping("/api/tester/bugs/{id}")
    public ResponseEntity<?> testerDeleteBug(@PathVariable Long id, HttpServletRequest req) {
        String mc = requireTester(req); if (mc == null) return unauth();
        try {
            svc.deleteBugFromTester(id, mc);
            return ResponseEntity.ok(Map.of("ok", true));
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** Lista bugs públicos (não confidenciais) pra outros testers verem e
     *  confirmarem replicação. Anonimiza dados sensíveis. */
    @GetMapping("/api/tester/bugs/community")
    public ResponseEntity<?> communityBugs(HttpServletRequest req,
                                            @RequestParam(defaultValue = "20") int limit) {
        String mc = requireTester(req); if (mc == null) return unauth();
        List<BugReport> bugs = svc.listAllBugs();
        // Apenas pendentes ou confirmados, limita a `limit` mais recentes
        bugs = bugs.stream()
                .filter(b -> b.getStatus() == BugReport.Status.PENDING || b.getStatus() == BugReport.Status.CONFIRMED)
                .limit(limit)
                .toList();
        List<Map<String, Object>> out = new ArrayList<>();
        for (BugReport b : bugs) {
            Map<String, Object> dto = bugDto(b);
            // Inclui confirmações pra UI mostrar "👍 X testers viram isso"
            out.add(dto);
        }
        return ResponseEntity.ok(Map.of("bugs", out, "count", out.size()));
    }

    @GetMapping("/api/tester/bugs/mine")
    public ResponseEntity<?> testerListMyBugs(HttpServletRequest req) {
        String mc = requireTester(req); if (mc == null) return unauth();
        List<Map<String, Object>> out = new ArrayList<>();
        for (BugReport b : svc.listBugsForTester(mc)) out.add(bugDto(b));
        return ResponseEntity.ok(Map.of("bugs", out, "count", out.size()));
    }

    @GetMapping("/api/tester/rewards")
    public ResponseEntity<?> testerListRewards(HttpServletRequest req) {
        String mc = requireTester(req); if (mc == null) return unauth();
        List<Map<String, Object>> out = new ArrayList<>();
        for (Reward r : svc.listRewards(true)) out.add(rewardDto(r));
        return ResponseEntity.ok(Map.of("rewards", out, "count", out.size()));
    }

    @PostMapping("/api/tester/rewards/{id}/redeem")
    public ResponseEntity<?> testerRedeem(@PathVariable Long id, HttpServletRequest req) {
        String mc = requireTester(req); if (mc == null) return unauth();
        try {
            RewardRedemption rr = svc.redeem(mc, id);
            return ResponseEntity.ok(redemptionDto(rr));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/api/tester/redemptions/mine")
    public ResponseEntity<?> testerListMyRedemptions(HttpServletRequest req) {
        String mc = requireTester(req); if (mc == null) return unauth();
        List<Map<String, Object>> out = new ArrayList<>();
        for (RewardRedemption rr : svc.listRedemptionsForTester(mc)) out.add(redemptionDto(rr));
        return ResponseEntity.ok(Map.of("redemptions", out, "count", out.size()));
    }

    // ============================================================ //
    // ADMIN ENDPOINTS — PACKAGES
    // ============================================================ //

    @PostMapping(value = "/api/admin/tester/packages", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> adminUploadPackage(@RequestParam MultipartFile file,
                                                @RequestParam String name,
                                                @RequestParam(required = false) String version,
                                                @RequestParam(required = false) String description) throws IOException {
        if (file.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "arquivo vazio"));
        ModPackage pkg = svc.uploadPackage(name, version, description, file, "admin");
        return ResponseEntity.ok(pkgDto(pkg));
    }

    @GetMapping("/api/admin/tester/packages")
    public Map<String, Object> adminListPackages() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (ModPackage p : svc.listPackages(false)) out.add(pkgDto(p));
        return Map.of("packages", out, "count", out.size());
    }

    @DeleteMapping("/api/admin/tester/packages/{id}")
    public Map<String, Object> adminDeletePackage(@PathVariable Long id) {
        svc.deletePackage(id);
        return Map.of("ok", true);
    }

    @PostMapping("/api/admin/tester/packages/{id}/toggle")
    public Map<String, Object> adminTogglePackage(@PathVariable Long id) {
        ModPackage pkg = svc.togglePackageEnabled(id);
        return pkgDto(pkg);
    }

    @GetMapping("/api/admin/tester/packages/{id}/dl")
    public ResponseEntity<?> adminDownloadPackage(@PathVariable Long id) throws IOException {
        return downloadPackageResponse(id);
    }

    // ============================================================ //
    // ADMIN ENDPOINTS — BETA ITEMS
    // ============================================================ //

    @GetMapping("/api/admin/tester/beta-items")
    public Map<String, Object> adminListBetaItems() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (BetaItem i : svc.listBetaItems(false)) out.add(betaItemDto(i));
        return Map.of("items", out, "count", out.size());
    }

    @PostMapping("/api/admin/tester/beta-items")
    public Map<String, Object> adminCreateBetaItem(@RequestBody BetaItem in) {
        in.setCreatedBy("admin");
        return betaItemDto(svc.createBetaItem(in));
    }

    @PutMapping("/api/admin/tester/beta-items/{id}")
    public Map<String, Object> adminUpdateBetaItem(@PathVariable Long id, @RequestBody BetaItem in) {
        return betaItemDto(svc.updateBetaItem(id, in));
    }

    @DeleteMapping("/api/admin/tester/beta-items/{id}")
    public Map<String, Object> adminDeleteBetaItem(@PathVariable Long id) {
        svc.deleteBetaItem(id);
        return Map.of("ok", true);
    }

    /**
     * Liga TODOS os beta items que estão com {@code enabled=false}. Usado
     * quando o admin importou um lote via bulk-import com {@code enabled:false}
     * por engano OU quando items antigos ficaram travados sem aparecer pros
     * testers. Retorna quantos foram afetados.
     *
     * Sintoma original: testers reclamavam "items beta tá vazio" mesmo com
     * 200+ items cadastrados no admin. Investigando, todos tavam
     * enabled=false. Esse endpoint é o "destrava tudo" do admin.
     */
    @PostMapping("/api/admin/tester/beta-items/enable-all")
    public Map<String, Object> adminEnableAllBetaItems() {
        int updated = 0;
        for (BetaItem it : svc.listBetaItems(false)) {
            if (!it.isEnabled()) {
                it.setEnabled(true);
                svc.updateBetaItem(it.getId(), it);
                updated++;
            }
        }
        return Map.of("ok", true, "updated", updated);
    }

    // ============================================================ //
    // ADMIN ENDPOINTS — BUGS
    // ============================================================ //

    @GetMapping("/api/admin/tester/bugs")
    public Map<String, Object> adminListBugs(@RequestParam(defaultValue = "false") boolean onlyPending) {
        List<BugReport> bugs = onlyPending ? svc.listPendingBugs() : svc.listAllBugs();
        List<Map<String, Object>> out = new ArrayList<>();
        for (BugReport b : bugs) out.add(bugDto(b));
        return Map.of("bugs", out, "count", out.size());
    }

    public record ConfirmBugRequest(int points, String note) {}

    @PostMapping("/api/admin/tester/bugs/{id}/confirm")
    public Map<String, Object> adminConfirmBug(@PathVariable Long id, @RequestBody ConfirmBugRequest body) {
        BugReport b = svc.confirmBug(id, body.points(), body.note(), "admin");
        return bugDto(b);
    }

    @PostMapping("/api/admin/tester/bugs/{id}/reject")
    public Map<String, Object> adminRejectBug(@PathVariable Long id, @RequestBody Map<String, String> body) {
        BugReport b = svc.rejectBug(id, body.get("note"), "admin");
        return bugDto(b);
    }

    // ============================================================ //
    // ADMIN ENDPOINTS — REWARDS
    // ============================================================ //

    @GetMapping("/api/admin/tester/rewards")
    public Map<String, Object> adminListRewards() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Reward r : svc.listRewards(false)) out.add(rewardDto(r));
        return Map.of("rewards", out, "count", out.size());
    }

    @PostMapping("/api/admin/tester/rewards")
    public Map<String, Object> adminCreateReward(@RequestBody Reward in) {
        return rewardDto(svc.createReward(in));
    }

    @PutMapping("/api/admin/tester/rewards/{id}")
    public Map<String, Object> adminUpdateReward(@PathVariable Long id, @RequestBody Reward in) {
        return rewardDto(svc.updateReward(id, in));
    }

    @DeleteMapping("/api/admin/tester/rewards/{id}")
    public Map<String, Object> adminDeleteReward(@PathVariable Long id) {
        svc.deleteReward(id);
        return Map.of("ok", true);
    }

    @GetMapping("/api/admin/tester/redemptions")
    public Map<String, Object> adminListRedemptions() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (RewardRedemption rr : svc.listAllRedemptions()) out.add(redemptionDto(rr));
        return Map.of("redemptions", out, "count", out.size());
    }

    @PostMapping("/api/admin/tester/redemptions/{id}/deliver")
    public Map<String, Object> adminMarkDelivered(@PathVariable Long id, @RequestBody Map<String, String> body) {
        RewardRedemption rr = svc.markDelivered(id, body.get("note"));
        return redemptionDto(rr);
    }

    // ============================================================ //
    // HELPERS
    // ============================================================ //

    private ResponseEntity<?> downloadPackageResponse(Long id) throws IOException {
        Optional<ModPackage> opt = svc.getPackage(id);
        if (opt.isEmpty()) {
            return ResponseEntity.status(404).contentType(MediaType.TEXT_HTML)
                    .body(packageMissingHtml("Pacote #" + id + " não existe no banco."));
        }
        Path file = svc.getPackagePath(id);
        ModPackage p = opt.get();
        if (file == null) {
            // v0.1.24: o registro existe no DB mas o ZIP físico SUMIU. Antes do
            // fix dos volumes persistentes, qualquer redeploy do backend
            // apagava /app/data/tester-packages. Agora isso só acontece se o
            // volume foi pruned manualmente OU se o package nunca foi
            // re-uploadado depois do fix.
            //
            // Mensagem em HTML porque o front baixa via <a href> — o browser
            // navega pra URL e renderiza o que recebe. JSON pelado fica feio
            // pro usuário final.
            return ResponseEntity.status(410).contentType(MediaType.TEXT_HTML)
                    .body(packageMissingHtml(
                            "O arquivo do pacote <b>" + escapeHtml(p.getName()) + "</b> " +
                            "(v" + escapeHtml(p.getVersion() == null ? "?" : p.getVersion()) + ") " +
                            "não está disponível no servidor.<br><br>" +
                            "Isso acontece quando o pacote foi enviado antes do fix de " +
                            "volume persistente (v0.1.24) — peça pro admin re-uploadar " +
                            "o ZIP no painel de admin."));
        }
        long len = Files.size(file);
        // STREAMING via InputStreamResource — não carrega o ZIP na heap.
        // Spring/Tomcat lê o FileInputStream chunk-a-chunk e escreve direto na
        // resposta. Resolveu OOM em packs > 200MB e o "Failed to fetch" do
        // browser que travava aguardando blob() montar o buffer inteiro.
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + p.getFilename() + "\"")
                .header("Accept-Ranges", "bytes")
                .header("Cache-Control", "private, max-age=0, no-cache")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(len)
                .body(new InputStreamResource(Files.newInputStream(file)));
    }

    private String requireTester(HttpServletRequest req) {
        // 1) Authorization: Bearer <token> (XHR/fetch padrão)
        String h = req.getHeader("Authorization");
        if (h != null && h.startsWith("Bearer ")) {
            String mc = testerSvc.validateToken(h.substring(7));
            if (mc != null) return mc;
        }
        // 2) Fallback ?token=<tester-token> — usado em downloads diretos via
        //    <a href="..."> onde não tem como mandar header Authorization.
        //    Eliminava o "Failed to fetch" do download de packages: antes o
        //    frontend fazia fetch() + blob() que dava OOM/timeout em arquivos
        //    grandes; agora a tag <a> baixa direto e o browser stream-a.
        String qpToken = req.getParameter("token");
        if (qpToken != null && !qpToken.isBlank()) {
            return testerSvc.validateToken(qpToken);
        }
        return null;
    }

    private ResponseEntity<?> unauth() {
        return ResponseEntity.status(401).body(Map.of("error", "token tester inválido"));
    }

    /**
     * Página HTML mostrada quando o tester clica num link de download que
     * aponta pra arquivo que não existe mais no disco. Tema dark pra casar
     * com o painel; link de voltar pro dashboard via JS history.back().
     */
    private static String packageMissingHtml(String reason) {
        return "<!DOCTYPE html>\n" +
                "<html lang=\"pt-BR\"><head><meta charset=\"utf-8\"><title>Arquivo indisponível</title>" +
                "<style>" +
                "body{background:#0f0817;color:#e7e1f2;font-family:system-ui,sans-serif;" +
                "display:flex;align-items:center;justify-content:center;min-height:100vh;margin:0;padding:24px}" +
                ".box{background:#1a0e2a;border:1px solid #5b2a8e;border-radius:12px;padding:32px;" +
                "max-width:520px;text-align:center;box-shadow:0 8px 32px rgba(91,42,142,0.3)}" +
                "h1{color:#ff7676;margin:0 0 16px;font-size:22px}" +
                "p{line-height:1.5;color:#cfc3e0}" +
                "a{color:#c997ff;text-decoration:none}" +
                "a:hover{text-decoration:underline}" +
                ".btn{display:inline-block;margin-top:20px;padding:10px 20px;" +
                "background:#5b2a8e;color:#fff;border-radius:8px;font-weight:bold}" +
                ".btn:hover{background:#7d3eb8;text-decoration:none}" +
                "</style></head><body><div class=\"box\">" +
                "<h1>⚠ Arquivo indisponível</h1>" +
                "<p>" + reason + "</p>" +
                "<a href=\"javascript:history.back()\" class=\"btn\">← Voltar</a>" +
                "</div></body></html>";
    }

    private static String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }

    private Map<String, Object> pkgDto(ModPackage p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", p.getId());
        m.put("name", p.getName());
        m.put("version", p.getVersion());
        m.put("description", p.getDescription());
        m.put("filename", p.getFilename());
        m.put("sizeBytes", p.getSizeBytes());
        m.put("uploadedBy", p.getUploadedBy());
        m.put("uploadedAt", p.getUploadedAt() == null ? null : p.getUploadedAt().toString());
        m.put("enabled", p.isEnabled());
        m.put("downloadCount", p.getDownloadCount());
        // v0.1.24: front usa pra desabilitar o botão de download em pacotes
        // "fantasma" (registro no DB mas ZIP perdido no disco). Embrulha em
        // try/catch defensivo: se o filesystem checar lento ou falhar (volume
        // não-montado, FS read-only, etc), default pra true (assume disponível).
        // Importante: nunca lançar exception aqui — esse DTO é serializado em
        // toda listagem de packages e qualquer falha aborta o endpoint inteiro.
        try {
            m.put("fileExists", svc.packageFileExists(p));
        } catch (Exception e) {
            m.put("fileExists", true);
        }
        return m;
    }

    private Map<String, Object> betaItemDto(BetaItem i) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", i.getId());
        m.put("name", i.getName());
        m.put("itemId", i.getItemId());
        m.put("kind", i.getKind() == null ? "ITEM" : i.getKind().name());
        m.put("description", i.getDescription());
        m.put("lore", i.getLore());
        m.put("propertiesJson", i.getPropertiesJson());
        m.put("recipeJson", i.getRecipeJson());
        m.put("effectsJson", i.getEffectsJson());
        m.put("giveCommand", i.getGiveCommand());
        m.put("imageUrl", i.getImageUrl());
        m.put("category", i.getCategory());
        m.put("enabled", i.isEnabled());
        m.put("createdAt", i.getCreatedAt() == null ? null : i.getCreatedAt().toString());
        return m;
    }

    private Map<String, Object> bugDto(BugReport b) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", b.getId());
        m.put("testerMcName", b.getTesterMcName());
        m.put("betaItemId", b.getBetaItemId());
        m.put("itemId", b.getItemId());
        m.put("title", b.getTitle());
        m.put("description", b.getDescription());
        m.put("stepsToReproduce", b.getStepsToReproduce());
        m.put("howFound", b.getHowFound());
        m.put("modVersion", b.getModVersion());
        m.put("mcVersion", b.getMcVersion());
        m.put("worldContext", b.getWorldContext());
        m.put("screenshotUrl", b.getScreenshotUrl());
        m.put("severity", b.getSeverity());
        // v96 — triage detalhada
        m.put("frequency", b.getFrequency());
        m.put("priority", b.getPriority());
        m.put("canReplicate", b.isCanReplicate());
        m.put("affectsOthers", b.isAffectsOthers());
        m.put("expectedBehavior", b.getExpectedBehavior());
        m.put("workaround", b.getWorkaround());
        m.put("tags", b.getTags());
        // Conta confirmações de replicação por outros testers
        m.put("replicationCount", countReplications(b.getConfirmationsJson()));
        m.put("status", b.getStatus().name());
        m.put("pointsAwarded", b.getPointsAwarded());
        m.put("adminNote", b.getAdminNote());
        m.put("triagedBy", b.getTriagedBy());
        m.put("triagedAt", b.getTriagedAt() == null ? null : b.getTriagedAt().toString());
        m.put("createdAt", b.getCreatedAt() == null ? null : b.getCreatedAt().toString());
        return m;
    }

    private static int countReplications(String json) {
        if (json == null || json.isBlank()) return 0;
        try {
            var arr = new com.fasterxml.jackson.databind.ObjectMapper()
                    .readValue(json, java.util.List.class);
            return arr.size();
        } catch (Exception e) { return 0; }
    }

    private Map<String, Object> rewardDto(Reward r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", r.getId());
        m.put("name", r.getName());
        m.put("description", r.getDescription());
        m.put("costPoints", r.getCostPoints());
        m.put("giveCommand", r.getGiveCommand());
        m.put("imageUrl", r.getImageUrl());
        m.put("category", r.getCategory());
        m.put("enabled", r.isEnabled());
        m.put("perTesterLimit", r.getPerTesterLimit());
        m.put("createdAt", r.getCreatedAt() == null ? null : r.getCreatedAt().toString());
        return m;
    }

    private Map<String, Object> redemptionDto(RewardRedemption rr) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", rr.getId());
        m.put("testerMcName", rr.getTesterMcName());
        m.put("rewardId", rr.getRewardId());
        m.put("rewardSnapshot", rr.getRewardSnapshot());
        m.put("pointsSpent", rr.getPointsSpent());
        m.put("delivered", rr.isDelivered());
        m.put("commandRun", rr.getCommandRun());
        m.put("deliveryNote", rr.getDeliveryNote());
        m.put("redeemedAt", rr.getRedeemedAt() == null ? null : rr.getRedeemedAt().toString());
        m.put("deliveredAt", rr.getDeliveredAt() == null ? null : rr.getDeliveredAt().toString());
        return m;
    }

    // ============================================================ //
    // MVP-6: 3D MODELS (BlockBench)
    // ============================================================ //

    /** Galeria pública (testers logados veem apenas modelos enabled). */
    @GetMapping("/api/tester/models")
    public ResponseEntity<?> testerListModels(HttpServletRequest req) {
        String mc = requireTester(req); if (mc == null) return unauth();
        List<Map<String, Object>> out = new ArrayList<>();
        for (Model3D m : svc.listModels(true)) out.add(modelDto(m));
        return ResponseEntity.ok(Map.of("models", out, "count", out.size()));
    }

    /** Baixa o JSON .bbmodel pro viewer 3D parsear. */
    @GetMapping("/api/tester/models/{id}/file")
    public ResponseEntity<?> testerDownloadModel(@PathVariable Long id, HttpServletRequest req) throws IOException {
        String mc = requireTester(req); if (mc == null) return unauth();
        return downloadModelResponse(id, true);
    }

    /** Detalhe do modelo (metadata, sem o arquivo). */
    @GetMapping("/api/tester/models/{id}")
    public ResponseEntity<?> testerGetModel(@PathVariable Long id, HttpServletRequest req) {
        String mc = requireTester(req); if (mc == null) return unauth();
        Optional<Model3D> opt = svc.getModel(id);
        if (opt.isEmpty() || !opt.get().isEnabled()) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(modelDto(opt.get()));
    }

    // -------- ADMIN --------

    @PostMapping(value = "/api/admin/tester/models", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> adminUploadModel(
            @RequestParam String name,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String category,
            @RequestParam("file") MultipartFile file) throws IOException {
        Model3D m = svc.uploadModel(name, description, category, file, "admin");
        return ResponseEntity.ok(modelDto(m));
    }

    @GetMapping("/api/admin/tester/models")
    public ResponseEntity<?> adminListModels() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Model3D m : svc.listModels(false)) out.add(modelDto(m));
        return ResponseEntity.ok(Map.of("models", out, "count", out.size()));
    }

    @GetMapping("/api/admin/tester/models/{id}/file")
    public ResponseEntity<?> adminDownloadModel(@PathVariable Long id) throws IOException {
        return downloadModelResponse(id, false);
    }

    @DeleteMapping("/api/admin/tester/models/{id}")
    public ResponseEntity<?> adminDeleteModel(@PathVariable Long id) throws IOException {
        svc.deleteModel(id);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @PostMapping("/api/admin/tester/models/{id}/toggle")
    public ResponseEntity<?> adminToggleModel(@PathVariable Long id) {
        Model3D m = svc.toggleModelEnabled(id);
        return ResponseEntity.ok(modelDto(m));
    }

    @PutMapping("/api/admin/tester/models/{id}")
    public ResponseEntity<?> adminUpdateModelMeta(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Model3D m = svc.updateModelMeta(id, body.get("name"), body.get("description"), body.get("category"));
        return ResponseEntity.ok(modelDto(m));
    }

    // -------- helpers --------

    private ResponseEntity<?> downloadModelResponse(Long id, boolean onlyEnabled) throws IOException {
        Optional<Model3D> opt = svc.getModel(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        Model3D m = opt.get();
        if (onlyEnabled && !m.isEnabled()) return ResponseEntity.notFound().build();
        byte[] data = svc.readModelFile(id);
        if (data == null) return ResponseEntity.notFound().build();
        // Content type por formato — gltf/bbmodel são JSON, glb/obj são binary/text
        String format = m.getFormat() == null ? "bbmodel" : m.getFormat();
        MediaType mt = switch (format) {
            case "glb"  -> MediaType.APPLICATION_OCTET_STREAM;
            case "obj"  -> MediaType.TEXT_PLAIN;
            case "gltf" -> MediaType.parseMediaType("model/gltf+json");
            default     -> MediaType.APPLICATION_JSON; // bbmodel
        };
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + m.getFilename() + "\"")
                .header("X-Model-Format", format)
                .contentType(mt)
                .contentLength(data.length)
                .body(new ByteArrayResource(data));
    }

    private Map<String, Object> modelDto(Model3D m) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("id", m.getId());
        out.put("name", m.getName());
        out.put("description", m.getDescription());
        out.put("category", m.getCategory());
        out.put("format", m.getFormat() == null ? "bbmodel" : m.getFormat());
        out.put("filename", m.getFilename());
        out.put("sizeBytes", m.getSizeBytes());
        out.put("uploadedBy", m.getUploadedBy());
        out.put("uploadedAt", m.getUploadedAt() == null ? null : m.getUploadedAt().toString());
        out.put("enabled", m.isEnabled());
        out.put("viewCount", m.getViewCount());
        return out;
    }

    // ============================================================ //
    // MVP-7: BETA AUDIOS
    // ============================================================ //

    @GetMapping("/api/tester/audios")
    public ResponseEntity<?> testerListAudios(HttpServletRequest req) {
        String mc = requireTester(req); if (mc == null) return unauth();
        List<BetaAudio> audios = svc.listAudios(true);
        List<Long> ids = audios.stream().map(BetaAudio::getId).toList();
        Map<Long, Map<String, Object>> votes = svc.bulkVoteCounts(BetaVote.Target.BETA_AUDIO, ids, mc);
        List<Map<String, Object>> out = new ArrayList<>();
        for (BetaAudio a : audios) {
            Map<String, Object> dto = audioDto(a);
            // Mesmo bug que tinha em testerListBetaItems: Map.of() lança NPE
            // quando algum value é null. Usar LinkedHashMap aceita null em
            // myVote (quando ninguém votou ainda).
            Map<String, Object> fallback = new LinkedHashMap<>();
            fallback.put("likes", 0);
            fallback.put("dislikes", 0);
            fallback.put("myVote", null);
            dto.put("votes", votes.getOrDefault(a.getId(), fallback));
            out.add(dto);
        }
        return ResponseEntity.ok(Map.of("audios", out, "count", out.size()));
    }

    @GetMapping("/api/tester/audios/{id}/stream")
    public ResponseEntity<?> testerStreamAudio(@PathVariable Long id, HttpServletRequest req) throws IOException {
        String mc = requireTester(req); if (mc == null) return unauth();
        return streamAudioResponse(id, true);
    }

    // -------- ADMIN --------

    @PostMapping(value = "/api/admin/tester/audios", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> adminUploadAudio(
            @RequestParam String name,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String creatureId,
            @RequestParam(required = false) Integer durationSec,
            @RequestParam("file") MultipartFile file) throws IOException {
        BetaAudio a = svc.uploadAudio(name, description, category, creatureId, durationSec, file, "admin");
        return ResponseEntity.ok(audioDto(a));
    }

    @GetMapping("/api/admin/tester/audios")
    public ResponseEntity<?> adminListAudios() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (BetaAudio a : svc.listAudios(false)) out.add(audioDto(a));
        return ResponseEntity.ok(Map.of("audios", out, "count", out.size()));
    }

    @GetMapping("/api/admin/tester/audios/{id}/stream")
    public ResponseEntity<?> adminStreamAudio(@PathVariable Long id) throws IOException {
        return streamAudioResponse(id, false);
    }

    @DeleteMapping("/api/admin/tester/audios/{id}")
    public ResponseEntity<?> adminDeleteAudio(@PathVariable Long id) {
        svc.deleteAudio(id);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @PostMapping("/api/admin/tester/audios/{id}/toggle")
    public ResponseEntity<?> adminToggleAudio(@PathVariable Long id) {
        BetaAudio a = svc.toggleAudioEnabled(id);
        return ResponseEntity.ok(audioDto(a));
    }

    @PutMapping("/api/admin/tester/audios/{id}")
    public ResponseEntity<?> adminUpdateAudioMeta(@PathVariable Long id, @RequestBody Map<String, String> body) {
        BetaAudio a = svc.updateAudioMeta(id, body.get("name"), body.get("description"),
                body.get("category"), body.get("creatureId"));
        return ResponseEntity.ok(audioDto(a));
    }

    // -------- VOTES (BETA_ITEM | BETA_AUDIO | MODEL_3D) --------

    /**
     * Body: {targetType, targetId, vote}
     *   targetType: "BETA_ITEM" | "BETA_AUDIO" | "MODEL_3D"
     *   vote: "LIKE" | "DISLIKE"
     *
     * Resposta: {voted: bool, vote: "LIKE"|"DISLIKE"|null, counts: {likes, dislikes, myVote}}
     *  - voted=false significa que o voto foi removido (toggle off — clicou de novo no mesmo)
     */
    @PostMapping("/api/tester/vote")
    public ResponseEntity<?> testerVote(@RequestBody Map<String, Object> body, HttpServletRequest req) {
        String mc = requireTester(req); if (mc == null) return unauth();
        try {
            String targetTypeStr = (String) body.get("targetType");
            Object idObj = body.get("targetId");
            String voteStr = (String) body.get("vote");
            if (targetTypeStr == null || idObj == null || voteStr == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "targetType, targetId, vote são obrigatórios"));
            }
            Long targetId = ((Number) idObj).longValue();
            BetaVote.Target target = BetaVote.Target.valueOf(targetTypeStr);
            BetaVote.Vote vote = BetaVote.Vote.valueOf(voteStr);
            BetaVote result = svc.vote(mc, target, targetId, vote);
            return ResponseEntity.ok(Map.of(
                    "voted", result != null,
                    "vote", result == null ? null : result.getVote().name(),
                    "counts", svc.voteCounts(target, targetId, mc)
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/api/tester/votes/{targetType}/{targetId}")
    public ResponseEntity<?> testerGetVotes(@PathVariable String targetType, @PathVariable Long targetId, HttpServletRequest req) {
        String mc = requireTester(req); if (mc == null) return unauth();
        try {
            BetaVote.Target target = BetaVote.Target.valueOf(targetType);
            return ResponseEntity.ok(svc.voteCounts(target, targetId, mc));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "targetType inválido"));
        }
    }

    // -------- helpers de audio --------

    private ResponseEntity<?> streamAudioResponse(Long id, boolean onlyEnabled) throws IOException {
        Optional<BetaAudio> opt = svc.getAudio(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        BetaAudio a = opt.get();
        if (onlyEnabled && !a.isEnabled()) return ResponseEntity.notFound().build();
        byte[] data = svc.readAudioFile(id);
        if (data == null) return ResponseEntity.notFound().build();
        String mime = a.getMimeType() != null ? a.getMimeType() : "audio/mpeg";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + a.getFilename() + "\"")
                .header("Accept-Ranges", "bytes")
                .header("Cache-Control", "private, max-age=3600")
                .contentType(MediaType.parseMediaType(mime))
                .contentLength(data.length)
                .body(new ByteArrayResource(data));
    }

    private Map<String, Object> audioDto(BetaAudio a) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("id", a.getId());
        out.put("name", a.getName());
        out.put("description", a.getDescription());
        out.put("category", a.getCategory());
        out.put("creatureId", a.getCreatureId());
        out.put("filename", a.getFilename());
        out.put("mimeType", a.getMimeType());
        out.put("sizeBytes", a.getSizeBytes());
        out.put("durationSec", a.getDurationSec());
        out.put("uploadedBy", a.getUploadedBy());
        out.put("uploadedAt", a.getUploadedAt() == null ? null : a.getUploadedAt().toString());
        out.put("enabled", a.isEnabled());
        out.put("playCount", a.getPlayCount());
        return out;
    }
}
