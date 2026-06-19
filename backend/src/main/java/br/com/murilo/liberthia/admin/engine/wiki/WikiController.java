package br.com.murilo.liberthia.admin.engine.wiki;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/wiki")
public class WikiController {
    private final WikiPageRepository repo;
    private final br.com.murilo.liberthia.admin.mod.ModBridgeClient bridge;
    public WikiController(WikiPageRepository repo, br.com.murilo.liberthia.admin.mod.ModBridgeClient bridge) {
        this.repo = repo;
        this.bridge = bridge;
    }

    /** Gera um written_book com o conteúdo da wiki e dá pro player. */
    @PostMapping("/{id}/give-book")
    public org.springframework.http.ResponseEntity<?> giveBook(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        WikiPage p = repo.findById(id).orElseThrow();
        String playerUuid = (String) body.get("playerUuid");
        if (playerUuid == null) return org.springframework.http.ResponseEntity.badRequest().body(Map.of("error", "playerUuid required"));
        try {
            String title = (p.getTitle() == null ? "Livro" : p.getTitle()).replace("\"", " ");
            String author = (p.getAuthorName() == null ? "Anônimo" : p.getAuthorName()).replace("\"", " ");
            String content = (p.getContentMarkdown() == null ? "" : p.getContentMarkdown());
            // Quebra o conteúdo em páginas (~250 chars cada)
            java.util.List<String> pages = new java.util.ArrayList<>();
            int i = 0;
            while (i < content.length()) {
                int end = Math.min(i + 250, content.length());
                pages.add(content.substring(i, end));
                i = end;
            }
            if (pages.isEmpty()) pages.add(" ");
            String pagesNbt = pages.stream()
                    .map(s -> "'{\"text\":\"" + s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\"}'")
                    .reduce((a, b) -> a + "," + b).orElse("'\"\"'");
            String nbt = String.format("{title:\"%s\",author:\"%s\",pages:[%s]}", title, author, pagesNbt);
            bridge.runCommand("give " + playerUuid + " minecraft:written_book" + nbt);
            return org.springframework.http.ResponseEntity.ok(Map.of("ok", true));
        } catch (Exception e) {
            return org.springframework.http.ResponseEntity.status(503).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping
    public Page<WikiPage> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        var pg = PageRequest.of(page, Math.min(size, 200));
        // Repo.search espera o pattern JÁ embrulhado e em lowercase (a query
        // só faz LOWER(coluna) LIKE :pattern — não toca no parâmetro).
        if (q != null && !q.isBlank()) return repo.search("%" + q.toLowerCase() + "%", pg);
        if (category != null && !category.isBlank()) return repo.findByCategoryOrderByUpdatedAtDesc(category, pg);
        if (status != null && !status.isBlank()) return repo.findByStatusOrderByUpdatedAtDesc(status, pg);
        return repo.findAllByOrderByUpdatedAtDesc(pg);
    }

    @GetMapping("/{idOrSlug}")
    public ResponseEntity<WikiPage> get(@PathVariable String idOrSlug) {
        WikiPage p;
        if (idOrSlug.matches("\\d+")) p = repo.findById(Long.parseLong(idOrSlug)).orElse(null);
        else p = repo.findBySlug(idOrSlug).orElse(null);
        if (p == null) return ResponseEntity.notFound().build();
        p.setViews(p.getViews() + 1);
        repo.save(p);
        return ResponseEntity.ok(p);
    }

    @PostMapping
    public WikiPage create(@RequestBody Map<String, Object> body) {
        WikiPage p = new WikiPage();
        applyBody(p, body);
        p.setCreatedAt(Instant.now());
        p.setUpdatedAt(Instant.now());
        return repo.save(p);
    }

    @PutMapping("/{id}")
    public WikiPage update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        WikiPage p = repo.findById(id).orElseThrow();
        applyBody(p, body);
        p.setUpdatedAt(Instant.now());
        return repo.save(p);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        repo.deleteById(id);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    /** Gera JSON no formato Patchouli pra a página virar livro in-game. */
    @GetMapping("/{id}/export-patchouli")
    public Map<String, Object> exportPatchouli(@PathVariable Long id) {
        WikiPage p = repo.findById(id).orElseThrow();
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("name", p.getTitle());
        json.put("icon", "minecraft:book");
        json.put("category", "liberthia_lore:" + (p.getCategory() == null ? "general" : p.getCategory()));
        json.put("priority", true);
        json.put("pages", java.util.List.of(
                Map.of("type", "patchouli:text",
                        "title", p.getTitle(),
                        "text", p.getContentMarkdown() == null ? "" : p.getContentMarkdown())
        ));
        return json;
    }

    private void applyBody(WikiPage p, Map<String, Object> b) {
        if (b.get("slug") != null) p.setSlug((String) b.get("slug"));
        if (b.get("title") != null) p.setTitle((String) b.get("title"));
        if (b.get("category") != null) p.setCategory((String) b.get("category"));
        if (b.get("contentMarkdown") != null) p.setContentMarkdown((String) b.get("contentMarkdown"));
        if (b.get("authorUuid") != null) p.setAuthorUuid((String) b.get("authorUuid"));
        if (b.get("authorName") != null) p.setAuthorName((String) b.get("authorName"));
        if (b.get("status") != null) p.setStatus((String) b.get("status"));
    }
}
