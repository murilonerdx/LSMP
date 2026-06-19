package br.com.murilo.liberthia.admin.engine.cutscene2;

import br.com.murilo.liberthia.admin.mod.ModBridgeClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/cutscenes-director")
public class Cutscene2Controller {

    private final Cutscene2Repository repo;
    private final ModBridgeClient bridge;
    private final ObjectMapper mapper = new ObjectMapper();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private final Map<Long, List<java.util.concurrent.ScheduledFuture<?>>> running = new ConcurrentHashMap<>();

    public Cutscene2Controller(Cutscene2Repository repo, ModBridgeClient bridge) {
        this.repo = repo; this.bridge = bridge;
    }

    @GetMapping
    public Page<Cutscene2> list(@RequestParam(defaultValue = "0") int page,
                                @RequestParam(defaultValue = "50") int size) {
        return repo.findAllByOrderByUpdatedAtDesc(PageRequest.of(page, Math.min(size, 200)));
    }

    @GetMapping("/{id}")
    public Cutscene2 get(@PathVariable Long id) { return repo.findById(id).orElseThrow(); }

    @PostMapping
    public Cutscene2 create(@RequestBody Map<String, Object> body) {
        Cutscene2 c = new Cutscene2();
        apply(c, body);
        c.setCreatedAt(Instant.now());
        c.setUpdatedAt(Instant.now());
        return repo.save(c);
    }

    @PutMapping("/{id}")
    public Cutscene2 update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Cutscene2 c = repo.findById(id).orElseThrow();
        apply(c, body);
        c.setUpdatedAt(Instant.now());
        return repo.save(c);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        cancel(id);
        repo.deleteById(id);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    /** Dispara a cutscene — agenda cada track no tempo configurado. */
    @PostMapping("/{id}/play")
    public ResponseEntity<?> play(@PathVariable Long id) {
        Cutscene2 c = repo.findById(id).orElseThrow();
        cancel(id);
        try {
            JsonNode tracks = mapper.readTree(c.getTracksJson() == null ? "[]" : c.getTracksJson());
            List<java.util.concurrent.ScheduledFuture<?>> futures = new ArrayList<>();
            for (JsonNode track : tracks) {
                long atMs = track.path("atMs").asLong(0);
                String type = track.path("type").asText("");
                JsonNode payload = track.path("payload");
                futures.add(scheduler.schedule(() -> dispatch(type, payload),
                        Math.max(0, atMs), TimeUnit.MILLISECONDS));
            }
            running.put(id, futures);
            c.setPlays(c.getPlays() + 1);
            repo.save(c);
            return ResponseEntity.ok(Map.of("ok", true, "tracksScheduled", futures.size()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/stop")
    public ResponseEntity<?> stop(@PathVariable Long id) {
        cancel(id);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    private void cancel(Long id) {
        var futs = running.remove(id);
        if (futs != null) futs.forEach(f -> f.cancel(false));
    }

    /** Dispatcher: traduz cada track type pra uma chamada ModBridgeClient. */
    private void dispatch(String type, JsonNode payload) {
        try {
            Map<String, Object> map = mapper.convertValue(payload, Map.class);
            switch (type) {
                case "title" -> {
                    String uuid = (String) map.get("playerUuid");
                    if (uuid != null) bridge.title(uuid, map);
                    else {
                        // broadcast title — usa command via runCommand
                        String text = (String) map.getOrDefault("text", "");
                        bridge.runCommand("title @a title " + text);
                    }
                }
                case "sound" -> {
                    String uuid = (String) map.get("playerUuid");
                    if (uuid != null) bridge.sound(uuid, map);
                    else bridge.runCommand("playsound " + map.getOrDefault("sound", "minecraft:ambient.cave") + " master @a");
                }
                case "particle" -> bridge.particle(map);
                case "weather" -> bridge.worldWeather(map);
                case "time" -> bridge.worldTime(map);
                case "broadcast" -> bridge.broadcast(map);
                case "command" -> bridge.runCommand((String) map.getOrDefault("command", ""));
                case "spawnEntity" -> bridge.spawnEntity(map);
                case "spawnPlayerClone" -> bridge.spawnPlayerClone(map);
                case "teleport" -> {
                    String uuid = (String) map.get("playerUuid");
                    if (uuid != null) bridge.teleport(uuid, map);
                }
                case "lightning" -> {
                    String uuid = (String) map.get("playerUuid");
                    if (uuid != null) bridge.lightning(uuid);
                }
                case "freeze" -> {
                    String uuid = (String) map.get("playerUuid");
                    if (uuid != null) bridge.freeze(uuid);
                }
                case "unfreeze" -> {
                    String uuid = (String) map.get("playerUuid");
                    if (uuid != null) bridge.unfreeze(uuid);
                }
                case "voice" -> bridge.voicePlay(map);
                case "explosion" -> bridge.explosion(map);
                default -> { /* no-op */ }
            }
        } catch (Exception ignored) {}
    }

    private void apply(Cutscene2 c, Map<String, Object> b) {
        if (b.get("name") != null) c.setName((String) b.get("name"));
        if (b.get("description") != null) c.setDescription((String) b.get("description"));
        if (b.get("tracksJson") != null) c.setTracksJson(b.get("tracksJson").toString());
        if (b.get("durationMs") != null) c.setDurationMs(((Number) b.get("durationMs")).intValue());
        if (b.get("tags") != null) c.setTags((String) b.get("tags"));
    }
}
