package br.com.murilo.liberthia.admin.engine.boxes;

import br.com.murilo.liberthia.admin.engine.EngineActions;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Repository
interface TimeBoxRepository extends JpaRepository<TimeBox, String> {}

@Service
public class BoxEngine {

    private static final Logger LOG = LoggerFactory.getLogger(BoxEngine.class);
    private final TimeBoxRepository repo;
    private final EngineActions actions;
    private final ObjectMapper mapper = new ObjectMapper();

    public BoxEngine(TimeBoxRepository repo, EngineActions actions) {
        this.repo = repo;
        this.actions = actions;
    }

    @Scheduled(fixedDelay = 5000, initialDelay = 9000)
    public void tick() {
        Instant now = Instant.now();
        List<TimeBox> all;
        try { all = repo.findAll(); }
        catch (Exception e) { return; }

        for (TimeBox b : all) {
            if (b.isDelivered()) continue;

            if (b.isCountdownEnabled() && b.getUnlockAt() != null && b.getUnlockAt().isAfter(now)) {
                Instant last = b.getLastCountdownTs();
                long minSinceLast = last == null ? Long.MAX_VALUE : (now.toEpochMilli() - last.toEpochMilli()) / 60000;
                if (minSinceLast >= b.getCountdownEveryMin()) {
                    String remain = formatRemaining(b.getUnlockAt().toEpochMilli() - now.toEpochMilli());
                    String msg = (b.getCountdownMsg() == null ? "" : b.getCountdownMsg()).replace("{time}", remain);
                    for (var p : resolveTargets(b.getRecipient())) {
                        actions.tellraw(p.name(), msg);
                    }
                    b.setLastCountdownTs(now);
                    try { repo.save(b); } catch (Exception ignored) {}
                }
            }

            if (b.getUnlockAt() != null && !b.getUnlockAt().isAfter(now)) {
                var targets = resolveTargets(b.getRecipient());
                if (targets.isEmpty()) continue; // espera target online
                deliverTo(b, targets);
                b.setDelivered(true);
                try { repo.save(b); } catch (Exception ignored) {}
            }
        }
    }

    private void deliverTo(TimeBox b, List<EngineActions.PlayerInfo> targets) {
        for (var p : targets) {
            try {
                actions.tellraw(p.name(), b.getUnlockMessage() == null ? "" : b.getUnlockMessage());
                actions.title(p.uuid(), b.getEmoji(), b.getName(), 10, 70, 20);
                actions.sound(p.uuid(), b.getUnlockSound(), 1, 1);
                actions.particle(b.getUnlockParticle(), p.x(), p.y() + 1, p.z(), 100);

                JsonNode items = mapper.readTree(b.getItemsJson() == null ? "[]" : b.getItemsJson());
                if (items.isArray()) for (JsonNode it : items)
                    actions.giveItem(p.uuid(), it.path("itemId").asText("minecraft:diamond"), it.path("count").asInt(1));

                JsonNode eff = mapper.readTree(b.getEffectsJson() == null ? "[]" : b.getEffectsJson());
                if (eff.isArray()) for (JsonNode e : eff)
                    actions.effect(p.uuid(),
                            e.path("effect").asText("minecraft:regeneration"),
                            e.path("durationSec").asInt(30) * 20,
                            e.path("amplifier").asInt(0));
            } catch (Exception e) { LOG.debug("box deliver fail: {}", e.getMessage()); }
        }
    }

    private List<EngineActions.PlayerInfo> resolveTargets(String recipient) {
        if ("@a".equals(recipient)) return actions.getPlayers();
        if ("@first".equals(recipient)) {
            var all = actions.getPlayers();
            return all.isEmpty() ? List.of() : List.of(all.get(0));
        }
        var p = actions.findPlayer(recipient);
        return p == null ? List.of() : List.of(p);
    }

    private String formatRemaining(long ms) {
        if (ms <= 0) return "AGORA";
        long d = ms / 86400000L;
        long h = (ms % 86400000L) / 3600000L;
        long m = (ms % 3600000L) / 60000L;
        long s = (ms % 60000L) / 1000L;
        if (d > 0) return d + "d " + h + "h " + m + "m";
        if (h > 0) return h + "h " + m + "m " + s + "s";
        if (m > 0) return m + "m " + s + "s";
        return s + "s";
    }

    public List<TimeBox> list() { return repo.findAll(); }
    public TimeBox save(TimeBox b) { return repo.save(b); }
    public void delete(String id) { repo.deleteById(id); }
    public TimeBox forceOpen(String id) {
        TimeBox b = repo.findById(id).orElseThrow();
        b.setUnlockAt(Instant.now().minusSeconds(5));
        return repo.save(b);
    }
    public TimeBox resetDelivered(String id) {
        TimeBox b = repo.findById(id).orElseThrow();
        b.setDelivered(false);
        return repo.save(b);
    }
}
