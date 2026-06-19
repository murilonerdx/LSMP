package br.com.murilo.liberthia.admin.engine.choice;

import br.com.murilo.liberthia.admin.engine.EngineActions;
import br.com.murilo.liberthia.admin.engine.EngineEventBus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Repository
interface ChoiceDecisionRepository extends JpaRepository<ChoiceDecision, String> {
    @Query("SELECT d FROM ChoiceDecision d WHERE d.activeTarget IS NOT NULL")
    List<ChoiceDecision> findActive();
}

@Repository
interface ChoiceHistoryRepository extends JpaRepository<ChoiceHistory, Long> {
    @Query("SELECT h FROM ChoiceHistory h ORDER BY h.ts DESC")
    List<ChoiceHistory> findRecent(org.springframework.data.domain.Pageable p);
}

@Service
public class ChoiceEngine {

    private static final Logger LOG = LoggerFactory.getLogger(ChoiceEngine.class);
    private static final Pattern MARKER = Pattern.compile("^\\[LIB:([a-z0-9_]+):(\\d+)\\]$", Pattern.CASE_INSENSITIVE);

    private final ChoiceDecisionRepository decRepo;
    private final ChoiceHistoryRepository histRepo;
    private final EngineEventBus bus;
    private final EngineActions actions;
    private final ObjectMapper mapper = new ObjectMapper();

    public ChoiceEngine(ChoiceDecisionRepository decRepo,
                        ChoiceHistoryRepository histRepo,
                        EngineEventBus bus,
                        EngineActions actions) {
        this.decRepo = decRepo;
        this.histRepo = histRepo;
        this.bus = bus;
        this.actions = actions;
    }

    @PostConstruct
    public void init() {
        bus.subscribe(this::onModEvent);
        LOG.info("ChoiceEngine ✓ subscribed to chat events");
    }

    private void onModEvent(JsonNode event) {
        if (!"chat".equals(event.path("type").asText())) return;
        JsonNode data = event.get("data"); if (data == null) return;
        String msg = data.path("message").asText("");
        Matcher m = MARKER.matcher(msg.trim());
        if (!m.matches()) {
            // mensagem de chat normal — ignora silencioso
            if (msg.contains("[LIB:") || msg.contains("[LIBERTHIA:")) {
                LOG.warn("[Choice] marker-like message NÃO bateu regex: '{}'", msg);
            }
            return;
        }

        String decisionId = m.group(1);
        int optIdx;
        try { optIdx = Integer.parseInt(m.group(2)); } catch (Exception e) { return; }

        String playerUuid = data.path("uuid").asText("");
        String playerName = data.path("name").asText("");
        LOG.info("[Choice] click recebido: dec='{}' optIdx={} player={} ({})",
                decisionId, optIdx, playerName, playerUuid);

        ChoiceDecision dec = decRepo.findById(decisionId).orElse(null);
        if (dec == null) {
            LOG.warn("[Choice] decisão '{}' não existe no DB", decisionId);
            return;
        }
        if (dec.getActiveTarget() == null) {
            LOG.warn("[Choice] decisão '{}' não está ativa (já foi cancelada/consumida)", decisionId);
            return;
        }
        if (playerUuid.isBlank()) {
            LOG.warn("[Choice] uuid do clicker vazio — chat event sem uuid?");
            return;
        }

        // Target match
        if (!"all".equals(dec.getActiveTarget()) && !dec.getActiveTarget().equals(playerUuid)) {
            LOG.info("[Choice] player {} clicou mas decisão é direcionada para {}", playerUuid, dec.getActiveTarget());
            return;
        }

        // Já respondeu?
        Set<String> responses = parseResponses(dec.getActiveResponsesJson());
        if (responses.contains(playerUuid)) {
            LOG.info("[Choice] player {} já respondeu essa decisão", playerName);
            return;
        }

        // Parse options
        JsonNode opts;
        try { opts = mapper.readTree(dec.getOptionsJson() == null ? "[]" : dec.getOptionsJson()); }
        catch (Exception e) {
            LOG.warn("[Choice] optionsJson inválido em decisão '{}': {}", decisionId, e.getMessage());
            return;
        }
        if (optIdx < 0 || optIdx >= opts.size()) {
            LOG.warn("[Choice] optIdx {} fora dos limites (total={})", optIdx, opts.size());
            return;
        }
        JsonNode opt = opts.get(optIdx);

        // Apply consequence
        String action = opt.path("action").asText("");
        String payload = opt.path("payload").asText("");
        String resultMessage = opt.path("resultMessage").asText("");
        LOG.info("[Choice] aplicando action='{}' payload='{}' para {}", action, payload, playerName);
        try {
            switch (action) {
                case "title":
                    actions.title(playerUuid, resultMessage, "", 10, 80, 20);
                    break;
                case "sound":
                    actions.sound(playerUuid, payload, 1, 1);
                    if (resultMessage != null && !resultMessage.isBlank())
                        actions.title(playerUuid, resultMessage, "", 10, 60, 20);
                    break;
                case "command":
                    actions.runCommand(payload.replace("{player}", playerName));
                    if (resultMessage != null && !resultMessage.isBlank())
                        actions.title(playerUuid, resultMessage, "", 10, 60, 20);
                    break;
                case "effect":
                    actions.runCommand("effect give " + playerName + " " + payload);
                    if (resultMessage != null && !resultMessage.isBlank())
                        actions.title(playerUuid, resultMessage, "", 10, 60, 20);
                    break;
                default:
                    LOG.warn("[Choice] action desconhecida: '{}'", action);
            }
        } catch (Exception e) {
            LOG.warn("[Choice] aplicação da consequência falhou: {}", e.getMessage());
        }

        // Save history
        try {
            histRepo.save(new ChoiceHistory(
                    decisionId, dec.getTitle(), playerUuid, playerName, optIdx,
                    opt.path("text").asText("")));
        } catch (Exception e) {
            LOG.warn("[Choice] save history falhou: {}", e.getMessage());
        }

        // Update responses + auto-close if not multi
        responses.add(playerUuid);
        try {
            ArrayNode arr = mapper.createArrayNode();
            for (String r : responses) arr.add(r);
            dec.setActiveResponsesJson(arr.toString());
            if (!dec.isMultiResponse()) {
                dec.setActiveTarget(null);
                dec.setActivatedAt(null);
                dec.setActiveResponsesJson(null);
                LOG.info("[Choice] decisão '{}' fechada (single-response)", decisionId);
            }
            decRepo.save(dec);
        } catch (Exception e) {
            LOG.warn("[Choice] update responses falhou: {}", e.getMessage());
        }
    }

    private Set<String> parseResponses(String json) {
        Set<String> set = new HashSet<>();
        if (json == null || json.isBlank()) return set;
        try {
            JsonNode arr = mapper.readTree(json);
            if (arr.isArray()) for (JsonNode n : arr) set.add(n.asText());
        } catch (Exception ignored) {}
        return set;
    }

    public ChoiceDecision present(String decisionId, String target) {
        ChoiceDecision dec = decRepo.findById(decisionId).orElseThrow();
        // Cancela outras decisões ativas (1 ativa por vez globalmente)
        for (ChoiceDecision other : decRepo.findActive()) {
            if (!other.getId().equals(decisionId)) {
                other.setActiveTarget(null);
                other.setActivatedAt(null);
                other.setActiveResponsesJson(null);
                decRepo.save(other);
            }
        }
        dec.setActiveTarget(target);
        dec.setActiveResponsesJson("[]");
        dec.setActivatedAt(Instant.now());
        decRepo.save(dec);

        // Manda o tellraw clicável
        String selector = "all".equals(target) ? "@a" : findNameByUuid(target);
        LOG.info("[Choice] present dec='{}' target='{}' → selector='{}'", decisionId, target, selector);
        try {
            JsonNode opts = mapper.readTree(dec.getOptionsJson() == null ? "[]" : dec.getOptionsJson());
            StringBuilder sb = new StringBuilder("[");
            sb.append(jsonText("\n§5§l═══════════════════════════════\n"));
            sb.append(",").append(jsonText("§e§l❓ " + escape(dec.getTitle()) + "§r\n"));
            sb.append(",").append(jsonText("§7" + escape(dec.getQuestion()) + "\n\n"));
            for (int i = 0; i < opts.size(); i++) {
                JsonNode opt = opts.get(i);
                String color = opt.path("color").asText("§f");
                String text = opt.path("text").asText("...");
                sb.append(",{\"text\":\"   ").append(color).append("» §l[").append(escape(text)).append("]§r\\n\",")
                  .append("\"clickEvent\":{\"action\":\"run_command\",\"value\":\"/me [LIB:")
                  .append(dec.getId()).append(":").append(i).append("]\"},")
                  .append("\"hoverEvent\":{\"action\":\"show_text\",\"value\":{\"text\":\"§7Clique pra escolher: §f")
                  .append(escape(text)).append("\"}}}");
            }
            sb.append(",").append(jsonText("§5§l═══════════════════════════════\n"));
            sb.append("]");
            actions.runCommand("tellraw " + selector + " " + sb);
            actions.runCommand("playsound minecraft:block.amethyst_block.chime master " + selector);
            LOG.info("[Choice] tellraw + playsound enviados para '{}'", selector);
        } catch (Exception e) {
            LOG.warn("[Choice] present fail: {}", e.getMessage(), e);
        }
        return dec;
    }

    public ChoiceDecision cancelActive(String decisionId) {
        ChoiceDecision dec = decRepo.findById(decisionId).orElseThrow();
        dec.setActiveTarget(null);
        dec.setActivatedAt(null);
        dec.setActiveResponsesJson(null);
        return decRepo.save(dec);
    }

    private String findNameByUuid(String uuid) {
        EngineActions.PlayerInfo p = actions.findPlayer(uuid);
        return p == null ? "@p" : p.name();
    }

    private String jsonText(String t) {
        return "{\"text\":\"" + escape(t) + "\"}";
    }
    private String escape(String s) {
        return (s == null ? "" : s).replace("\\", "\\\\").replace("\"", "\\\"");
    }

    // --- API ---
    public List<ChoiceDecision> list() { return decRepo.findAll(); }
    public ChoiceDecision save(ChoiceDecision d) { return decRepo.save(d); }
    public void delete(String id) { decRepo.deleteById(id); }
    public List<ChoiceHistory> recentHistory(int limit) {
        return histRepo.findRecent(PageRequest.of(0, Math.min(200, Math.max(1, limit))));
    }
}

@RestController
@RequestMapping("/api/choice")
class ChoiceController {
    private final ChoiceEngine engine;
    ChoiceController(ChoiceEngine engine) { this.engine = engine; }

    @GetMapping("")
    public Map<String, Object> all() {
        return Map.of("decisions", engine.list(), "history", engine.recentHistory(50));
    }

    @PostMapping("/decisions")
    public ChoiceDecision save(@RequestBody ChoiceDecision d) {
        if (d.getId() == null || d.getId().isBlank())
            d.setId("dec_" + Long.toHexString(System.currentTimeMillis()));
        return engine.save(d);
    }

    @DeleteMapping("/decisions/{id}")
    public Map<String, Object> delete(@PathVariable String id) {
        engine.delete(id);
        return Map.of("ok", true);
    }

    @PostMapping("/decisions/{id}/present")
    public ChoiceDecision present(@PathVariable String id, @RequestBody Map<String, String> body) {
        return engine.present(id, body.getOrDefault("target", "all"));
    }

    @PostMapping("/decisions/{id}/cancel")
    public ChoiceDecision cancel(@PathVariable String id) { return engine.cancelActive(id); }
}
