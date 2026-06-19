package br.com.murilo.liberthia.admin.engine.cursed;

import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/cursed")
public class CursedController {

    private final CursedEngine engine;

    public CursedController(CursedEngine engine) { this.engine = engine; }

    @GetMapping("")
    public Map<String, Object> all() {
        return Map.of("curses", engine.listCurses(), "bindings", engine.listBindings());
    }

    @PostMapping("/curses")
    public Curse save(@RequestBody Curse c) {
        if (c.getId() == null || c.getId().isBlank())
            c.setId("curse_" + Long.toHexString(System.currentTimeMillis()));
        return engine.saveCurse(c);
    }

    @DeleteMapping("/curses/{id}")
    public Map<String, Object> delete(@PathVariable String id) {
        engine.deleteCurse(id);
        return Map.of("ok", true);
    }

    @PostMapping("/forge")
    public CurseBinding forge(@RequestBody Map<String, String> body) {
        return engine.forge(body.get("curseId"), body.get("playerUuid"));
    }

    @PostMapping("/unbind/{id}")
    public Map<String, Object> unbind(@PathVariable Long id) {
        engine.unbind(id);
        return Map.of("ok", true);
    }
}
