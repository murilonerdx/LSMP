package br.com.murilo.liberthia.admin.engine.replay;

import br.com.murilo.liberthia.admin.engine.EngineEventBus;
import br.com.murilo.liberthia.admin.mod.ModBridgeClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.annotation.PostConstruct;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

@RestController
@RequestMapping("/api/replays")
public class ReplayController {

    private final ReplayRepository repo;
    private final ModBridgeClient bridge;
    private final EngineEventBus eventBus;
    private final ObjectMapper mapper = new ObjectMapper();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    /** Estado de gravação ativa (null = não gravando). */
    private final AtomicReference<RecordingSession> activeRecording = new AtomicReference<>();

    public ReplayController(ReplayRepository repo, ModBridgeClient bridge, EngineEventBus eventBus) {
        this.repo = repo; this.bridge = bridge; this.eventBus = eventBus;
    }

    @PostConstruct
    public void wireRecorder() {
        Consumer<JsonNode> handler = ev -> {
            RecordingSession sess = activeRecording.get();
            if (sess == null) return;
            long atMs = System.currentTimeMillis() - sess.startMs;
            ObjectNode wrap = mapper.createObjectNode();
            wrap.put("atMs", atMs);
            wrap.put("type", ev.path("type").asText("event"));
            wrap.set("payload", ev);
            sess.events.add(wrap);
        };
        eventBus.subscribe(handler);
    }

    private static class RecordingSession {
        long startMs;
        String name;
        List<JsonNode> events = Collections.synchronizedList(new ArrayList<>());
        RecordingSession(String name) { this.startMs = System.currentTimeMillis(); this.name = name; }
    }

    @GetMapping
    public Page<Replay> list(@RequestParam(defaultValue = "0") int page,
                             @RequestParam(defaultValue = "50") int size) {
        return repo.findAllByOrderByUpdatedAtDesc(PageRequest.of(page, Math.min(size, 200)));
    }

    @GetMapping("/{id}")
    public Replay get(@PathVariable Long id) { return repo.findById(id).orElseThrow(); }

    @PostMapping
    public Replay create(@RequestBody Map<String, Object> body) {
        Replay r = new Replay();
        apply(r, body);
        r.setRecordedAt(Instant.now());
        r.setUpdatedAt(Instant.now());
        return repo.save(r);
    }

    @PutMapping("/{id}")
    public Replay update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Replay r = repo.findById(id).orElseThrow();
        apply(r, body);
        r.setUpdatedAt(Instant.now());
        return repo.save(r);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        repo.deleteById(id);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    /** Reproduz o replay agendando cada evento. */
    @PostMapping("/{id}/play")
    public ResponseEntity<?> play(@PathVariable Long id) {
        Replay r = repo.findById(id).orElseThrow();
        try {
            JsonNode events = mapper.readTree(r.getEventsJson() == null ? "[]" : r.getEventsJson());
            int scheduled = 0;
            for (JsonNode ev : events) {
                long atMs = ev.path("atMs").asLong(0);
                String type = ev.path("type").asText("");
                JsonNode payload = ev.path("payload");
                scheduler.schedule(() -> dispatch(type, payload),
                        Math.max(0, atMs), TimeUnit.MILLISECONDS);
                scheduled++;
            }
            r.setPlays(r.getPlays() + 1);
            repo.save(r);
            return ResponseEntity.ok(Map.of("ok", true, "events", scheduled));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @SuppressWarnings("unchecked")
    private void dispatch(String type, JsonNode payload) {
        try {
            Map<String, Object> m = mapper.convertValue(payload, Map.class);
            switch (type) {
                case "command" -> bridge.runCommand((String) m.getOrDefault("command", ""));
                case "broadcast" -> bridge.broadcast(m);
                case "spawn" -> bridge.spawnEntity(m);
                case "title" -> {
                    String uuid = (String) m.get("playerUuid");
                    if (uuid != null) bridge.title(uuid, m);
                }
                case "sound" -> {
                    String uuid = (String) m.get("playerUuid");
                    if (uuid != null) bridge.sound(uuid, m);
                }
                case "particle" -> bridge.particle(m);
                default -> { /* unknown */ }
            }
        } catch (Exception ignored) {}
    }

    private void apply(Replay r, Map<String, Object> b) {
        if (b.get("name") != null) r.setName((String) b.get("name"));
        if (b.get("description") != null) r.setDescription((String) b.get("description"));
        if (b.get("eventsJson") != null) r.setEventsJson(b.get("eventsJson").toString());
        if (b.get("durationMs") != null) r.setDurationMs(((Number) b.get("durationMs")).intValue());
        if (b.get("eventCount") != null) r.setEventCount(((Number) b.get("eventCount")).intValue());
    }

    // ===== Auto-record via WS/SSE =====

    /** Inicia gravação automática — captura todos eventos do mod até /record/stop. */
    @PostMapping("/record/start")
    public ResponseEntity<?> recordStart(@RequestBody(required = false) Map<String, Object> body) {
        String name = body == null ? "Gravação automática" : (String) body.getOrDefault("name", "Gravação automática");
        RecordingSession current = activeRecording.get();
        if (current != null) {
            return ResponseEntity.status(409).body(Map.of(
                    "error", "already_recording",
                    "name", current.name,
                    "elapsedMs", System.currentTimeMillis() - current.startMs,
                    "events", current.events.size()
            ));
        }
        activeRecording.set(new RecordingSession(name));
        return ResponseEntity.ok(Map.of("ok", true, "recording", name, "startedAt", Instant.now().toString()));
    }

    /** Para gravação, salva como Replay no DB. */
    @PostMapping("/record/stop")
    public ResponseEntity<?> recordStop(@RequestBody(required = false) Map<String, Object> body) {
        RecordingSession sess = activeRecording.getAndSet(null);
        if (sess == null) return ResponseEntity.status(404).body(Map.of("error", "not_recording"));
        long durationMs = System.currentTimeMillis() - sess.startMs;
        String description = body == null ? "" : (String) body.getOrDefault("description", "");

        Replay r = new Replay();
        r.setName(sess.name);
        r.setDescription(description);
        try {
            r.setEventsJson(mapper.writeValueAsString(sess.events));
        } catch (Exception e) {
            r.setEventsJson("[]");
        }
        r.setEventCount(sess.events.size());
        r.setDurationMs((int) durationMs);
        r.setRecordedAt(Instant.now());
        r.setUpdatedAt(Instant.now());
        Replay saved = repo.save(r);
        return ResponseEntity.ok(Map.of("ok", true, "saved", saved));
    }

    @GetMapping("/record/status")
    public Map<String, Object> recordStatus() {
        RecordingSession sess = activeRecording.get();
        if (sess == null) return Map.of("recording", false);
        return Map.of(
                "recording", true,
                "name", sess.name,
                "elapsedMs", System.currentTimeMillis() - sess.startMs,
                "events", sess.events.size()
        );
    }
}
