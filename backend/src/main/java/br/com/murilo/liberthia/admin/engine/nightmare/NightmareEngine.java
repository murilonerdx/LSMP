package br.com.murilo.liberthia.admin.engine.nightmare;

import br.com.murilo.liberthia.admin.engine.EngineActions;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Repository
interface NightmareSequenceRepository extends JpaRepository<NightmareSequence, String> {}

/**
 * Nightmare Sequence engine — roda no backend (não depende da página aberta).
 * Cada execução é uma thread async que processa steps sequencialmente.
 */
@Service
public class NightmareEngine {

    private static final Logger LOG = LoggerFactory.getLogger(NightmareEngine.class);

    private final NightmareSequenceRepository repo;
    private final EngineActions actions;
    private final ObjectMapper mapper = new ObjectMapper();
    private final Map<String, RunStatus> activeRuns = new ConcurrentHashMap<>();
    private final Set<String> cancelled = ConcurrentHashMap.newKeySet();

    public NightmareEngine(NightmareSequenceRepository repo, EngineActions actions) {
        this.repo = repo;
        this.actions = actions;
    }

    public static class RunStatus {
        public String runId;
        public String seqId;
        public String seqName;
        public String playerUuid;
        public String playerName;
        public long startedAt;
        public int currentStep;
        public int totalSteps;
        public boolean finished;
        public boolean cancelled;
        public String currentStepType;
    }

    @Async
    public void runAsync(String seqId, String playerUuid) {
        NightmareSequence seq = repo.findById(seqId).orElse(null);
        if (seq == null) return;
        EngineActions.PlayerInfo p = actions.findPlayer(playerUuid);
        if (p == null) {
            LOG.warn("nightmare: player {} offline", playerUuid);
            return;
        }
        String runId = "run_" + Long.toHexString(System.currentTimeMillis());
        RunStatus status = new RunStatus();
        status.runId = runId;
        status.seqId = seqId;
        status.seqName = seq.getName();
        status.playerUuid = playerUuid;
        status.playerName = p.name();
        status.startedAt = System.currentTimeMillis();
        status.currentStep = 0;
        activeRuns.put(runId, status);

        JsonNode steps;
        try { steps = mapper.readTree(seq.getStepsJson() == null ? "[]" : seq.getStepsJson()); }
        catch (Exception e) { activeRuns.remove(runId); return; }
        status.totalSteps = steps.size();

        for (int i = 0; i < steps.size(); i++) {
            if (cancelled.contains(runId)) { status.cancelled = true; break; }
            status.currentStep = i;
            JsonNode step = steps.get(i);
            String type = step.path("type").asText("");
            status.currentStepType = type;
            try {
                executeStep(type, step, p);
            } catch (Exception e) {
                LOG.warn("nightmare step {} fail: {}", type, e.getMessage());
            }
        }

        status.finished = true;
        // Mantém status por 5s depois remove
        new Thread(() -> {
            try { Thread.sleep(5000); } catch (InterruptedException ignored) {}
            activeRuns.remove(runId);
            cancelled.remove(runId);
        }, "nightmare-cleanup").start();
    }

    private void executeStep(String type, JsonNode s, EngineActions.PlayerInfo p) throws InterruptedException {
        switch (type) {
            case "title":
                actions.title(p.uuid(), s.path("title").asText(""), s.path("subtitle").asText(""),
                        s.path("fadeIn").asInt(10), s.path("stay").asInt(50), s.path("fadeOut").asInt(10));
                break;
            case "sound":
                actions.sound(p.uuid(), s.path("sound").asText("minecraft:ambient.cave"),
                        s.path("volume").asDouble(1), s.path("pitch").asDouble(1));
                break;
            case "particle":
                actions.particle(s.path("particle").asText("minecraft:smoke"),
                        p.x(), p.y() + s.path("offsetY").asDouble(0), p.z(),
                        s.path("count").asInt(30));
                break;
            case "effect":
                actions.effect(p.uuid(), s.path("effect").asText("minecraft:slowness"),
                        s.path("durationSec").asInt(10) * 20, s.path("amplifier").asInt(0));
                break;
            case "spawn_mob":
                int cnt = s.path("count").asInt(1);
                for (int k = 0; k < cnt; k++) {
                    actions.spawnEntity(s.path("entity").asText("minecraft:zombie"),
                            p.x() + s.path("offsetX").asDouble(0) + Math.random() * 2 - 1,
                            p.y() + s.path("offsetY").asDouble(0),
                            p.z() + s.path("offsetZ").asDouble(0) + Math.random() * 2 - 1,
                            1, p.dimension().replace("minecraft:", ""));
                }
                break;
            case "chat":
                actions.tellraw(p.name(), s.path("message").asText(""));
                break;
            case "command":
                actions.runCommand(s.path("command").asText("").replace("@s", p.name()));
                break;
            case "tp":
                actions.teleport(p.uuid(),
                        s.has("x") ? s.path("x").asDouble() : p.x(),
                        s.has("y") ? s.path("y").asDouble() : p.y(),
                        s.has("z") ? s.path("z").asDouble() : p.z(),
                        s.path("dim").asText(p.dimension().replace("minecraft:", "")));
                break;
            case "lightning":
                actions.lightning(p.uuid());
                break;
            case "wait":
                Thread.sleep(s.path("durationSec").asLong(1) * 1000);
                break;
            // snapshot_inv/restore_inv: requer endpoint do mod; pulamos silenciosamente
            case "snapshot_inv":
            case "restore_inv":
                // backend não tem acesso direto à mod snapshot ainda — TODO
                break;
        }
    }

    public void cancel(String runId) { cancelled.add(runId); }
    public List<RunStatus> activeRuns() { return List.copyOf(activeRuns.values()); }

    public List<NightmareSequence> list() { return repo.findAll(); }
    public NightmareSequence save(NightmareSequence s) { return repo.save(s); }
    public void delete(String id) { repo.deleteById(id); }
}

@RestController
@RequestMapping("/api/nightmares")
class NightmareController {

    private final NightmareEngine engine;
    NightmareController(NightmareEngine engine) { this.engine = engine; }

    @GetMapping("")
    public Map<String, Object> all() {
        return Map.of("sequences", engine.list(), "active", engine.activeRuns());
    }

    @PostMapping("/sequences")
    public NightmareSequence save(@RequestBody NightmareSequence s) {
        if (s.getId() == null || s.getId().isBlank())
            s.setId("nm_" + Long.toHexString(System.currentTimeMillis()));
        return engine.save(s);
    }

    @DeleteMapping("/sequences/{id}")
    public Map<String, Object> delete(@PathVariable String id) {
        engine.delete(id);
        return Map.of("ok", true);
    }

    @PostMapping("/run")
    public Map<String, Object> run(@RequestBody Map<String, String> body) {
        engine.runAsync(body.get("seqId"), body.get("playerUuid"));
        return Map.of("ok", true);
    }

    @PostMapping("/cancel/{runId}")
    public Map<String, Object> cancel(@PathVariable String runId) {
        engine.cancel(runId);
        return Map.of("ok", true);
    }
}
