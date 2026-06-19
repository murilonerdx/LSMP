package br.com.murilo.liberthia.admin.engine.anchor;

import br.com.murilo.liberthia.admin.engine.EngineActions;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Repository
interface SaveAnchorRepository extends JpaRepository<SaveAnchor, String> {}

@Service
class AnchorEngine {

    private static final Logger LOG = LoggerFactory.getLogger(AnchorEngine.class);
    private final SaveAnchorRepository repo;
    private final EngineActions actions;
    private final ObjectMapper mapper = new ObjectMapper();

    AnchorEngine(SaveAnchorRepository repo, EngineActions actions) {
        this.repo = repo;
        this.actions = actions;
    }

    @Scheduled(fixedDelay = 1500, initialDelay = 11000)
    public void tick() {
        List<SaveAnchor> all;
        try { all = repo.findAll(); }
        catch (Exception e) { return; }

        var players = actions.getPlayers();
        for (SaveAnchor a : all) {
            if (!a.isEnabled()) continue;
            String dimMatch = "minecraft:" + a.getPosDim();
            Set<String> uses = parseUses(a);
            for (var p : players) {
                if (!p.dimension().equals(dimMatch)) continue;
                double dx = p.x() - a.getPosX(), dy = p.y() - a.getPosY(), dz = p.z() - a.getPosZ();
                double d = Math.sqrt(dx * dx + dy * dy + dz * dz);
                if (d > a.getRadius()) continue;
                if (uses.contains(p.uuid())) continue;
                activate(a, p, uses);
            }
        }
    }

    private void activate(SaveAnchor a, EngineActions.PlayerInfo p, Set<String> uses) {
        try {
            actions.runCommand("spawnpoint " + p.name() + " "
                    + Math.round(a.getPosX()) + " " + Math.round(a.getPosY()) + " " + Math.round(a.getPosZ()));
            actions.runCommand("effect give " + p.name() + " minecraft:instant_health 1 4");
            actions.title(p.uuid(), a.getEmoji() + " §6§l" + a.getName(), "§7§o⚓ ponto de descanso ativado", 15, 100, 30);
            actions.sound(p.uuid(), "minecraft:block.respawn_anchor.set_spawn", 1, 1);
            // Pulse particles
            for (int pulse = 0; pulse < 4; pulse++) {
                for (int i = 0; i < 16; i++) {
                    double ang = (i / 16.0) * Math.PI * 2;
                    double r = 2 + pulse * 0.5;
                    actions.particle("minecraft:soul_fire_flame",
                            a.getPosX() + Math.cos(ang) * r, a.getPosY() + 0.5, a.getPosZ() + Math.sin(ang) * r, 1);
                }
                try { Thread.sleep(200); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); break; }
            }
            uses.add(p.uuid());
            saveUses(a, uses);
        } catch (Exception e) { LOG.debug("anchor activate fail: {}", e.getMessage()); }
    }

    private Set<String> parseUses(SaveAnchor a) {
        Set<String> set = new HashSet<>();
        if (a.getUsesJson() == null) return set;
        try {
            JsonNode arr = mapper.readTree(a.getUsesJson());
            if (arr.isArray()) for (JsonNode n : arr) set.add(n.asText());
        } catch (Exception ignored) {}
        return set;
    }
    private void saveUses(SaveAnchor a, Set<String> set) {
        try {
            ArrayNode arr = mapper.createArrayNode();
            for (String s : set) arr.add(s);
            a.setUsesJson(arr.toString());
            repo.save(a);
        } catch (Exception ignored) {}
    }

    public List<SaveAnchor> list() { return repo.findAll(); }
    public SaveAnchor save(SaveAnchor a) { return repo.save(a); }
    public void delete(String id) { repo.deleteById(id); }
    public SaveAnchor toggle(String id) {
        SaveAnchor a = repo.findById(id).orElseThrow();
        a.setEnabled(!a.isEnabled());
        return repo.save(a);
    }
    public SaveAnchor resetUses(String id) {
        SaveAnchor a = repo.findById(id).orElseThrow();
        a.setUsesJson("[]");
        return repo.save(a);
    }
}

@RestController
@RequestMapping("/api/anchors")
class AnchorController {
    private final AnchorEngine engine;
    AnchorController(AnchorEngine engine) { this.engine = engine; }

    @GetMapping("")
    public Map<String, Object> list() { return Map.of("anchors", engine.list()); }

    @PostMapping("")
    public SaveAnchor save(@RequestBody SaveAnchor a) {
        if (a.getId() == null || a.getId().isBlank())
            a.setId("anchor_" + Long.toHexString(System.currentTimeMillis()));
        return engine.save(a);
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable String id) {
        engine.delete(id);
        return Map.of("ok", true);
    }

    @PostMapping("/{id}/toggle")
    public SaveAnchor toggle(@PathVariable String id) { return engine.toggle(id); }

    @PostMapping("/{id}/reset")
    public SaveAnchor resetUses(@PathVariable String id) { return engine.resetUses(id); }
}
