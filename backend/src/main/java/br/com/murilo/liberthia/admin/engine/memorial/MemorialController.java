package br.com.murilo.liberthia.admin.engine.memorial;

import br.com.murilo.liberthia.admin.mod.LiveEventBroadcaster;
import br.com.murilo.liberthia.admin.mod.ModBridgeClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/memorials")
public class MemorialController {

    private final MemorialRepository repo;
    private final LiveEventBroadcaster broadcaster;
    private final ModBridgeClient bridge;
    private final ObjectMapper mapper = new ObjectMapper();

    public MemorialController(MemorialRepository repo, LiveEventBroadcaster broadcaster, ModBridgeClient bridge) {
        this.repo = repo;
        this.broadcaster = broadcaster;
        this.bridge = bridge;
    }

    /** Teleporta player até a lápide. */
    @PostMapping("/{id}/visit")
    public ResponseEntity<?> visit(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Memorial m = repo.findById(id).orElseThrow();
        String playerUuid = (String) body.get("playerUuid");
        if (playerUuid == null) return ResponseEntity.badRequest().body(Map.of("error", "playerUuid required"));
        try {
            bridge.teleport(playerUuid, Map.of(
                    "x", m.getX(), "y", m.getY() + 1, "z", m.getZ(),
                    "dimension", m.getDimension()));
            return ResponseEntity.ok(Map.of("ok", true));
        } catch (Exception e) {
            return ResponseEntity.status(503).body(Map.of("error", e.getMessage()));
        }
    }

    /** Materializa uma cruz/lápide física no local da morte. */
    @PostMapping("/{id}/materialize")
    public ResponseEntity<?> materialize(@PathVariable Long id) {
        Memorial m = repo.findById(id).orElseThrow();
        try {
            String dim = m.getDimension() == null ? "minecraft:overworld" : m.getDimension();
            bridge.runCommand(String.format("execute in %s run setblock %d %d %d minecraft:cobblestone_wall",
                    dim, m.getX(), m.getY(), m.getZ()));
            bridge.runCommand(String.format("execute in %s run setblock %d %d %d minecraft:cobblestone_wall",
                    dim, m.getX(), m.getY() + 1, m.getZ()));
            String dateStr = m.getDiedAt() == null ? "" :
                    new java.text.SimpleDateFormat("dd/MM/yyyy").format(java.util.Date.from(m.getDiedAt()));
            String signNbt = String.format(
                    "{front_text:{messages:['[\"§l✝§r\"]','[\"%s\"]','[\"§7%s\"]','[\"§o%s\"]']}}",
                    safe(m.getPlayerName()), safe(m.getCauseOfDeath() == null ? "" : m.getCauseOfDeath()), dateStr);
            bridge.runCommand(String.format("execute in %s run setblock %d %d %d minecraft:oak_sign%s",
                    dim, m.getX(), m.getY() + 2, m.getZ(), signNbt));
            return ResponseEntity.ok(Map.of("ok", true, "at", m.getX() + "," + (m.getY() + 2) + "," + m.getZ()));
        } catch (Exception e) {
            return ResponseEntity.status(503).body(Map.of("error", e.getMessage()));
        }
    }

    private static String safe(String s) { return s == null ? "" : s.replace("'", " ").replace("\"", " "); }

    @GetMapping
    public Page<Memorial> list(
            @RequestParam(required = false) Boolean permanent,
            @RequestParam(required = false) String playerUuid,
            @RequestParam(defaultValue = "recent") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size) {
        var pg = PageRequest.of(page, Math.min(size, 100));
        if (Boolean.TRUE.equals(permanent)) return repo.findByPermanentTrueOrderByDiedAtDesc(pg);
        if (playerUuid != null && !playerUuid.isBlank()) return repo.findByPlayerUuidOrderByDiedAtDesc(playerUuid, pg);
        return "top".equals(sort) ? repo.findAllByOrderByReactionsDesc(pg) : repo.findAllByOrderByDiedAtDesc(pg);
    }

    @GetMapping("/{id}")
    public Memorial get(@PathVariable Long id) {
        return repo.findById(id).orElseThrow();
    }

    /** Cria memorial — chamado pelo mod no death event, ou manualmente. */
    @PostMapping
    public Memorial create(@RequestBody Map<String, Object> body) {
        Memorial m = new Memorial();
        m.setPlayerUuid((String) body.getOrDefault("playerUuid", ""));
        m.setPlayerName((String) body.getOrDefault("playerName", "?"));
        m.setCauseOfDeath((String) body.getOrDefault("causeOfDeath", "unknown"));
        m.setEpitaph((String) body.getOrDefault("epitaph", ""));
        m.setSnapshotJson(body.get("snapshotJson") == null ? null : body.get("snapshotJson").toString());
        m.setDimension((String) body.getOrDefault("dimension", ""));
        m.setX(toInt(body.get("x"))); m.setY(toInt(body.get("y"))); m.setZ(toInt(body.get("z")));
        m.setPermanent(Boolean.TRUE.equals(body.get("permanent")));
        m.setDiedAt(Instant.now());
        Memorial saved = repo.save(m);
        pushWs(saved);
        return saved;
    }

    /** Player escreve epitáfio (popup pós-morte). */
    @PostMapping("/{id}/epitaph")
    public Memorial epitaph(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Memorial m = repo.findById(id).orElseThrow();
        m.setEpitaph((String) body.getOrDefault("epitaph", ""));
        return repo.save(m);
    }

    @PostMapping("/{id}/react")
    public Memorial react(@PathVariable Long id) {
        Memorial m = repo.findById(id).orElseThrow();
        m.setReactions(m.getReactions() + 1);
        return repo.save(m);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        repo.deleteById(id);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    private void pushWs(Memorial m) {
        try {
            Map<String, Object> ev = new LinkedHashMap<>();
            ev.put("type", "memorial_created");
            ev.put("id", m.getId());
            ev.put("playerName", m.getPlayerName());
            ev.put("causeOfDeath", m.getCauseOfDeath());
            ev.put("permanent", m.isPermanent());
            ev.put("diedAt", m.getDiedAt() == null ? null : m.getDiedAt().toString());
            broadcaster.broadcast(mapper.writeValueAsString(ev));
        } catch (Exception ignored) {}
    }

    private static int toInt(Object o) {
        if (o == null) return 0;
        if (o instanceof Number n) return n.intValue();
        try { return (int) Double.parseDouble(o.toString()); } catch (Exception e) { return 0; }
    }
}
