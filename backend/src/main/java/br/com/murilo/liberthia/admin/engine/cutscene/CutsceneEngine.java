package br.com.murilo.liberthia.admin.engine.cutscene;

import br.com.murilo.liberthia.admin.engine.EngineActions;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Repository
interface CutsceneRepository extends JpaRepository<Cutscene, String> {}

@Service
public class CutsceneEngine {

    private static final Logger LOG = LoggerFactory.getLogger(CutsceneEngine.class);
    private final CutsceneRepository repo;
    private final EngineActions actions;
    private final ObjectMapper mapper = new ObjectMapper();
    private final Set<String> running = new HashSet<>();

    public CutsceneEngine(CutsceneRepository repo, EngineActions actions) {
        this.repo = repo;
        this.actions = actions;
    }

    @Scheduled(fixedDelay = 10000, initialDelay = 15000)
    public void tick() {
        Instant now = Instant.now();
        List<Cutscene> all;
        try { all = repo.findAll(); }
        catch (Exception e) { return; }

        for (Cutscene cs : all) {
            if (!cs.isEnabled()) continue;
            if (running.contains(cs.getId())) continue;
            if (!shouldRunNow(cs, now)) continue;
            triggerAsync(cs);
        }
    }

    private boolean shouldRunNow(Cutscene cs, Instant now) {
        String mode = cs.getScheduleMode() == null ? "once" : cs.getScheduleMode();
        switch (mode) {
            case "once":
                if (cs.getScheduledAt() == null) return false;
                if (cs.getRunCount() > 0) return false;
                return !cs.getScheduledAt().isAfter(now);
            case "daily":
                if (cs.getDailyHour() == null || cs.getDailyMinute() == null) return false;
                LocalDateTime ld = LocalDateTime.ofInstant(now, ZoneId.systemDefault());
                int h = ld.getHour(), m = ld.getMinute();
                if (h != cs.getDailyHour() || m != cs.getDailyMinute()) return false;
                Instant last = cs.getLastRunAt();
                // Evita rodar 2x na mesma janela de 1min
                return last == null || now.toEpochMilli() - last.toEpochMilli() > 90_000;
            case "interval":
                if (cs.getIntervalSec() == null || cs.getIntervalSec() < 5) return false;
                Instant lr = cs.getLastRunAt();
                return lr == null || now.toEpochMilli() - lr.toEpochMilli() >= cs.getIntervalSec() * 1000L;
            default:
                return false;
        }
    }

    @Async
    public void triggerAsync(Cutscene cs) {
        running.add(cs.getId());
        try {
            executeSteps(cs);
            cs.setLastRunAt(Instant.now());
            cs.setRunCount(cs.getRunCount() + 1);
            try { repo.save(cs); } catch (Exception ignored) {}
        } catch (Exception e) {
            LOG.warn("cutscene {} fail: {}", cs.getName(), e.getMessage());
        } finally {
            running.remove(cs.getId());
        }
    }

    public void executeSteps(Cutscene cs) {
        JsonNode steps;
        try { steps = mapper.readTree(cs.getStepsJson() == null ? "[]" : cs.getStepsJson()); }
        catch (Exception e) { return; }
        if (!steps.isArray()) return;

        List<EngineActions.PlayerInfo> targets = resolveTargets(cs.getTargetSelector());

        for (JsonNode step : steps) {
            String type = step.path("type").asText("");
            try {
                switch (type) {
                    case "title":
                        for (var t : targets) actions.title(t.uuid(),
                                step.path("title").asText(""), step.path("subtitle").asText(""),
                                step.path("fadeIn").asInt(10), step.path("stay").asInt(50), step.path("fadeOut").asInt(10));
                        break;
                    case "sound":
                        for (var t : targets) actions.sound(t.uuid(),
                                step.path("sound").asText("minecraft:ambient.cave"),
                                step.path("volume").asDouble(1), step.path("pitch").asDouble(1));
                        break;
                    case "particle":
                        for (var t : targets) actions.particle(step.path("particle").asText("minecraft:smoke"),
                                t.x(), t.y() + step.path("offsetY").asDouble(1), t.z(),
                                step.path("count").asInt(30));
                        break;
                    case "effect":
                        for (var t : targets) actions.effect(t.uuid(),
                                step.path("effect").asText("minecraft:slowness"),
                                step.path("durationSec").asInt(10) * 20,
                                step.path("amplifier").asInt(0));
                        break;
                    case "chat":
                        for (var t : targets) actions.tellraw(t.name(), step.path("message").asText(""));
                        break;
                    case "command":
                        String cmd = step.path("command").asText("");
                        if (!cmd.isBlank()) {
                            if (targets.isEmpty()) actions.runCommand(cmd);
                            else for (var t : targets) actions.runCommand(cmd.replace("@s", t.name()));
                        }
                        break;
                    case "tp":
                        for (var t : targets) actions.teleport(t.uuid(),
                                step.path("x").asDouble(t.x()),
                                step.path("y").asDouble(t.y()),
                                step.path("z").asDouble(t.z()),
                                step.path("dim").asText(t.dimension().replace("minecraft:", "")));
                        break;
                    case "lightning":
                        for (var t : targets) actions.lightning(t.uuid());
                        break;
                    case "spawn_mob":
                        int count = step.path("count").asInt(1);
                        for (var t : targets) for (int k = 0; k < count; k++)
                            actions.spawnEntity(step.path("entity").asText("minecraft:zombie"),
                                    t.x() + step.path("offsetX").asDouble(0) + Math.random() * 2 - 1,
                                    t.y() + step.path("offsetY").asDouble(0),
                                    t.z() + step.path("offsetZ").asDouble(0) + Math.random() * 2 - 1,
                                    1, t.dimension().replace("minecraft:", ""));
                        break;
                    case "wait":
                        Thread.sleep(step.path("durationSec").asInt(1) * 1000L);
                        break;
                }
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception e) {
                LOG.debug("cutscene step {} fail: {}", type, e.getMessage());
            }
        }
    }

    private List<EngineActions.PlayerInfo> resolveTargets(String sel) {
        if (sel == null) return List.of();
        if ("@a".equals(sel)) return actions.getPlayers();
        if ("@first".equals(sel)) {
            var all = actions.getPlayers();
            return all.isEmpty() ? List.of() : List.of(all.get(0));
        }
        var p = actions.findPlayer(sel);
        return p == null ? List.of() : List.of(p);
    }

    public List<Cutscene> list() { return repo.findAll(); }
    public Cutscene save(Cutscene c) { return repo.save(c); }
    public void delete(String id) { repo.deleteById(id); }
    public Cutscene toggle(String id) {
        Cutscene c = repo.findById(id).orElseThrow();
        c.setEnabled(!c.isEnabled());
        return repo.save(c);
    }
    public void runNow(String id) {
        repo.findById(id).ifPresent(this::triggerAsync);
    }
}
