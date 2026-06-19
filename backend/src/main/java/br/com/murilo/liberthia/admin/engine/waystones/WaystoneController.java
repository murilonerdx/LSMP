package br.com.murilo.liberthia.admin.engine.waystones;

import br.com.murilo.liberthia.admin.mod.ModBridgeClient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/waystones")
public class WaystoneController {

    private final WaystoneRepository repo;
    private final ModBridgeClient bridge;

    public WaystoneController(WaystoneRepository repo, ModBridgeClient bridge) {
        this.repo = repo; this.bridge = bridge;
    }

    @GetMapping
    public Page<Waystone> list(
            @RequestParam(required = false) Boolean publicOnly,
            @RequestParam(required = false) String dimension,
            @RequestParam(required = false) String ownerUuid,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        var pg = PageRequest.of(page, Math.min(size, 500));
        if (Boolean.TRUE.equals(publicOnly)) return repo.findByPublicAccessTrueOrderByUseCountDesc(pg);
        if (dimension != null && !dimension.isBlank()) return repo.findByDimensionOrderByName(dimension, pg);
        if (ownerUuid != null && !ownerUuid.isBlank()) return repo.findByOwnerUuidOrderByCreatedAtDesc(ownerUuid, pg);
        return repo.findAllByOrderByCreatedAtDesc(pg);
    }

    @PostMapping
    public Waystone create(@RequestBody Map<String, Object> body) {
        Waystone w = new Waystone();
        apply(w, body);
        w.setCreatedAt(Instant.now());
        w.setUpdatedAt(Instant.now());
        return repo.save(w);
    }

    @PutMapping("/{id}")
    public Waystone update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Waystone w = repo.findById(id).orElseThrow();
        apply(w, body);
        w.setUpdatedAt(Instant.now());
        return repo.save(w);
    }

    /** Teleporta um player pra esta waystone (admin tool). */
    @PostMapping("/{id}/tp")
    public ResponseEntity<?> teleport(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Waystone w = repo.findById(id).orElseThrow();
        String playerUuid = (String) body.get("playerUuid");
        if (playerUuid == null) return ResponseEntity.badRequest().body(Map.of("error", "playerUuid required"));
        try {
            bridge.teleport(playerUuid, Map.of(
                    "x", w.getX(), "y", w.getY(), "z", w.getZ(),
                    "dimension", w.getDimension()));
            w.setUseCount(w.getUseCount() + 1);
            repo.save(w);
            return ResponseEntity.ok(Map.of("ok", true));
        } catch (Exception e) {
            return ResponseEntity.status(503).body(Map.of("error", "mod_offline", "message", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        repo.deleteById(id);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    private void apply(Waystone w, Map<String, Object> b) {
        if (b.get("name") != null) w.setName((String) b.get("name"));
        if (b.get("ownerUuid") != null) w.setOwnerUuid((String) b.get("ownerUuid"));
        if (b.get("ownerName") != null) w.setOwnerName((String) b.get("ownerName"));
        if (b.get("dimension") != null) w.setDimension((String) b.get("dimension"));
        if (b.get("x") != null) w.setX(toInt(b.get("x")));
        if (b.get("y") != null) w.setY(toInt(b.get("y")));
        if (b.get("z") != null) w.setZ(toInt(b.get("z")));
        if (b.get("publicAccess") != null) w.setPublicAccess(Boolean.parseBoolean(b.get("publicAccess").toString()));
    }

    private static int toInt(Object o) {
        if (o instanceof Number n) return n.intValue();
        try { return (int) Double.parseDouble(o.toString()); } catch (Exception e) { return 0; }
    }
}
