package br.com.murilo.liberthia.admin.engine.quotes;

import br.com.murilo.liberthia.admin.mod.ModBridgeClient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/quotes")
public class QuoteController {
    private final QuoteRepository repo;
    private final ModBridgeClient bridge;
    public QuoteController(QuoteRepository repo, ModBridgeClient bridge) {
        this.repo = repo;
        this.bridge = bridge;
    }

    /** Broadcasta a quote no chat do server como "frase memorável". */
    @PostMapping("/{id}/broadcast")
    public ResponseEntity<?> broadcast(@PathVariable Long id) {
        Quote q = repo.findById(id).orElseThrow();
        try {
            bridge.broadcast(Map.of(
                    "message", "§7§o— frase de §e" + q.getPlayerName() + "§7§o:§r §f\"" + q.getText() + "\"",
                    "color", "yellow"));
            return ResponseEntity.ok(Map.of("ok", true));
        } catch (Exception e) {
            return ResponseEntity.status(503).body(Map.of("error", "mod_offline", "message", e.getMessage()));
        }
    }

    /** Lista paginada — ?sort=top|recent|player&playerUuid=...&page=0&size=50&q=texto */
    @GetMapping
    public Page<Quote> list(
            @RequestParam(defaultValue = "recent") String sort,
            @RequestParam(required = false) String playerUuid,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        var pg = PageRequest.of(page, Math.min(size, 200));
        // Pattern já em lowercase + wildcards (repo só faz LOWER no campo).
        if (q != null && !q.isBlank()) return repo.search("%" + q.toLowerCase() + "%", pg);
        if (playerUuid != null && !playerUuid.isBlank())
            return repo.findByPlayerUuidOrderByCapturedAtDesc(playerUuid, pg);
        return switch (sort) {
            case "top" -> repo.findAllByOrderByScoreDesc(pg);
            default -> repo.findAllByOrderByCapturedAtDesc(pg);
        };
    }

    /** Cria manualmente uma quote (chat capturado, balão, etc). */
    @PostMapping
    public Quote create(@RequestBody Map<String, Object> body) {
        Quote q = new Quote();
        q.setPlayerUuid((String) body.getOrDefault("playerUuid", ""));
        q.setPlayerName((String) body.getOrDefault("playerName", "?"));
        q.setText((String) body.getOrDefault("text", ""));
        q.setDimension((String) body.getOrDefault("dimension", ""));
        q.setX(toDouble(body.get("x"))); q.setY(toDouble(body.get("y"))); q.setZ(toDouble(body.get("z")));
        q.setCapturedAt(Instant.now());
        return repo.save(q);
    }

    @PostMapping("/{id}/upvote")
    public Quote up(@PathVariable Long id) {
        Quote q = repo.findById(id).orElseThrow();
        q.setUpvotes(q.getUpvotes() + 1);
        q.setScore(q.getUpvotes() - q.getDownvotes());
        return repo.save(q);
    }

    @PostMapping("/{id}/downvote")
    public Quote down(@PathVariable Long id) {
        Quote q = repo.findById(id).orElseThrow();
        q.setDownvotes(q.getDownvotes() + 1);
        q.setScore(q.getUpvotes() - q.getDownvotes());
        return repo.save(q);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        repo.deleteById(id);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    private static double toDouble(Object o) {
        if (o == null) return 0;
        if (o instanceof Number n) return n.doubleValue();
        try { return Double.parseDouble(o.toString()); } catch (Exception e) { return 0; }
    }
}
