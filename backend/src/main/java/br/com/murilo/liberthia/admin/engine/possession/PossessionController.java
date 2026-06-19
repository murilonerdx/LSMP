package br.com.murilo.liberthia.admin.engine.possession;

import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/possession")
public class PossessionController {

    private final PossessionEngine engine;

    public PossessionController(PossessionEngine engine) { this.engine = engine; }

    @GetMapping("/entities")
    public Map<String, Object> entities() {
        return Map.of("entities", engine.listEntities(), "active", engine.listActive());
    }

    @PostMapping("/entities")
    public PossessionEntity save(@RequestBody PossessionEntity e) {
        if (e.getId() == null || e.getId().isBlank())
            e.setId("ent_" + Long.toHexString(System.currentTimeMillis()));
        return engine.saveEntity(e);
    }

    @DeleteMapping("/entities/{id}")
    public Map<String, Object> delete(@PathVariable String id) {
        engine.deleteEntity(id);
        return Map.of("ok", true);
    }

    @PostMapping("/start")
    public PossessionActive start(@RequestBody Map<String, Object> body) {
        String entityId = String.valueOf(body.get("entityId"));
        String playerUuid = String.valueOf(body.get("playerUuid"));
        int duration = ((Number) body.getOrDefault("durationSec", 120)).intValue();
        return engine.start(entityId, playerUuid, duration);
    }

    @PostMapping("/end/{id}")
    public Map<String, Object> end(@PathVariable Long id) {
        engine.endById(id);
        return Map.of("ok", true);
    }
}
