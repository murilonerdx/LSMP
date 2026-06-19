package br.com.murilo.liberthia.admin.engine.security;

import br.com.murilo.liberthia.admin.mod.LiveEventBroadcaster;
import br.com.murilo.liberthia.admin.mod.ModBridgeClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/security")
public class SecurityEventController {

    private final SecurityEventRepository repo;
    private final LiveEventBroadcaster broadcaster;
    private final ModBridgeClient bridge;
    private final ObjectMapper mapper = new ObjectMapper();

    public SecurityEventController(SecurityEventRepository repo, LiveEventBroadcaster broadcaster, ModBridgeClient bridge) {
        this.repo = repo;
        this.broadcaster = broadcaster;
        this.bridge = bridge;
    }

    /** TP até o local do evento (admin investiga in-loco). */
    @PostMapping("/{id}/tp")
    public ResponseEntity<?> tp(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        SecurityEvent e = repo.findById(id).orElseThrow();
        String playerUuid = (String) body.get("playerUuid");
        try {
            bridge.teleport(playerUuid, Map.of("x", e.getX(), "y", e.getY() + 1, "z", e.getZ(),
                    "dimension", e.getDimension()));
            return ResponseEntity.ok(Map.of("ok", true));
        } catch (Exception ex) {
            return ResponseEntity.status(503).body(Map.of("error", ex.getMessage()));
        }
    }

    /** Notifica o dono via title + sino. */
    @PostMapping("/{id}/notify-owner")
    public ResponseEntity<?> notifyOwner(@PathVariable Long id) {
        SecurityEvent e = repo.findById(id).orElseThrow();
        if (e.getOwnerUuid() == null || e.getOwnerUuid().isBlank())
            return ResponseEntity.badRequest().body(Map.of("error", "no owner"));
        try {
            bridge.title(e.getOwnerUuid(), Map.of(
                    "title", "§c⚠ §lINVASÃO",
                    "subtitle", "§f" + (e.getIntruderName() == null ? "?" : e.getIntruderName()) + " §7na sua base",
                    "fadeIn", 5, "stay", 80, "fadeOut", 10));
            bridge.sound(e.getOwnerUuid(), Map.of("sound", "minecraft:block.bell.use", "volume", 1.0, "pitch", 1.0));
            return ResponseEntity.ok(Map.of("ok", true));
        } catch (Exception ex) {
            return ResponseEntity.status(503).body(Map.of("error", ex.getMessage()));
        }
    }

    /** Kicka o intruso. */
    @PostMapping("/{id}/kick")
    public ResponseEntity<?> kick(@PathVariable Long id) {
        SecurityEvent e = repo.findById(id).orElseThrow();
        if (e.getIntruderUuid() == null || e.getIntruderUuid().isBlank())
            return ResponseEntity.badRequest().body(Map.of("error", "no intruder"));
        try {
            bridge.kick(e.getIntruderUuid(), Map.of("reason", "Tentativa de invasão detectada"));
            return ResponseEntity.ok(Map.of("ok", true));
        } catch (Exception ex) {
            return ResponseEntity.status(503).body(Map.of("error", ex.getMessage()));
        }
    }

    @GetMapping
    public Page<SecurityEvent> list(
            @RequestParam(required = false) String kind,
            @RequestParam(required = false) String ownerUuid,
            @RequestParam(required = false) String intruderUuid,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        var pg = PageRequest.of(page, Math.min(size, 500));
        if (kind != null && !kind.isBlank()) return repo.findByKindOrderByOccurredAtDesc(kind, pg);
        if (ownerUuid != null && !ownerUuid.isBlank()) return repo.findByOwnerUuidOrderByOccurredAtDesc(ownerUuid, pg);
        if (intruderUuid != null && !intruderUuid.isBlank()) return repo.findByIntruderUuidOrderByOccurredAtDesc(intruderUuid, pg);
        return repo.findAllByOrderByOccurredAtDesc(pg);
    }

    /** Stats últimas 24h por owner — quem foi mais "invadido". */
    @GetMapping("/stats/last-24h")
    public List<Map<String, Object>> stats() {
        var events = repo.findByOccurredAtAfterOrderByOccurredAtDesc(Instant.now().minus(24, ChronoUnit.HOURS));
        Map<String, Map<String, Object>> agg = new HashMap<>();
        for (var e : events) {
            agg.computeIfAbsent(e.getOwnerUuid() == null ? "" : e.getOwnerUuid(), k -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("ownerUuid", e.getOwnerUuid());
                m.put("ownerName", e.getOwnerName());
                m.put("count", 0);
                m.put("uniqueIntruders", new HashSet<String>());
                return m;
            });
            Map<String, Object> m = agg.get(e.getOwnerUuid() == null ? "" : e.getOwnerUuid());
            m.put("count", ((int) m.get("count")) + 1);
            @SuppressWarnings("unchecked")
            Set<String> set = (Set<String>) m.get("uniqueIntruders");
            if (e.getIntruderUuid() != null) set.add(e.getIntruderUuid());
        }
        return agg.values().stream()
                .peek(m -> m.put("uniqueIntruders", ((Set<?>) m.get("uniqueIntruders")).size()))
                .sorted((a, b) -> Integer.compare((int) b.get("count"), (int) a.get("count")))
                .collect(Collectors.toList());
    }

    @PostMapping
    public SecurityEvent create(@RequestBody Map<String, Object> body) {
        SecurityEvent e = new SecurityEvent();
        e.setKind((String) body.getOrDefault("kind", "unknown"));
        e.setIntruderUuid((String) body.getOrDefault("intruderUuid", ""));
        e.setIntruderName((String) body.getOrDefault("intruderName", "?"));
        e.setOwnerUuid((String) body.getOrDefault("ownerUuid", ""));
        e.setOwnerName((String) body.getOrDefault("ownerName", "?"));
        e.setDimension((String) body.getOrDefault("dimension", ""));
        e.setX(toInt(body.get("x"))); e.setY(toInt(body.get("y"))); e.setZ(toInt(body.get("z")));
        e.setBlockTypeId((String) body.getOrDefault("blockTypeId", ""));
        e.setDetail((String) body.getOrDefault("detail", ""));
        e.setOccurredAt(Instant.now());
        SecurityEvent saved = repo.save(e);
        // Push WS — Activity Wall do front atualiza em real-time
        pushWs(saved);
        return saved;
    }

    private void pushWs(SecurityEvent e) {
        try {
            Map<String, Object> ev = new LinkedHashMap<>();
            ev.put("type", "security_event");
            ev.put("id", e.getId());
            ev.put("kind", e.getKind());
            ev.put("intruderName", e.getIntruderName());
            ev.put("ownerName", e.getOwnerName());
            ev.put("dimension", e.getDimension());
            ev.put("x", e.getX()); ev.put("y", e.getY()); ev.put("z", e.getZ());
            ev.put("detail", e.getDetail());
            ev.put("occurredAt", e.getOccurredAt() == null ? null : e.getOccurredAt().toString());
            broadcaster.broadcast(mapper.writeValueAsString(ev));
        } catch (Exception ex) {
            // log silencioso — push é best-effort
        }
    }

    private static int toInt(Object o) {
        if (o == null) return 0;
        if (o instanceof Number n) return n.intValue();
        try { return (int) Double.parseDouble(o.toString()); } catch (Exception e) { return 0; }
    }
}
