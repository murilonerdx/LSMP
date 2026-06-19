package br.com.murilo.liberthia.admin.engine.rift;

import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/rifts")
public class RiftController {

    private final RiftEngine engine;
    public RiftController(RiftEngine engine) { this.engine = engine; }

    @GetMapping("")
    public Map<String, Object> list() { return Map.of("rifts", engine.list()); }

    @PostMapping("")
    public Rift save(@RequestBody Rift r) {
        if (r.getId() == null || r.getId().isBlank())
            r.setId("rift_" + Long.toHexString(System.currentTimeMillis()));
        return engine.save(r);
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable String id) {
        engine.delete(id);
        return Map.of("ok", true);
    }

    @PostMapping("/{id}/toggle")
    public Rift toggle(@PathVariable String id) { return engine.toggle(id); }
}
