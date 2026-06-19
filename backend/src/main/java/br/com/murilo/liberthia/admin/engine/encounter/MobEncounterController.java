package br.com.murilo.liberthia.admin.engine.encounter;

import br.com.murilo.liberthia.admin.mod.ModBridgeClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/encounters")
public class MobEncounterController {

    private final MobEncounterRepository repo;
    private final ModBridgeClient bridge;
    private final ObjectMapper mapper = new ObjectMapper();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    public MobEncounterController(MobEncounterRepository repo, ModBridgeClient bridge) {
        this.repo = repo; this.bridge = bridge;
    }

    @GetMapping
    public Page<MobEncounter> list(@RequestParam(defaultValue = "0") int page,
                                   @RequestParam(defaultValue = "50") int size) {
        return repo.findAllByOrderByUpdatedAtDesc(PageRequest.of(page, Math.min(size, 200)));
    }

    @GetMapping("/{id}")
    public MobEncounter get(@PathVariable Long id) { return repo.findById(id).orElseThrow(); }

    @PostMapping
    public MobEncounter create(@RequestBody Map<String, Object> body) {
        MobEncounter e = new MobEncounter();
        apply(e, body);
        e.setCreatedAt(Instant.now());
        e.setUpdatedAt(Instant.now());
        return repo.save(e);
    }

    @PutMapping("/{id}")
    public MobEncounter update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        MobEncounter e = repo.findById(id).orElseThrow();
        apply(e, body);
        e.setUpdatedAt(Instant.now());
        return repo.save(e);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        repo.deleteById(id);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    /** Inicia o encontro: agenda spawn de cada wave nos delays definidos. */
    @PostMapping("/{id}/start")
    public ResponseEntity<?> start(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> body) {
        MobEncounter e = repo.findById(id).orElseThrow();
        double cx = e.getCenterX(), cy = e.getCenterY(), cz = e.getCenterZ();
        // Override via body
        if (body != null) {
            if (body.get("x") != null) cx = ((Number) body.get("x")).doubleValue();
            if (body.get("y") != null) cy = ((Number) body.get("y")).doubleValue();
            if (body.get("z") != null) cz = ((Number) body.get("z")).doubleValue();
        }

        try {
            JsonNode waves = mapper.readTree(e.getWavesJson() == null ? "[]" : e.getWavesJson());
            long cumulativeMs = 0;
            int waveNum = 0;
            for (JsonNode wave : waves) {
                cumulativeMs += wave.path("delayMs").asLong(0);
                long when = cumulativeMs;
                waveNum++;
                int wn = waveNum;
                double fcx = cx, fcy = cy, fcz = cz;
                scheduler.schedule(() -> spawnWave(wave, fcx, fcy, fcz, wn, e.getDimension()),
                        Math.max(0, when), TimeUnit.MILLISECONDS);
            }
            e.setTimesRun(e.getTimesRun() + 1);
            repo.save(e);
            return ResponseEntity.ok(Map.of("ok", true, "waves", waveNum));
        } catch (Exception ex) {
            return ResponseEntity.status(500).body(Map.of("error", ex.getMessage()));
        }
    }

    private void spawnWave(JsonNode wave, double cx, double cy, double cz, int waveNum, String dim) {
        try {
            String announce = wave.path("announce").asText("");
            if (!announce.isEmpty()) {
                bridge.broadcast(Map.of("message", "§c§l⚔ Wave " + waveNum + ": §f" + announce, "color", "red"));
            }
            // Som dramático no início da wave
            bridge.runCommand("playsound minecraft:event.raid.horn master @a");

            for (JsonNode mob : wave.path("mobs")) {
                String id = mob.path("id").asText("minecraft:zombie");
                int count = mob.path("count").asInt(1);
                double dx = mob.path("dx").asDouble(0), dy = mob.path("dy").asDouble(0), dz = mob.path("dz").asDouble(0);
                String tag = mob.path("tag").asText("encounter_w" + waveNum);
                String nbt = mob.path("nbt").asText("");

                for (int i = 0; i < count; i++) {
                    Map<String, Object> spawn = new HashMap<>();
                    spawn.put("entity", id);
                    spawn.put("x", cx + dx + (Math.random() - 0.5) * 2);
                    spawn.put("y", cy + dy);
                    spawn.put("z", cz + dz + (Math.random() - 0.5) * 2);
                    spawn.put("dimension", dim == null ? "minecraft:overworld" : dim);
                    spawn.put("tag", tag);
                    if (!nbt.isEmpty()) spawn.put("nbt", nbt);
                    try { bridge.spawnEntity(spawn); } catch (Exception ignored) {}
                }
            }
        } catch (Exception ignored) {}
    }

    /** Mata todos mobs do encontro (com tag encounter_*). */
    @PostMapping("/{id}/clear")
    public ResponseEntity<?> clear(@PathVariable Long id) {
        try {
            bridge.killByTag(Map.of("tag", "encounter_w1"));
            bridge.runCommand("kill @e[tag=encounter_w1]");
            for (int i = 1; i <= 10; i++) {
                bridge.runCommand("kill @e[tag=encounter_w" + i + "]");
            }
            return ResponseEntity.ok(Map.of("ok", true));
        } catch (Exception ex) {
            return ResponseEntity.status(500).body(Map.of("error", ex.getMessage()));
        }
    }

    private void apply(MobEncounter e, Map<String, Object> b) {
        if (b.get("name") != null) e.setName((String) b.get("name"));
        if (b.get("description") != null) e.setDescription((String) b.get("description"));
        if (b.get("wavesJson") != null) e.setWavesJson(b.get("wavesJson").toString());
        if (b.get("dimension") != null) e.setDimension((String) b.get("dimension"));
        if (b.get("centerX") != null) e.setCenterX(((Number) b.get("centerX")).doubleValue());
        if (b.get("centerY") != null) e.setCenterY(((Number) b.get("centerY")).doubleValue());
        if (b.get("centerZ") != null) e.setCenterZ(((Number) b.get("centerZ")).doubleValue());
        if (b.get("totalDifficulty") != null) e.setTotalDifficulty(((Number) b.get("totalDifficulty")).intValue());
    }
}
