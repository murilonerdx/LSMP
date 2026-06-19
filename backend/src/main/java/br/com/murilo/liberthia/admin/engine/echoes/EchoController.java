package br.com.murilo.liberthia.admin.engine.echoes;

import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/memory-echoes")
public class EchoController {

    private final EchoEngine engine;
    public EchoController(EchoEngine engine) { this.engine = engine; }

    @GetMapping("")
    public Map<String, Object> list() {
        return Map.of("echoes", engine.list(), "active", engine.listActive());
    }

    @PostMapping("")
    public MemoryEcho save(@RequestBody MemoryEcho m) {
        if (m.getId() == null || m.getId().isBlank())
            m.setId("echo_" + Long.toHexString(System.currentTimeMillis()));
        return engine.save(m);
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable String id) {
        engine.delete(id);
        return Map.of("ok", true);
    }

    @PostMapping("/{id}/toggle")
    public MemoryEcho toggle(@PathVariable String id) { return engine.toggle(id); }

    @PostMapping("/purge")
    public Map<String, Object> purge() {
        engine.purge();
        return Map.of("ok", true);
    }
}
