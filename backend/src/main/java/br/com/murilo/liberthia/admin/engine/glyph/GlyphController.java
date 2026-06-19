package br.com.murilo.liberthia.admin.engine.glyph;

import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/glyphs")
public class GlyphController {

    private final GlyphEngine engine;
    public GlyphController(GlyphEngine engine) { this.engine = engine; }

    @GetMapping("")
    public Map<String, Object> list() { return Map.of("glyphs", engine.list()); }

    @PostMapping("")
    public Glyph save(@RequestBody Glyph g) {
        if (g.getId() == null || g.getId().isBlank())
            g.setId("glyph_" + Long.toHexString(System.currentTimeMillis()));
        return engine.save(g);
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable String id) {
        engine.delete(id);
        return Map.of("ok", true);
    }

    @PostMapping("/{id}/toggle")
    public Glyph toggle(@PathVariable String id) { return engine.toggle(id); }

    @PostMapping("/{id}/reset")
    public Map<String, Object> resetDiscoveries(@PathVariable String id) {
        engine.resetDiscoveries(id);
        return Map.of("ok", true);
    }
}
