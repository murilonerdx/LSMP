package br.com.murilo.liberthia.admin.engine.kubejs;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.*;
import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/kubejs")
public class KubeScriptController {

    private final KubeScriptRepository repo;
    /** Pasta KubeJS do server real — configurável. Vazio = só armazena no DB. */
    private final String kubeJsRoot;

    public KubeScriptController(KubeScriptRepository repo,
                                @Value("${kubejs.root:}") String kubeJsRoot) {
        this.repo = repo;
        this.kubeJsRoot = kubeJsRoot;
    }

    @GetMapping
    public Page<KubeScript> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String authorUuid,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        var pg = PageRequest.of(page, Math.min(size, 200));
        if (status != null && !status.isBlank()) return repo.findByStatusOrderByUpdatedAtDesc(status, pg);
        if (authorUuid != null && !authorUuid.isBlank()) return repo.findByAuthorUuidOrderByUpdatedAtDesc(authorUuid, pg);
        return repo.findAllByOrderByUpdatedAtDesc(pg);
    }

    @PostMapping
    public KubeScript create(@RequestBody Map<String, Object> body) {
        KubeScript s = new KubeScript();
        applyBody(s, body);
        s.setStatus("pending");
        s.setCreatedAt(Instant.now());
        s.setUpdatedAt(Instant.now());
        return repo.save(s);
    }

    @PutMapping("/{id}")
    public KubeScript update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        KubeScript s = repo.findById(id).orElseThrow();
        applyBody(s, body);
        s.setUpdatedAt(Instant.now());
        return repo.save(s);
    }

    /** Approve → escreve no disco do server (se kubejs.root configurado). */
    @PostMapping("/{id}/approve")
    public ResponseEntity<?> approve(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> body) {
        KubeScript s = repo.findById(id).orElseThrow();
        s.setStatus("approved");
        s.setReviewedBy(body == null ? null : (String) body.get("reviewedBy"));
        s.setReviewNote(body == null ? null : (String) body.get("reviewNote"));
        s.setUpdatedAt(Instant.now());
        repo.save(s);

        if (!kubeJsRoot.isBlank() && s.getSourceCode() != null) {
            try {
                Path sub = Paths.get(kubeJsRoot, s.getScope() + "_scripts");
                Files.createDirectories(sub);
                String fname = sanitize(s.getName()) + "_" + s.getId() + ".js";
                Files.writeString(sub.resolve(fname), s.getSourceCode());
                return ResponseEntity.ok(Map.of("ok", true, "path", sub.resolve(fname).toString()));
            } catch (Exception e) {
                return ResponseEntity.status(500).body(Map.of("ok", false, "error", e.getMessage()));
            }
        }
        return ResponseEntity.ok(Map.of("ok", true, "note", "approved (kubejs.root not configured, not deployed to disk)"));
    }

    @PostMapping("/{id}/reject")
    public KubeScript reject(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> body) {
        KubeScript s = repo.findById(id).orElseThrow();
        s.setStatus("rejected");
        s.setReviewNote(body == null ? null : (String) body.get("reviewNote"));
        s.setUpdatedAt(Instant.now());
        return repo.save(s);
    }

    @GetMapping(value = "/{id}/download", produces = MediaType.TEXT_PLAIN_VALUE)
    public String download(@PathVariable Long id) {
        KubeScript s = repo.findById(id).orElseThrow();
        s.setDownloads(s.getDownloads() + 1);
        repo.save(s);
        return s.getSourceCode();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        repo.deleteById(id);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    private void applyBody(KubeScript s, Map<String, Object> b) {
        if (b.get("name") != null) s.setName((String) b.get("name"));
        if (b.get("description") != null) s.setDescription((String) b.get("description"));
        if (b.get("sourceCode") != null) s.setSourceCode((String) b.get("sourceCode"));
        if (b.get("scope") != null) s.setScope((String) b.get("scope"));
        if (b.get("authorUuid") != null) s.setAuthorUuid((String) b.get("authorUuid"));
        if (b.get("authorName") != null) s.setAuthorName((String) b.get("authorName"));
    }

    private static String sanitize(String s) {
        return s == null ? "script" : s.replaceAll("[^a-zA-Z0-9_-]", "_").toLowerCase();
    }
}
