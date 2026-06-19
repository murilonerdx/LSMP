package br.com.murilo.liberthia.admin.engine.forbidden;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/forbidden")
public class ForbiddenController {

    private final ForbiddenRuleRepository ruleRepo;
    private final ForbiddenEngine engine;

    public ForbiddenController(ForbiddenRuleRepository ruleRepo, ForbiddenEngine engine) {
        this.ruleRepo = ruleRepo;
        this.engine = engine;
    }

    @GetMapping("/rules")
    public Map<String, Object> listRules() {
        List<ForbiddenRule> rules = ruleRepo.findAll();
        return Map.of("rules", rules);
    }

    @PostMapping("/rules")
    public ForbiddenRule createOrUpdate(@RequestBody ForbiddenRule r) {
        if (r.getId() == null || r.getId().isBlank()) {
            r.setId("fw_" + Long.toHexString(System.currentTimeMillis()));
        }
        if (r.getMatchMode() == null) r.setMatchMode("contains");
        if (r.getTarget() == null) r.setTarget("speaker");
        if (r.getEmoji() == null) r.setEmoji("🩸");
        r.setUpdatedAt(Instant.now());
        return ruleRepo.save(r);
    }

    @DeleteMapping("/rules/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        ruleRepo.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/rules/{id}/toggle")
    public ForbiddenRule toggle(@PathVariable String id) {
        ForbiddenRule r = ruleRepo.findById(id).orElseThrow();
        r.setEnabled(!r.isEnabled());
        return ruleRepo.save(r);
    }

    @PostMapping("/rules/{id}/simulate")
    public Map<String, Object> simulate(@PathVariable String id, @RequestBody Map<String, String> body) {
        String playerUuid = body.getOrDefault("playerUuid", "");
        engine.simulate(id, playerUuid);
        return Map.of("ok", true);
    }

    @GetMapping("/invocations")
    public Map<String, Object> invocations(@RequestParam(defaultValue = "50") int limit) {
        return Map.of("invocations", engine.recentInvocations(limit));
    }
}
