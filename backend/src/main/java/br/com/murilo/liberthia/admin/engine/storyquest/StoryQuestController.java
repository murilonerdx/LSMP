package br.com.murilo.liberthia.admin.engine.storyquest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping("/api/story-quests")
public class StoryQuestController {

    private final StoryQuestRepository repo;
    private final ObjectMapper mapper = new ObjectMapper();
    private final String ftbquestsRoot;

    public StoryQuestController(StoryQuestRepository repo,
                                @Value("${ftbquests.root:}") String ftbquestsRoot) {
        this.repo = repo;
        this.ftbquestsRoot = ftbquestsRoot;
    }

    @GetMapping
    public Page<StoryQuest> list(@RequestParam(defaultValue = "0") int page,
                                 @RequestParam(defaultValue = "50") int size) {
        return repo.findAllByOrderByUpdatedAtDesc(PageRequest.of(page, Math.min(size, 200)));
    }

    @GetMapping("/{id}")
    public StoryQuest get(@PathVariable Long id) { return repo.findById(id).orElseThrow(); }

    @PostMapping
    public StoryQuest create(@RequestBody Map<String, Object> body) {
        StoryQuest q = new StoryQuest();
        apply(q, body);
        q.setCreatedAt(Instant.now());
        q.setUpdatedAt(Instant.now());
        return repo.save(q);
    }

    @PutMapping("/{id}")
    public StoryQuest update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        StoryQuest q = repo.findById(id).orElseThrow();
        apply(q, body);
        q.setUpdatedAt(Instant.now());
        return repo.save(q);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        repo.deleteById(id);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    /**
     * Deploy direto: gera JSON e ESCREVE no filesystem em ftbquests.root.
     * Útil quando o backend tem volume mount pro config/ftbquests/quests/chapters/ do server real.
     */
    @PostMapping("/{id}/deploy")
    public ResponseEntity<?> deploy(@PathVariable Long id) {
        if (ftbquestsRoot == null || ftbquestsRoot.isBlank())
            return ResponseEntity.status(503).body(Map.of(
                    "error", "ftbquests.root not configured",
                    "hint", "Set FTBQUESTS_ROOT env var pointing to <server>/config/ftbquests/quests/chapters"));
        try {
            Map<String, Object> chapter = exportFtb(id);
            String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(chapter);
            StoryQuest q = repo.findById(id).orElseThrow();
            String safeName = (q.getName() == null ? "quest" : q.getName())
                    .toLowerCase().replaceAll("[^a-z0-9_-]", "_");
            Path dir = Paths.get(ftbquestsRoot);
            Files.createDirectories(dir);
            Path file = dir.resolve("lib_" + q.getId() + "_" + safeName + ".json");
            Files.writeString(file, json);
            return ResponseEntity.ok(Map.of("ok", true, "deployedTo", file.toString(),
                    "note", "Reload no MC: /ftbquests reload (ou restart)"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /** Exporta como JSON do FTB Quests (formato config/ftbquests/quests/chapter.snbt simplificado). */
    @GetMapping("/{id}/export-ftb")
    public Map<String, Object> exportFtb(@PathVariable Long id) {
        StoryQuest q = repo.findById(id).orElseThrow();
        try {
            JsonNode nodes = mapper.readTree(q.getNodesJson() == null ? "[]" : q.getNodesJson());
            List<Map<String, Object>> quests = new ArrayList<>();
            for (JsonNode n : nodes) {
                Map<String, Object> qm = new LinkedHashMap<>();
                qm.put("id", n.path("id").asText());
                qm.put("title", n.path("title").asText("Objetivo"));
                qm.put("description", n.path("description").asText(""));
                qm.put("x", n.path("x").asDouble(0));
                qm.put("y", n.path("y").asDouble(0));
                qm.put("tasks", n.path("tasks"));
                qm.put("rewards", n.path("rewards"));
                quests.add(qm);
            }
            Map<String, Object> chapter = new LinkedHashMap<>();
            chapter.put("id", "lib_" + q.getId());
            chapter.put("title", q.getName());
            chapter.put("subtitle", q.getSummary());
            chapter.put("quests", quests);
            return chapter;
        } catch (Exception e) {
            return Map.of("error", e.getMessage());
        }
    }

    private void apply(StoryQuest q, Map<String, Object> b) {
        if (b.get("name") != null) q.setName((String) b.get("name"));
        if (b.get("summary") != null) q.setSummary((String) b.get("summary"));
        if (b.get("archetype") != null) q.setArchetype((String) b.get("archetype"));
        if (b.get("nodesJson") != null) q.setNodesJson(b.get("nodesJson").toString());
        if (b.get("connectionsJson") != null) q.setConnectionsJson(b.get("connectionsJson").toString());
        if (b.get("startNodeId") != null) q.setStartNodeId((String) b.get("startNodeId"));
        if (b.get("published") != null) q.setPublished(Boolean.parseBoolean(b.get("published").toString()));
    }
}
