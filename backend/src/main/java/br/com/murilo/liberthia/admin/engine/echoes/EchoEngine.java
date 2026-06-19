package br.com.murilo.liberthia.admin.engine.echoes;

import br.com.murilo.liberthia.admin.engine.EngineActions;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Repository
interface MemoryEchoRepository extends JpaRepository<MemoryEcho, String> {}

@Repository
interface MemoryEchoActiveRepository extends JpaRepository<MemoryEchoActive, Long> {}

@Service
public class EchoEngine {

    private static final Logger LOG = LoggerFactory.getLogger(EchoEngine.class);
    private final MemoryEchoRepository repo;
    private final MemoryEchoActiveRepository activeRepo;
    private final EngineActions actions;
    private final ObjectMapper mapper = new ObjectMapper();
    private final Map<String, Long> cooldowns = new ConcurrentHashMap<>();

    public EchoEngine(MemoryEchoRepository repo, MemoryEchoActiveRepository activeRepo, EngineActions actions) {
        this.repo = repo;
        this.activeRepo = activeRepo;
        this.actions = actions;
    }

    @Scheduled(fixedDelay = 2000, initialDelay = 10000)
    public void tick() {
        Instant now = Instant.now();
        long nowMs = now.toEpochMilli();

        List<MemoryEcho> all;
        try { all = repo.findAll(); }
        catch (Exception e) { return; }

        var players = actions.getPlayers();
        List<MemoryEchoActive> active;
        try { active = activeRepo.findAll(); }
        catch (Exception e) { return; }

        // Trigger detection
        for (MemoryEcho echo : all) {
            if (!echo.isEnabled()) continue;
            String dimMatch = "minecraft:" + echo.getPosDim();
            for (var p : players) {
                if (!p.dimension().equals(dimMatch)) continue;
                double dx = p.x() - echo.getPosX(), dy = p.y() - echo.getPosY(), dz = p.z() - echo.getPosZ();
                double d = Math.sqrt(dx * dx + dy * dy + dz * dz);
                if (d > echo.getRadius()) continue;

                String cdKey = p.uuid() + "_" + echo.getId();
                if (cooldowns.getOrDefault(cdKey, 0L) > nowMs) continue;
                boolean alreadyActive = active.stream().anyMatch(a -> a.getEchoId().equals(echo.getId()) && a.getPlayerUuid().equals(p.uuid()));
                if (alreadyActive) continue;

                cooldowns.put(cdKey, nowMs + echo.getCooldownSec() * 1000L);
                startEcho(echo, p);
                MemoryEchoActive ma = new MemoryEchoActive();
                ma.setEchoId(echo.getId());
                ma.setPlayerUuid(p.uuid());
                ma.setStartedAt(now);
                ma.setEndsAt(now.plusSeconds(echo.getPlayDurationSec()));
                ma.setLastWhisper(Instant.EPOCH);
                ma.setWhisperIdx(0);
                try { activeRepo.save(ma); } catch (Exception ignored) {}
            }
        }

        // Tick active
        try { active = activeRepo.findAll(); }
        catch (Exception e) { return; }
        for (MemoryEchoActive a : active) {
            MemoryEcho echo = repo.findById(a.getEchoId()).orElse(null);
            if (echo == null) { activeRepo.delete(a); continue; }
            var p = actions.findPlayer(a.getPlayerUuid());
            if (p == null || nowMs >= a.getEndsAt().toEpochMilli()) {
                endEcho(echo);
                try { activeRepo.delete(a); } catch (Exception ignored) {}
                continue;
            }
            // Whisper rotativo
            try {
                JsonNode whispers = mapper.readTree(echo.getWhispersJson() == null ? "[]" : echo.getWhispersJson());
                if (whispers.isArray() && whispers.size() > 0 &&
                        nowMs - a.getLastWhisper().toEpochMilli() >= echo.getWhisperEverySec() * 1000L) {
                    String msg = whispers.get(a.getWhisperIdx() % whispers.size()).asText();
                    actions.tellraw(p.name(), msg);
                    a.setLastWhisper(now);
                    a.setWhisperIdx(a.getWhisperIdx() + 1);
                    try { activeRepo.save(a); } catch (Exception ignored) {}
                }
            } catch (Exception ignored) {}
            actions.particle(echo.getAmbientParticle(), echo.getPosX(), echo.getPosY() + 1, echo.getPosZ(), 30);
            if (Math.random() < 0.3) actions.sound(p.uuid(), echo.getAmbientSound(), 0.5, 0.5 + Math.random() * 0.5);
        }
    }

    private void startEcho(MemoryEcho echo, EngineActions.PlayerInfo p) {
        actions.title(p.uuid(), "§7§l...", "§5§o— você sente uma memória —", 10, 50, 10);
        actions.sound(p.uuid(), echo.getAmbientSound(), 1, 0.6);

        // Spawna fantasmas como ClonePlayerEntity (entidade do mod que renderiza
        // como player real: PlayerModel + armor + skin do nome dado).
        try {
            JsonNode ghosts = mapper.readTree(echo.getGhostsJson() == null ? "[]" : echo.getGhostsJson());
            if (ghosts.isArray()) {
                String tag = "liberthia_echo_" + echo.getId();
                for (JsonNode g : ghosts) {
                    String name = g.path("playerName").asText("Steve").replace("\"", "");
                    double x = echo.getPosX() + g.path("offset").path("x").asDouble(0);
                    double y = echo.getPosY() + g.path("offset").path("y").asDouble(0);
                    double z = echo.getPosZ() + g.path("offset").path("z").asDouble(0);
                    float rot = g.path("rotation").floatValue();
                    String customName = "§7§o" + name;
                    actions.spawnPlayerClone(name, x, y, z, echo.getPosDim(), rot, "liberthia_echo," + tag, customName);
                }
            }
        } catch (Exception ignored) {}
        actions.particle(echo.getAmbientParticle(), echo.getPosX(), echo.getPosY() + 1, echo.getPosZ(), 100);
    }

    private void endEcho(MemoryEcho echo) {
        actions.killByTag("liberthia_echo_" + echo.getId());
    }

    @PreDestroy
    public void cleanup() {
        try { actions.killByTag("liberthia_echo"); }
        catch (Exception ignored) {}
    }

    public List<MemoryEcho> list() { return repo.findAll(); }
    public MemoryEcho save(MemoryEcho m) { return repo.save(m); }
    public void delete(String id) { repo.deleteById(id); }
    public MemoryEcho toggle(String id) {
        MemoryEcho m = repo.findById(id).orElseThrow();
        m.setEnabled(!m.isEnabled());
        return repo.save(m);
    }
    public List<MemoryEchoActive> listActive() { return activeRepo.findAll(); }
    public void purge() {
        activeRepo.deleteAll();
        actions.runCommand("kill @e[type=armor_stand,tag=liberthia_echo]");
    }
}
