package br.com.murilo.liberthia.admin.engine.backrooms;

import br.com.murilo.liberthia.admin.mod.ModBridgeClient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/backrooms")
public class BackroomsController {
    private final BackroomsRepository repo;
    private final ModBridgeClient bridge;
    public BackroomsController(BackroomsRepository repo, ModBridgeClient bridge) {
        this.repo = repo;
        this.bridge = bridge;
    }

    /** Resgata player de qualquer dimensão de volta pro overworld no spawn. */
    @PostMapping("/rescue")
    public ResponseEntity<?> rescue(@RequestBody Map<String, Object> body) {
        String playerUuid = (String) body.get("playerUuid");
        if (playerUuid == null) return ResponseEntity.badRequest().body(Map.of("error", "playerUuid required"));
        try {
            bridge.teleport(playerUuid, Map.of(
                    "x", 0, "y", 100, "z", 0,
                    "dimension", "minecraft:overworld"));
            bridge.title(playerUuid, Map.of(
                    "title", "§a✓ §lRESGATADO",
                    "subtitle", "§7Você foi trazido de volta",
                    "fadeIn", 10, "stay", 60, "fadeOut", 10));
            bridge.sound(playerUuid, Map.of("sound", "minecraft:entity.experience_orb.pickup", "volume", 1.0, "pitch", 0.8));
            return ResponseEntity.ok(Map.of("ok", true));
        } catch (Exception e) {
            return ResponseEntity.status(503).body(Map.of("error", e.getMessage()));
        }
    }

    /** Feed completo. */
    @GetMapping
    public Page<BackroomsEntry> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        return repo.findAllByOrderByOccurredAtDesc(PageRequest.of(page, Math.min(size, 500)));
    }

    /** Players "perdidos" — última ação foi enter em algum level e ainda não exit. */
    @GetMapping("/lost")
    public List<Map<String, Object>> lost() {
        List<BackroomsEntry> all = repo.findAllByOrderByOccurredAtDesc(PageRequest.of(0, 1000)).getContent();
        Map<String, BackroomsEntry> lastByPlayer = new LinkedHashMap<>();
        for (var e : all) lastByPlayer.putIfAbsent(e.getPlayerUuid(), e);
        return lastByPlayer.values().stream()
                .filter(e -> "enter".equals(e.getEventType()))
                .map(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("playerUuid", e.getPlayerUuid());
                    m.put("playerName", e.getPlayerName());
                    m.put("levelId", e.getLevelId());
                    m.put("enteredAt", e.getOccurredAt());
                    long secs = Instant.now().getEpochSecond() - e.getOccurredAt().getEpochSecond();
                    m.put("hoursLost", secs / 3600.0);
                    return m;
                })
                .collect(Collectors.toList());
    }

    /** Estatística por level. */
    @GetMapping("/stats")
    public List<Map<String, Object>> stats() {
        List<BackroomsEntry> all = repo.findAllByOrderByOccurredAtDesc(PageRequest.of(0, 5000)).getContent();
        Map<String, Long[]> byLevel = new HashMap<>(); // [enters, exits]
        for (var e : all) {
            Long[] arr = byLevel.computeIfAbsent(e.getLevelId(), k -> new Long[]{0L, 0L});
            if ("enter".equals(e.getEventType())) arr[0]++;
            else if ("exit".equals(e.getEventType())) arr[1]++;
        }
        return byLevel.entrySet().stream()
                .map(en -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("levelId", en.getKey());
                    m.put("enters", en.getValue()[0]);
                    m.put("exits", en.getValue()[1]);
                    m.put("currently", en.getValue()[0] - en.getValue()[1]);
                    return m;
                })
                .sorted((a, b) -> Long.compare((Long) b.get("enters"), (Long) a.get("enters")))
                .collect(Collectors.toList());
    }

    /** Cria entry manual (mod chama isso quando detecta dimension change). */
    @PostMapping
    public BackroomsEntry create(@RequestBody Map<String, Object> body) {
        BackroomsEntry e = new BackroomsEntry();
        e.setPlayerUuid((String) body.getOrDefault("playerUuid", ""));
        e.setPlayerName((String) body.getOrDefault("playerName", "?"));
        e.setLevelId((String) body.getOrDefault("levelId", ""));
        e.setEventType((String) body.getOrDefault("eventType", "enter"));
        e.setX(toDouble(body.get("x"))); e.setY(toDouble(body.get("y"))); e.setZ(toDouble(body.get("z")));
        e.setOccurredAt(Instant.now());
        return repo.save(e);
    }

    private static double toDouble(Object o) {
        if (o == null) return 0;
        if (o instanceof Number n) return n.doubleValue();
        try { return Double.parseDouble(o.toString()); } catch (Exception ex) { return 0; }
    }
}
