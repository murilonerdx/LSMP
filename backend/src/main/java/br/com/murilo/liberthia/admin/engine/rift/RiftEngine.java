package br.com.murilo.liberthia.admin.engine.rift;

import br.com.murilo.liberthia.admin.engine.EngineActions;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Repository
interface RiftRepository extends JpaRepository<Rift, String> {
    List<Rift> findByEnabledTrue();
}

@Service
public class RiftEngine {

    private static final Logger LOG = LoggerFactory.getLogger(RiftEngine.class);

    private final RiftRepository repo;
    private final EngineActions actions;
    private final ObjectMapper mapper = new ObjectMapper();
    private final Random rng = new Random();
    private final Map<String, Long> cooldowns = new ConcurrentHashMap<>();
    private final Map<String, Long> lastVisual = new ConcurrentHashMap<>();

    public RiftEngine(RiftRepository repo, EngineActions actions) {
        this.repo = repo;
        this.actions = actions;
    }

    @Scheduled(fixedDelay = 1500, initialDelay = 7000)
    public void tick() {
        long now = System.currentTimeMillis();
        List<Rift> enabled;
        try { enabled = repo.findByEnabledTrue(); }
        catch (Exception e) { return; }

        var players = actions.getPlayers();
        for (Rift r : enabled) {
            String dimMatch = "minecraft:" + r.getPosDim();

            // Visualize partículas
            if (r.isVisualize() && now - lastVisual.getOrDefault(r.getId(), 0L) >= r.getVisualEvery() * 1000L) {
                lastVisual.put(r.getId(), now);
                for (var p : players) {
                    if (!p.dimension().equals(dimMatch)) continue;
                    double d = dist(p.x(), p.y(), p.z(), r.getPosX(), r.getPosY(), r.getPosZ());
                    if (d < 40) {
                        actions.particle(r.getParticle(), r.getPosX(), r.getPosY(), r.getPosZ(), r.getParticleCount());
                        actions.sound(p.uuid(), r.getAmbientSound(), 0.45, 0.7);
                    }
                }
            }

            // Detecção de player
            for (var p : players) {
                if (!p.dimension().equals(dimMatch)) continue;
                double d = dist(p.x(), p.y(), p.z(), r.getPosX(), r.getPosY(), r.getPosZ());
                if (d > r.getRadius()) continue;
                String key = p.uuid() + "_" + r.getId();
                if (cooldowns.getOrDefault(key, 0L) > now) continue;
                cooldowns.put(key, now + r.getCooldownSec() * 1000L);
                triggerRift(r, p);
            }
        }
    }

    private void triggerRift(Rift r, EngineActions.PlayerInfo p) {
        new Thread(() -> {
            try {
                actions.tellraw(p.name(), r.getPreMsg() == null ? "" : r.getPreMsg());
                applyEffects(r.getPreEffectsJson(), p.uuid());
                actions.sound(p.uuid(), r.getAmbientSound(), 1.0, 0.3);
                actions.particle(r.getParticle(), p.x(), p.y() + 1, p.z(), 80);
                Thread.sleep(1500);

                double destX, destY, destZ;
                String destDim;
                if (r.getDestX() != null && r.getDestY() != null && r.getDestZ() != null && r.getDestDim() != null) {
                    destX = r.getDestX(); destY = r.getDestY(); destZ = r.getDestZ();
                    destDim = r.getDestDim();
                } else {
                    double ang = rng.nextDouble() * Math.PI * 2;
                    double dd = r.getRandomDestRadius() * (0.3 + rng.nextDouble() * 0.7);
                    destX = r.getPosX() + Math.cos(ang) * dd;
                    destZ = r.getPosZ() + Math.sin(ang) * dd;
                    destY = r.getPosY();
                    destDim = r.getPosDim();
                }
                actions.teleport(p.uuid(), destX, destY, destZ, destDim);
                Thread.sleep(500);

                actions.tellraw(p.name(), r.getPostMsg() == null ? "" : r.getPostMsg());
                applyEffects(r.getPostEffectsJson(), p.uuid());
                actions.particle(r.getParticle(), destX, destY + 1, destZ, 100);

                r.setTriggers(r.getTriggers() + 1);
                try { repo.save(r); } catch (Exception ignored) {}
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                LOG.debug("rift trigger fail: {}", e.getMessage());
            }
        }, "rift-trigger").start();
    }

    private void applyEffects(String json, String uuid) {
        if (json == null || json.isBlank()) return;
        try {
            JsonNode arr = mapper.readTree(json);
            if (!arr.isArray()) return;
            for (JsonNode e : arr) {
                actions.effect(uuid,
                        e.path("effect").asText("minecraft:blindness"),
                        e.path("durationSec").asInt(5) * 20,
                        e.path("amplifier").asInt(0));
            }
        } catch (Exception ignored) {}
    }

    private double dist(double ax, double ay, double az, double bx, double by, double bz) {
        double dx = ax - bx, dy = ay - by, dz = az - bz;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    public List<Rift> list() { return repo.findAll(); }
    public Rift save(Rift r) { return repo.save(r); }
    public void delete(String id) { repo.deleteById(id); }
    public Rift toggle(String id) {
        Rift r = repo.findById(id).orElseThrow();
        r.setEnabled(!r.isEnabled());
        return repo.save(r);
    }
}
