package br.com.murilo.liberthia.admin.engine.boxes;

import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/boxes")
public class BoxController {

    private final BoxEngine engine;
    public BoxController(BoxEngine engine) { this.engine = engine; }

    @GetMapping("")
    public Map<String, Object> list() { return Map.of("boxes", engine.list()); }

    @PostMapping("")
    public TimeBox save(@RequestBody TimeBox b) {
        if (b.getId() == null || b.getId().isBlank())
            b.setId("box_" + Long.toHexString(System.currentTimeMillis()));
        return engine.save(b);
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable String id) {
        engine.delete(id);
        return Map.of("ok", true);
    }

    @PostMapping("/{id}/force-open")
    public TimeBox forceOpen(@PathVariable String id) { return engine.forceOpen(id); }

    @PostMapping("/{id}/reset")
    public TimeBox reset(@PathVariable String id) { return engine.resetDelivered(id); }
}
