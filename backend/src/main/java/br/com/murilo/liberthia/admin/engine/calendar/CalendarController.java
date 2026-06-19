package br.com.murilo.liberthia.admin.engine.calendar;

import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/calendar")
public class CalendarController {

    private final CalendarEngine engine;
    public CalendarController(CalendarEngine engine) { this.engine = engine; }

    @GetMapping("")
    public Map<String, Object> all() {
        CalendarConfig c = engine.getConfig();
        return Map.of(
                "config", c,
                "specialDays", engine.listDays(),
                "today", engine.currentDay(c)
        );
    }

    @PostMapping("/config")
    public CalendarConfig saveConfig(@RequestBody CalendarConfig c) { return engine.saveConfig(c); }

    @PostMapping("/days")
    public SpecialDay saveDay(@RequestBody SpecialDay s) {
        if (s.getId() == null || s.getId().isBlank())
            s.setId("sp_" + Long.toHexString(System.currentTimeMillis()));
        return engine.saveDay(s);
    }

    @DeleteMapping("/days/{id}")
    public Map<String, Object> delete(@PathVariable String id) {
        engine.deleteDay(id);
        return Map.of("ok", true);
    }

    @PostMapping("/days/{id}/fire")
    public Map<String, Object> fire(@PathVariable String id) {
        engine.fireNow(id);
        return Map.of("ok", true);
    }
}
