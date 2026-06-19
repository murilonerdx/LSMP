package br.com.murilo.liberthia.admin.engine.cutscene;

import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/cutscenes")
public class CutsceneController {

    private final CutsceneEngine engine;
    public CutsceneController(CutsceneEngine engine) { this.engine = engine; }

    @GetMapping("")
    public Map<String, Object> list() { return Map.of("cutscenes", engine.list()); }

    @PostMapping("")
    public Cutscene save(@RequestBody Cutscene c) {
        if (c.getId() == null || c.getId().isBlank())
            c.setId("cs_" + Long.toHexString(System.currentTimeMillis()));
        if (c.getScheduleMode() == null) c.setScheduleMode("once");
        return engine.save(c);
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable String id) {
        engine.delete(id);
        return Map.of("ok", true);
    }

    @PostMapping("/{id}/toggle")
    public Cutscene toggle(@PathVariable String id) { return engine.toggle(id); }

    @PostMapping("/{id}/run-now")
    public Map<String, Object> runNow(@PathVariable String id) {
        engine.runNow(id);
        return Map.of("ok", true);
    }
}
