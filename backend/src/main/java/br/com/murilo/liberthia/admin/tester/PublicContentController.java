package br.com.murilo.liberthia.admin.tester;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Endpoints públicos (sem auth) e admin para Changelog, Roadmap e Bug Hunter
 * Leaderboard.
 *
 * PÚBLICO (sem auth — adicionar em AuthFilter.isPublic):
 *  - GET  /api/public/changelog
 *  - GET  /api/public/changelog/{id}
 *  - GET  /api/public/roadmap                  (agrupado por categoria)
 *  - GET  /api/public/roadmap/flat             (lista simples)
 *  - POST /api/public/roadmap/{id}/vote
 *  - POST /api/public/roadmap/{id}/unvote
 *  - GET  /api/public/leaderboard              (bug hunter ranking)
 *
 * ADMIN (Bearer JWT):
 *  - POST   /api/admin/changelog
 *  - PUT    /api/admin/changelog/{id}
 *  - DELETE /api/admin/changelog/{id}
 *  - POST   /api/admin/roadmap
 *  - PUT    /api/admin/roadmap/{id}
 *  - DELETE /api/admin/roadmap/{id}
 */
@RestController
public class PublicContentController {

    private final PublicContentService svc;

    public PublicContentController(PublicContentService svc) {
        this.svc = svc;
    }

    // ===== Changelog público =====
    @GetMapping("/api/public/changelog")
    public Map<String, Object> publicChangelog() {
        var list = svc.listChangelog();
        return Map.of("entries", list, "count", list.size());
    }

    @GetMapping("/api/public/changelog/{id}")
    public ResponseEntity<?> publicChangelogOne(@PathVariable Long id) {
        var dto = svc.getChangelog(id);
        if (dto == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(dto);
    }

    // ===== Roadmap público =====
    @GetMapping("/api/public/roadmap")
    public Map<String, Object> publicRoadmap() {
        return Map.of("grouped", svc.listRoadmapGrouped());
    }

    @GetMapping("/api/public/roadmap/flat")
    public Map<String, Object> publicRoadmapFlat() {
        var list = svc.listRoadmap();
        return Map.of("items", list, "count", list.size());
    }

    @PostMapping("/api/public/roadmap/{id}/vote")
    public ResponseEntity<?> publicVote(@PathVariable Long id) {
        try {
            var it = svc.vote(id);
            return ResponseEntity.ok(Map.of("ok", true, "votes", it.getVotes()));
        } catch (Exception e) {
            return ResponseEntity.status(404).body(Map.of("ok", false, "error", "not_found"));
        }
    }

    @PostMapping("/api/public/roadmap/{id}/unvote")
    public ResponseEntity<?> publicUnvote(@PathVariable Long id) {
        try {
            var it = svc.unvote(id);
            return ResponseEntity.ok(Map.of("ok", true, "votes", it.getVotes()));
        } catch (Exception e) {
            return ResponseEntity.status(404).body(Map.of("ok", false, "error", "not_found"));
        }
    }

    // ===== Bug Hunter Leaderboard público =====
    @GetMapping("/api/public/leaderboard")
    public Map<String, Object> publicLeaderboard() {
        return svc.bugLeaderboard();
    }

    // ===== Admin: Changelog =====
    @PostMapping("/api/admin/changelog")
    public ChangelogEntry adminChangelogCreate(@RequestBody Map<String, Object> body) {
        String createdBy = body.get("createdBy") != null ? body.get("createdBy").toString() : "admin";
        return svc.createChangelog(body, createdBy);
    }

    @PutMapping("/api/admin/changelog/{id}")
    public ChangelogEntry adminChangelogUpdate(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return svc.updateChangelog(id, body);
    }

    @DeleteMapping("/api/admin/changelog/{id}")
    public Map<String, Object> adminChangelogDelete(@PathVariable Long id) {
        svc.deleteChangelog(id);
        return Map.of("ok", true);
    }

    /**
     * Auto-gera um draft de PRÓXIMA entry de changelog baseado em:
     *   • Bugs CONFIRMED após o último changelog OU após o último mod package
     *     (o mais recente dos 2 — pra não repetir bugs do release anterior)
     *   • Suggestions APPROVED/IMPLEMENTED no mesmo período
     *
     * Retorna estrutura pronta pra entry com bugs/suggestions estruturados.
     * Admin pode editar antes de POST /api/admin/changelog ou bulk-import.
     */
    @GetMapping("/api/admin/changelog/auto-generate-next")
    public Map<String, Object> adminAutoGenerateNext() {
        return svc.autoGenerateNextChangelog();
    }

    // ===== Admin: Roadmap =====
    @PostMapping("/api/admin/roadmap")
    public RoadmapItem adminRoadmapCreate(@RequestBody Map<String, Object> body) {
        String createdBy = body.get("createdBy") != null ? body.get("createdBy").toString() : "admin";
        return svc.createRoadmap(body, createdBy);
    }

    @PutMapping("/api/admin/roadmap/{id}")
    public RoadmapItem adminRoadmapUpdate(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return svc.updateRoadmap(id, body);
    }

    @DeleteMapping("/api/admin/roadmap/{id}")
    public Map<String, Object> adminRoadmapDelete(@PathVariable Long id) {
        svc.deleteRoadmap(id);
        return Map.of("ok", true);
    }
}
