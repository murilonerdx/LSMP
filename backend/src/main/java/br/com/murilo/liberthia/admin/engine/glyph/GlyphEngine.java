package br.com.murilo.liberthia.admin.engine.glyph;

import br.com.murilo.liberthia.admin.engine.EngineActions;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Repository
interface GlyphRepository extends JpaRepository<Glyph, String> {}

@Service
public class GlyphEngine {

    private static final Logger LOG = LoggerFactory.getLogger(GlyphEngine.class);
    private final GlyphRepository repo;
    private final EngineActions actions;
    private final ObjectMapper mapper = new ObjectMapper();
    private final Map<String, Long> lastHint = new ConcurrentHashMap<>();
    private final Map<String, Long> debounce = new ConcurrentHashMap<>();

    public GlyphEngine(GlyphRepository repo, EngineActions actions) {
        this.repo = repo;
        this.actions = actions;
    }

    @Scheduled(fixedDelay = 1500, initialDelay = 8000)
    public void tick() {
        long now = System.currentTimeMillis();
        List<Glyph> all;
        try { all = repo.findAll(); }
        catch (Exception e) { return; }

        var players = actions.getPlayers();
        for (Glyph g : all) {
            if (!g.isEnabled()) continue;
            String dimMatch = "minecraft:" + g.getPosDim();

            // Hint particles
            if (now - lastHint.getOrDefault(g.getId(), 0L) >= g.getHintEvery() * 1000L) {
                lastHint.put(g.getId(), now);
                actions.particle(g.getHintParticle(), g.getPosX(), g.getPosY(), g.getPosZ(), g.getHintCount());
            }

            Set<String> discovered = getDiscoveredSet(g);
            for (var p : players) {
                if (!p.dimension().equals(dimMatch)) continue;
                if (discovered.contains(p.uuid())) continue;
                double dx = p.x() - g.getPosX(), dy = p.y() - g.getPosY(), dz = p.z() - g.getPosZ();
                double d = Math.sqrt(dx * dx + dy * dy + dz * dz);
                if (d > g.getRadius()) continue;

                String key = p.uuid() + "_" + g.getId();
                if (debounce.getOrDefault(key, 0L) > now - 5000) continue;
                debounce.put(key, now);

                discover(g, p);
                discovered.add(p.uuid());
                saveDiscovered(g, discovered);
            }
        }
    }

    private void discover(Glyph g, EngineActions.PlayerInfo p) {
        actions.title(p.uuid(), g.getSymbol(), "§e§l— GLIFO DESCOBERTO —", 10, 70, 20);
        actions.tellraw(p.name(), "§e§l✦ Você descobriu: §r" + g.getName());
        actions.tellraw(p.name(), g.getLoreFragment() == null ? "" : g.getLoreFragment());
        actions.sound(p.uuid(), g.getDiscoverSound(), 1, 1);
        actions.particle(g.getDiscoverParticle(), p.x(), p.y() + 1, p.z(), 80);
        actions.particle(g.getDiscoverParticle(), g.getPosX(), g.getPosY(), g.getPosZ(), 120);

        try {
            JsonNode arr = mapper.readTree(g.getRewardsJson() == null ? "[]" : g.getRewardsJson());
            if (arr.isArray()) for (JsonNode r : arr) applyReward(r, p);
        } catch (Exception ignored) {}
    }

    private void applyReward(JsonNode r, EngineActions.PlayerInfo p) {
        switch (r.path("type").asText()) {
            case "item":
                actions.giveItem(p.uuid(), r.path("itemId").asText("minecraft:diamond"), r.path("count").asInt(1));
                break;
            case "effect":
                actions.effect(p.uuid(),
                        r.path("effect").asText("minecraft:luck"),
                        r.path("durationSec").asInt(600) * 20,
                        r.path("amplifier").asInt(0));
                break;
            case "title":
                actions.title(p.uuid(),
                        r.path("title").asText(""), r.path("subtitle").asText(""),
                        r.path("fadeIn").asInt(10), r.path("stay").asInt(60), r.path("fadeOut").asInt(20));
                break;
            case "command":
                String cmd = r.path("command").asText("");
                if (!cmd.isBlank()) actions.runCommand(cmd.replace("@s", p.name()));
                break;
        }
    }

    private Set<String> getDiscoveredSet(Glyph g) {
        Set<String> set = new HashSet<>();
        try {
            JsonNode arr = mapper.readTree(g.getDiscoveredByJson() == null ? "[]" : g.getDiscoveredByJson());
            if (arr.isArray()) for (JsonNode x : arr) set.add(x.asText());
        } catch (Exception ignored) {}
        return set;
    }

    private void saveDiscovered(Glyph g, Set<String> set) {
        try {
            ArrayNode arr = mapper.createArrayNode();
            for (String s : set) arr.add(s);
            g.setDiscoveredByJson(arr.toString());
            repo.save(g);
        } catch (Exception ignored) {}
    }

    public List<Glyph> list() { return repo.findAll(); }
    public Glyph save(Glyph g) { return repo.save(g); }
    public void delete(String id) { repo.deleteById(id); }
    public Glyph toggle(String id) {
        Glyph g = repo.findById(id).orElseThrow();
        g.setEnabled(!g.isEnabled());
        return repo.save(g);
    }
    public void resetDiscoveries(String id) {
        Glyph g = repo.findById(id).orElseThrow();
        g.setDiscoveredByJson("[]");
        repo.save(g);
    }
}
