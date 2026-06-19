package br.com.murilo.liberthia.admin.engine.forbidden;

import br.com.murilo.liberthia.admin.engine.EngineActions;
import br.com.murilo.liberthia.admin.engine.EngineEventBus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Engine de palavras proibidas. Roda em background no backend (não depende do
 * frontend estar aberto). Subscreve eventos 'chat' do mod via {@link EngineEventBus}.
 *
 * Quando um chat event chega:
 *  - Carrega regras do DB (cacheado por 5s pra evitar I/O em cada msg)
 *  - Verifica match contra cada regra ativada
 *  - Respeita cooldown por (uuid, ruleId)
 *  - Dispara consequências (sound, effect, particle, title, lightning, tellraw, spawn_mob, command)
 *  - Incrementa counter triggered + cria ForbiddenInvocation no DB
 */
@Service
public class ForbiddenEngine {

    private static final Logger LOG = LoggerFactory.getLogger(ForbiddenEngine.class);

    private final ForbiddenRuleRepository ruleRepo;
    private final ForbiddenInvocationRepository invRepo;
    private final EngineEventBus eventBus;
    private final EngineActions actions;
    private final ObjectMapper mapper = new ObjectMapper();

    private final Map<String, Long> cooldowns = new ConcurrentHashMap<>();

    public ForbiddenEngine(ForbiddenRuleRepository ruleRepo,
                           ForbiddenInvocationRepository invRepo,
                           EngineEventBus eventBus,
                           EngineActions actions) {
        this.ruleRepo = ruleRepo;
        this.invRepo = invRepo;
        this.eventBus = eventBus;
        this.actions = actions;
    }

    @PostConstruct
    public void init() {
        eventBus.subscribe(this::onModEvent);
        LOG.info("ForbiddenEngine ✓ subscribed to mod events");
    }

    private void onModEvent(JsonNode event) {
        if (!"chat".equals(event.path("type").asText())) return;
        JsonNode data = event.get("data");
        if (data == null) return;

        String msg = data.path("message").asText("");
        String speakerUuid = data.path("uuid").asText("");
        String speakerName = data.path("name").asText("");
        if (msg.isBlank() || speakerUuid.isBlank()) return;

        List<ForbiddenRule> active;
        try { active = ruleRepo.findByEnabledTrue(); }
        catch (Exception e) { LOG.warn("DB read fail: {}", e.getMessage()); return; }

        long now = System.currentTimeMillis();
        for (ForbiddenRule r : active) {
            if (!matches(msg, r)) continue;
            String cdKey = speakerUuid + "_" + r.getId();
            Long cdEnd = cooldowns.get(cdKey);
            if (cdEnd != null && cdEnd > now) continue;
            cooldowns.put(cdKey, now + r.getCooldownSec() * 1000L);

            try {
                r.setTriggered(r.getTriggered() + 1);
                ruleRepo.save(r);
                invRepo.save(new ForbiddenInvocation(speakerUuid, speakerName, r.getId(), r.getName(), r.getPattern()));
            } catch (Exception e) { LOG.warn("DB write fail: {}", e.getMessage()); }

            executeConsequences(r, speakerUuid, speakerName);
        }
    }

    private boolean matches(String msg, ForbiddenRule r) {
        String pattern = r.getPattern();
        if (pattern == null || pattern.isEmpty()) return false;
        String m = r.isCaseSensitive() ? msg : msg.toLowerCase();
        String p = r.isCaseSensitive() ? pattern : pattern.toLowerCase();
        switch (String.valueOf(r.getMatchMode())) {
            case "exact": return m.trim().equals(p);
            case "regex":
                try { return Pattern.compile(pattern, r.isCaseSensitive() ? 0 : Pattern.CASE_INSENSITIVE).matcher(msg).find(); }
                catch (Exception e) { return false; }
            case "contains":
            default: return m.contains(p);
        }
    }

    private void executeConsequences(ForbiddenRule r, String speakerUuid, String speakerName) {
        String json = r.getConsequencesJson();
        if (json == null || json.isBlank()) return;
        JsonNode list;
        try { list = mapper.readTree(json); }
        catch (Exception e) { LOG.warn("invalid consequencesJson for rule {}: {}", r.getId(), e.getMessage()); return; }
        if (!list.isArray()) return;

        EngineActions.PlayerInfo speaker = actions.findPlayer(speakerUuid);
        List<EngineActions.PlayerInfo> targets;
        switch (String.valueOf(r.getTarget())) {
            case "everyone":
                targets = actions.getPlayers();
                break;
            case "both":
                targets = new java.util.ArrayList<>(actions.getPlayers());
                break;
            case "speaker":
            default:
                targets = speaker == null ? List.of() : List.of(speaker);
        }

        for (JsonNode c : list) {
            String type = c.path("type").asText("");
            try {
                switch (type) {
                    case "sound":
                        for (var t : targets)
                            actions.sound(t.uuid(), c.path("sound").asText("minecraft:ambient.cave"),
                                    c.path("volume").asDouble(1), c.path("pitch").asDouble(1));
                        break;
                    case "effect":
                        for (var t : targets)
                            actions.effect(t.uuid(), c.path("effect").asText("minecraft:slowness"),
                                    c.path("durationSec").asInt(10) * 20, c.path("amplifier").asInt(0));
                        break;
                    case "particle":
                        for (var t : targets)
                            actions.particle(c.path("particle").asText("minecraft:smoke"),
                                    t.x(), t.y() + c.path("offsetY").asDouble(1), t.z(),
                                    c.path("count").asInt(30));
                        break;
                    case "title":
                        for (var t : targets)
                            actions.title(t.uuid(),
                                    c.path("title").asText(""), c.path("subtitle").asText(""),
                                    c.path("fadeIn").asInt(10), c.path("stay").asInt(50), c.path("fadeOut").asInt(10));
                        break;
                    case "lightning":
                        for (var t : targets) actions.lightning(t.uuid());
                        break;
                    case "tellraw":
                        String dest = "broadcast".equals(c.path("target").asText("speaker")) ? "@a" : (speaker == null ? "@s" : speaker.name());
                        actions.tellraw(dest, c.path("message").asText(""));
                        break;
                    case "spawn_mob":
                        int count = c.path("count").asInt(1);
                        for (var t : targets)
                            for (int k = 0; k < count; k++)
                                actions.spawnEntity(c.path("entity").asText("minecraft:zombie"),
                                        t.x() + c.path("offsetX").asDouble(0) + Math.random() * 2 - 1,
                                        t.y(),
                                        t.z() + c.path("offsetZ").asDouble(0) + Math.random() * 2 - 1,
                                        1, t.dimension().replace("minecraft:", ""));
                        break;
                    case "command":
                        String cmd = c.path("command").asText("");
                        if (!cmd.isBlank())
                            actions.runCommand(cmd.replace("@s", speakerName));
                        break;
                }
            } catch (Exception e) { LOG.debug("cons {} fail: {}", type, e.getMessage()); }
        }
    }

    /** Manually simulate a rule for a given player (used by /test endpoint). */
    public void simulate(String ruleId, String playerUuid) {
        ForbiddenRule r = ruleRepo.findById(ruleId).orElse(null);
        if (r == null) return;
        EngineActions.PlayerInfo p = actions.findPlayer(playerUuid);
        executeConsequences(r, playerUuid, p == null ? "?" : p.name());
    }

    public List<ForbiddenInvocation> recentInvocations(int limit) {
        return invRepo.findRecent(PageRequest.of(0, Math.min(200, Math.max(1, limit))));
    }
}
