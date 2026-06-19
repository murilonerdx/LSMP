package br.com.murilo.liberthia.admin.engine.costume;

import br.com.murilo.liberthia.admin.mod.ModBridgeClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping("/api/costumes")
public class NpcCostumeController {

    private final NpcCostumeRepository repo;
    private final ModBridgeClient bridge;
    private final ObjectMapper mapper = new ObjectMapper();

    public NpcCostumeController(NpcCostumeRepository repo, ModBridgeClient bridge) {
        this.repo = repo; this.bridge = bridge;
    }

    @GetMapping
    public Page<NpcCostume> list(
            @RequestParam(required = false) String archetype,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        var pg = PageRequest.of(page, Math.min(size, 200));
        if (archetype != null && !archetype.isBlank())
            return repo.findByArchetypeOrderByUpdatedAtDesc(archetype, pg);
        return repo.findAllByOrderByUpdatedAtDesc(pg);
    }

    @GetMapping("/{id}")
    public NpcCostume get(@PathVariable Long id) { return repo.findById(id).orElseThrow(); }

    @PostMapping
    public NpcCostume create(@RequestBody Map<String, Object> body) {
        NpcCostume c = new NpcCostume();
        apply(c, body);
        c.setCreatedAt(Instant.now());
        c.setUpdatedAt(Instant.now());
        return repo.save(c);
    }

    @PutMapping("/{id}")
    public NpcCostume update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        NpcCostume c = repo.findById(id).orElseThrow();
        apply(c, body);
        c.setUpdatedAt(Instant.now());
        return repo.save(c);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        repo.deleteById(id);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    /** Aplica costume num player via comandos in-game. */
    @PostMapping("/{id}/apply")
    public ResponseEntity<?> apply(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        NpcCostume c = repo.findById(id).orElseThrow();
        String playerUuid = (String) body.get("playerUuid");
        String playerName = (String) body.get("playerName");
        if (playerUuid == null && playerName == null)
            return ResponseEntity.badRequest().body(Map.of("error", "playerUuid or playerName required"));

        try {
            String selector = playerUuid != null ? playerUuid : playerName;
            // 1) fakename — troca nome visível
            if (c.getFakeName() != null && !c.getFakeName().isBlank()) {
                bridge.runCommand("fakename set " + selector + " " + c.getFakeName());
            }
            // 2) CPM project — troca modelo (precisa do mod CPM e arquivo de modelo)
            if (c.getCpmProjectPath() != null && !c.getCpmProjectPath().isBlank()) {
                bridge.runCommand("cpm setskin " + selector + " " + c.getCpmProjectPath());
            }
            // 3) ArmoursWorkshop skin (se id válido)
            if (c.getArmourerSkinId() != null && !c.getArmourerSkinId().isBlank()) {
                bridge.runCommand("armourers wardrobe set " + selector + " " + c.getArmourerSkinId());
            }
            // 4) Pehkui scale
            if (c.getScale() != 1.0) {
                bridge.runCommand("scale set " + c.getScale() + " " + selector);
            }
            // 5) Equipment via /item replace
            if (c.getEquipmentJson() != null && !c.getEquipmentJson().isBlank()) {
                JsonNode eq = mapper.readTree(c.getEquipmentJson());
                Map<String, String> slotMap = Map.of(
                        "mainhand", "weapon.mainhand", "offhand", "weapon.offhand",
                        "helmet", "armor.head", "chest", "armor.chest",
                        "legs", "armor.legs", "boots", "armor.feet"
                );
                for (var entry : slotMap.entrySet()) {
                    JsonNode slot = eq.get(entry.getKey());
                    if (slot != null && !slot.isNull()) {
                        String itemId = slot.path("id").asText("");
                        if (!itemId.isEmpty()) {
                            bridge.runCommand(String.format("item replace entity %s %s with %s",
                                    selector, entry.getValue(), itemId));
                        }
                    }
                }
            }
            // 6) Effects ao vestir
            if (c.getEffectsJson() != null && !c.getEffectsJson().isBlank()) {
                JsonNode effects = mapper.readTree(c.getEffectsJson());
                for (JsonNode ef : effects) {
                    bridge.runCommand(String.format("effect give %s %s %d %d true",
                            selector, ef.path("effect").asText("minecraft:speed"),
                            ef.path("durationSec").asInt(60),
                            ef.path("amplifier").asInt(0)));
                }
            }
            c.setTimesUsed(c.getTimesUsed() + 1);
            repo.save(c);
            return ResponseEntity.ok(Map.of("ok", true, "applied", c.getName()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /** Restaura player ao normal (limpa fakename, scale, etc). */
    @PostMapping("/restore")
    public ResponseEntity<?> restore(@RequestBody Map<String, Object> body) {
        String playerName = (String) body.get("playerName");
        if (playerName == null) return ResponseEntity.badRequest().body(Map.of("error", "playerName required"));
        try {
            bridge.runCommand("fakename clear " + playerName);
            bridge.runCommand("scale set 1.0 " + playerName);
            bridge.runCommand("cpm setskin " + playerName + " default");
            return ResponseEntity.ok(Map.of("ok", true));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    private void apply(NpcCostume c, Map<String, Object> b) {
        if (b.get("name") != null) c.setName((String) b.get("name"));
        if (b.get("archetype") != null) c.setArchetype((String) b.get("archetype"));
        if (b.get("description") != null) c.setDescription((String) b.get("description"));
        if (b.get("fakeName") != null) c.setFakeName((String) b.get("fakeName"));
        if (b.get("chatTitle") != null) c.setChatTitle((String) b.get("chatTitle"));
        if (b.get("cpmProjectPath") != null) c.setCpmProjectPath((String) b.get("cpmProjectPath"));
        if (b.get("skinUrl") != null) c.setSkinUrl((String) b.get("skinUrl"));
        if (b.get("armourerSkinId") != null) c.setArmourerSkinId((String) b.get("armourerSkinId"));
        if (b.get("scale") != null) c.setScale(((Number) b.get("scale")).doubleValue());
        if (b.get("equipmentJson") != null) c.setEquipmentJson(b.get("equipmentJson").toString());
        if (b.get("effectsJson") != null) c.setEffectsJson(b.get("effectsJson").toString());
    }
}
