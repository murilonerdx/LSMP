package br.com.murilo.liberthia.admin.engine.director;

import br.com.murilo.liberthia.admin.mod.ModBridgeClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Mass Player Director — controla N players ao mesmo tempo pra cutscenes coletivas.
 * Cada endpoint recebe lista de UUIDs + payload da ação.
 */
@RestController
@RequestMapping("/api/director")
public class DirectorController {

    private final ModBridgeClient bridge;
    public DirectorController(ModBridgeClient bridge) { this.bridge = bridge; }

    @PostMapping("/teleport")
    public Map<String, Object> tp(@RequestBody Map<String, Object> body) {
        return forEach(body, "teleport", uuid -> bridge.teleport(uuid, body));
    }

    @PostMapping("/title")
    public Map<String, Object> title(@RequestBody Map<String, Object> body) {
        return forEach(body, "title", uuid -> bridge.title(uuid, body));
    }

    @PostMapping("/sound")
    public Map<String, Object> sound(@RequestBody Map<String, Object> body) {
        return forEach(body, "sound", uuid -> bridge.sound(uuid, body));
    }

    @PostMapping("/effect")
    public Map<String, Object> effect(@RequestBody Map<String, Object> body) {
        return forEach(body, "effect", uuid -> bridge.applyEffect(uuid, body));
    }

    @PostMapping("/lightning")
    public Map<String, Object> lightning(@RequestBody Map<String, Object> body) {
        return forEach(body, "lightning", uuid -> bridge.lightning(uuid));
    }

    @PostMapping("/freeze")
    public Map<String, Object> freeze(@RequestBody Map<String, Object> body) {
        return forEach(body, "freeze", uuid -> bridge.freeze(uuid));
    }

    @PostMapping("/unfreeze")
    public Map<String, Object> unfreeze(@RequestBody Map<String, Object> body) {
        return forEach(body, "unfreeze", uuid -> bridge.unfreeze(uuid));
    }

    @PostMapping("/heal")
    public Map<String, Object> heal(@RequestBody Map<String, Object> body) {
        return forEach(body, "heal", uuid -> bridge.heal(uuid));
    }

    @PostMapping("/feed")
    public Map<String, Object> feed(@RequestBody Map<String, Object> body) {
        return forEach(body, "feed", uuid -> bridge.feed(uuid));
    }

    @PostMapping("/give")
    public Map<String, Object> give(@RequestBody Map<String, Object> body) {
        return forEach(body, "give", uuid -> bridge.giveItem(uuid, body));
    }

    /** Fade-to-black com title preto enorme + DARKNESS effect. */
    @PostMapping("/fade-black")
    public Map<String, Object> fadeBlack(@RequestBody Map<String, Object> body) {
        int duration = ((Number) body.getOrDefault("durationSec", 5)).intValue();
        return forEach(body, "fadeBlack", uuid -> {
            bridge.applyEffect(uuid, Map.of("effect", "minecraft:darkness", "duration", duration, "amplifier", 0));
            bridge.applyEffect(uuid, Map.of("effect", "minecraft:blindness", "duration", duration, "amplifier", 0));
            bridge.title(uuid, Map.of("title", "§0█████████████", "fadeIn", 5, "stay", duration * 20 - 10, "fadeOut", 5));
        });
    }

    /** Helper que aplica uma action em cada uuid da lista. */
    @SuppressWarnings("unchecked")
    private Map<String, Object> forEach(Map<String, Object> body, String op, java.util.function.Consumer<String> action) {
        List<String> uuids = (List<String>) body.getOrDefault("playerUuids", List.of());
        int ok = 0, fail = 0;
        List<String> errors = new ArrayList<>();
        for (String u : uuids) {
            try { action.accept(u); ok++; }
            catch (Exception e) { fail++; errors.add(u + ": " + e.getMessage()); }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("op", op);
        result.put("ok", ok);
        result.put("fail", fail);
        result.put("total", uuids.size());
        if (!errors.isEmpty()) result.put("errors", errors);
        return result;
    }
}
