package br.com.murilo.liberthia.admin.engine.cinema;

import br.com.murilo.liberthia.admin.mod.ModBridgeClient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping("/api/cinema")
public class CinemaController {

    private final CinemaRepository repo;
    private final ModBridgeClient bridge;

    public CinemaController(CinemaRepository repo, ModBridgeClient bridge) {
        this.repo = repo; this.bridge = bridge;
    }

    @GetMapping
    public Page<CinemaSession> list(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size) {
        var pg = PageRequest.of(page, Math.min(size, 100));
        if (status != null && !status.isBlank()) return repo.findByStatusOrderByStartsAtAsc(status, pg);
        return repo.findAllByOrderByStartsAtDesc(pg);
    }

    @PostMapping
    public CinemaSession create(@RequestBody Map<String, Object> body) {
        CinemaSession s = new CinemaSession();
        apply(s, body);
        s.setCreatedAt(Instant.now());
        s.setRsvpsJson("[]");
        s.setStatus("scheduled");
        return repo.save(s);
    }

    @PutMapping("/{id}")
    public CinemaSession update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        CinemaSession s = repo.findById(id).orElseThrow();
        apply(s, body);
        return repo.save(s);
    }

    @PostMapping("/{id}/rsvp")
    public CinemaSession rsvp(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        CinemaSession s = repo.findById(id).orElseThrow();
        String uuid = (String) body.get("uuid");
        Set<String> set = new LinkedHashSet<>(parseRsvps(s.getRsvpsJson()));
        if (Boolean.TRUE.equals(body.get("remove"))) set.remove(uuid);
        else if (uuid != null) set.add(uuid);
        s.setRsvpsJson("[" + set.stream().map(u -> "\"" + u + "\"").reduce((a, b) -> a + "," + b).orElse("") + "]");
        return repo.save(s);
    }

    /** Dispara manualmente uma sessão (admin força). */
    @PostMapping("/{id}/start")
    public ResponseEntity<?> start(@PathVariable Long id) {
        CinemaSession s = repo.findById(id).orElseThrow();
        triggerStart(s);
        return ResponseEntity.ok(s);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        repo.deleteById(id);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    private void apply(CinemaSession s, Map<String, Object> b) {
        if (b.get("title") != null) s.setTitle((String) b.get("title"));
        if (b.get("description") != null) s.setDescription((String) b.get("description"));
        if (b.get("mediaUrl") != null) s.setMediaUrl((String) b.get("mediaUrl"));
        if (b.get("theaterLocation") != null) s.setTheaterLocation((String) b.get("theaterLocation"));
        if (b.get("createdBy") != null) s.setCreatedBy((String) b.get("createdBy"));
        if (b.get("startsAt") != null) s.setStartsAt(Instant.parse(b.get("startsAt").toString()));
    }

    private List<String> parseRsvps(String json) {
        if (json == null || json.isBlank() || "[]".equals(json)) return List.of();
        return Arrays.stream(json.replaceAll("[\\[\\]\"]", "").split(","))
                .map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    /** Scheduler: a cada minuto checa sessões scheduled cujo startsAt passou. */
    @Component
    public static class CinemaTrigger {
        private final CinemaRepository repo;
        private final ModBridgeClient bridge;
        public CinemaTrigger(CinemaRepository repo, ModBridgeClient bridge) {
            this.repo = repo; this.bridge = bridge;
        }
        @Scheduled(fixedRate = 60_000)
        public void tick() {
            var due = repo.findByStatusAndStartsAtLessThanEqual("scheduled", Instant.now());
            for (var s : due) {
                try {
                    fireWatermedia(bridge, s);
                    repo.save(s);
                } catch (Exception ignored) {}
            }
        }
    }

    /**
     * Dispara watermedia + TP coletivo de quem fez RSVP.
     * theaterLocation format: "minecraft:overworld:100,64,200"
     */
    private static void fireWatermedia(ModBridgeClient bridge, CinemaSession s) {
        bridge.broadcast(Map.of(
                "message", "§5§l▶ §dCinema:§r §f" + s.getTitle() + " §7- §b§n" + s.getMediaUrl(),
                "color", "light_purple"));
        if (s.getTheaterLocation() != null && !s.getTheaterLocation().isBlank()) {
            try {
                String loc = s.getTheaterLocation();
                int lastColon = loc.lastIndexOf(':');
                String dim = loc.substring(0, lastColon);
                String[] xyz = loc.substring(lastColon + 1).split(",");
                double tx = Double.parseDouble(xyz[0]);
                double ty = Double.parseDouble(xyz[1]);
                double tz = Double.parseDouble(xyz[2]);
                List<String> rsvps = parseRsvpsStatic(s.getRsvpsJson());
                for (String uuid : rsvps) {
                    try { bridge.teleport(uuid, Map.of("x", tx, "y", ty, "z", tz, "dimension", dim)); } catch (Exception ignored) {}
                }
                // Dispara watermedia no theater (precisa de display block lá ou usa wm em geral)
                bridge.runCommand(String.format("execute in %s positioned %.1f %.1f %.1f run wm play %s",
                        dim, tx, ty, tz, s.getMediaUrl()));
            } catch (Exception ignored) {}
        }
        s.setStatus("live");
    }

    private static List<String> parseRsvpsStatic(String json) {
        if (json == null || json.isBlank() || "[]".equals(json)) return List.of();
        return Arrays.stream(json.replaceAll("[\\[\\]\"]", "").split(","))
                .map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    private void triggerStart(CinemaSession s) {
        try {
            fireWatermedia(bridge, s);
            repo.save(s);
        } catch (Exception ignored) {}
    }
}
