package br.com.murilo.liberthia.admin.engine.madness;

import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/madness")
public class MadnessController {

    private final MadnessEngine engine;

    public MadnessController(MadnessEngine engine) {
        this.engine = engine;
    }

    @GetMapping("/state")
    public Map<String, Object> state() {
        List<MadnessState> all = engine.getAllState();
        return Map.of("state", all, "config", engine.getConfig());
    }

    @GetMapping("/config")
    public MadnessConfig config() { return engine.getConfig(); }

    @PostMapping("/config")
    public MadnessConfig saveConfig(@RequestBody MadnessConfig c) { return engine.saveConfig(c); }

    @PostMapping("/state/{uuid}")
    public Map<String, Object> setSanity(@PathVariable String uuid, @RequestBody Map<String, Double> body) {
        Double v = body.get("sanity");
        if (v != null) engine.setSanity(uuid, v);
        return Map.of("ok", true);
    }

    @PostMapping("/state/{uuid}/delta")
    public Map<String, Object> delta(@PathVariable String uuid, @RequestBody Map<String, Double> body) {
        engine.deltaSanity(uuid, body.getOrDefault("delta", 0.0));
        return Map.of("ok", true);
    }

    @PostMapping("/reset")
    public Map<String, Object> reset(@RequestBody Map<String, Double> body) {
        engine.resetAll(body.getOrDefault("sanity", 100.0));
        return Map.of("ok", true);
    }

    @PostMapping("/manifest/{uuid}")
    public Map<String, Object> manifest(@PathVariable String uuid) {
        engine.manifest(uuid);
        return Map.of("ok", true);
    }

    @PostMapping("/toggle")
    public MadnessConfig toggle() {
        MadnessConfig c = engine.getConfig();
        c.setEnabled(!c.isEnabled());
        return engine.saveConfig(c);
    }
}
