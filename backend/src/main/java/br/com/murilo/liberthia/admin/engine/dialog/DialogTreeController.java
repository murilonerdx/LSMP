package br.com.murilo.liberthia.admin.engine.dialog;

import br.com.murilo.liberthia.admin.mod.ModBridgeClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/dialog-trees")
public class DialogTreeController {

    private final DialogTreeRepository repo;
    private final ModBridgeClient bridge;
    private final ObjectMapper mapper = new ObjectMapper();

    public DialogTreeController(DialogTreeRepository repo, ModBridgeClient bridge) {
        this.repo = repo; this.bridge = bridge;
    }

    @GetMapping
    public Page<DialogTree> list(@RequestParam(defaultValue = "0") int page,
                                 @RequestParam(defaultValue = "50") int size) {
        return repo.findAllByOrderByUpdatedAtDesc(PageRequest.of(page, Math.min(size, 200)));
    }

    @GetMapping("/{id}")
    public DialogTree get(@PathVariable Long id) { return repo.findById(id).orElseThrow(); }

    @PostMapping
    public DialogTree create(@RequestBody Map<String, Object> body) {
        DialogTree d = new DialogTree();
        apply(d, body);
        d.setUpdatedAt(Instant.now());
        return repo.save(d);
    }

    @PutMapping("/{id}")
    public DialogTree update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        DialogTree d = repo.findById(id).orElseThrow();
        apply(d, body);
        d.setUpdatedAt(Instant.now());
        return repo.save(d);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        repo.deleteById(id);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    /** Triggera o nó pra um player — manda title/sound representando o diálogo. */
    @PostMapping("/{id}/trigger")
    public ResponseEntity<?> trigger(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        DialogTree d = repo.findById(id).orElseThrow();
        String nodeId = (String) body.getOrDefault("nodeId", d.getRootNodeId());
        String playerUuid = (String) body.get("playerUuid");
        if (playerUuid == null) return ResponseEntity.badRequest().body(Map.of("error", "playerUuid required"));

        try {
            JsonNode nodes = mapper.readTree(d.getNodesJson() == null ? "{}" : d.getNodesJson());
            JsonNode node = nodes.get(nodeId);
            if (node == null || node.isMissingNode()) return ResponseEntity.notFound().build();

            String text = node.path("text").asText("");
            String style = d.getBalloonStyle();
            // Manda como title (subtítulo amarelo igual filme) ou como /broadcast (Comics Bubble se mod escutar)
            if ("title".equals(style)) {
                bridge.title(playerUuid, Map.of("title", "", "subtitle", text, "fadeIn", 5, "stay", 80, "fadeOut", 10));
            } else {
                // Manda como broadcast pessoal (mod pode escutar tag e mostrar balão)
                bridge.runCommand("tellraw " + playerUuid + " {\"text\":\"" + text.replace("\"", "\\\"") + "\"}");
            }
            // som suave de fala
            bridge.sound(playerUuid, Map.of("sound", "minecraft:entity.villager.ambient", "volume", 0.7, "pitch", 1.0));
            return ResponseEntity.ok(Map.of("ok", true, "node", nodeId));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    private void apply(DialogTree d, Map<String, Object> b) {
        if (b.get("name") != null) d.setName((String) b.get("name"));
        if (b.get("npcTag") != null) d.setNpcTag((String) b.get("npcTag"));
        if (b.get("rootNodeId") != null) d.setRootNodeId((String) b.get("rootNodeId"));
        if (b.get("nodesJson") != null) d.setNodesJson(b.get("nodesJson").toString());
        if (b.get("balloonStyle") != null) d.setBalloonStyle((String) b.get("balloonStyle"));
        if (b.get("active") != null) d.setActive(Boolean.parseBoolean(b.get("active").toString()));
    }
}
