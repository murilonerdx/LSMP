package br.com.murilo.liberthia.admin.engine.possession;

import br.com.murilo.liberthia.admin.engine.EngineActions;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Random;

@Service
public class PossessionEngine {

    private static final Logger LOG = LoggerFactory.getLogger(PossessionEngine.class);

    private final PossessionEntityRepository entityRepo;
    private final PossessionActiveRepository activeRepo;
    private final EngineActions actions;
    private final ObjectMapper mapper = new ObjectMapper();
    private final Random rng = new Random();

    public PossessionEngine(PossessionEntityRepository entityRepo,
                            PossessionActiveRepository activeRepo,
                            EngineActions actions) {
        this.entityRepo = entityRepo;
        this.activeRepo = activeRepo;
        this.actions = actions;
    }

    @PostConstruct
    public void init() {
        LOG.info("PossessionEngine ✓ ready (entities via SQL seed)");
    }

    @Scheduled(fixedDelay = 2000, initialDelay = 5000)
    public void tick() {
        Instant now = Instant.now();
        List<PossessionActive> active;
        try { active = activeRepo.findByEndsAtAfter(now); }
        catch (Exception e) { return; }

        for (PossessionActive pa : active) {
            PossessionEntity ent = entityRepo.findById(pa.getEntityId()).orElse(null);
            if (ent == null) continue;
            EngineActions.PlayerInfo player = actions.findPlayer(pa.getPlayerUuid());
            if (player == null) continue;

            // Renova effects
            applyEffects(ent, player.uuid(), 100);
            actions.particle(ent.getParticle(), player.x(), player.y() + 1, player.z(), 20);

            // Fala?
            if (pa.getLastSpeak() == null || now.toEpochMilli() - pa.getLastSpeak().toEpochMilli() >= ent.getSpeakIntervalSec() * 1000L) {
                String line = pickRandomLine(ent);
                if (line != null) {
                    String msg = String.format("<%s§r§f> %s", ent.getDisplayName(), line);
                    actions.tellraw("@a", msg);
                    actions.sound(player.uuid(), ent.getSound(), 0.7, 0.7 + rng.nextDouble() * 0.4);
                }
                pa.setLastSpeak(now);
                try { activeRepo.save(pa); } catch (Exception ignored) {}
            }
        }

        // Expira
        List<PossessionActive> expired;
        try { expired = activeRepo.findByEndsAtBefore(now); }
        catch (Exception e) { return; }
        for (PossessionActive pa : expired) {
            endNow(pa);
        }
    }

    private void applyEffects(PossessionEntity ent, String uuid, int durationTicks) {
        try {
            JsonNode arr = mapper.readTree(ent.getEffectsJson() == null ? "[]" : ent.getEffectsJson());
            if (!arr.isArray()) return;
            for (JsonNode e : arr) {
                actions.effect(uuid, e.path("effect").asText("minecraft:glowing"), durationTicks, e.path("amplifier").asInt(0));
            }
        } catch (Exception ignored) {}
    }

    private String pickRandomLine(PossessionEntity ent) {
        try {
            JsonNode arr = mapper.readTree(ent.getVoiceLinesJson() == null ? "[]" : ent.getVoiceLinesJson());
            if (!arr.isArray() || arr.size() == 0) return null;
            return arr.get(rng.nextInt(arr.size())).asText();
        } catch (Exception e) { return null; }
    }

    public PossessionActive start(String entityId, String playerUuid, int durationSec) {
        PossessionEntity ent = entityRepo.findById(entityId).orElse(null);
        if (ent == null) throw new IllegalArgumentException("entity not found");
        EngineActions.PlayerInfo player = actions.findPlayer(playerUuid);
        String name = player == null ? "?" : player.name();

        if (player != null) {
            actions.title(player.uuid(), ent.getEmoji(), ent.getDisplayName(), 10, 60, 20);
            actions.sound(player.uuid(), ent.getSound(), 1.0, 0.5);
            actions.tellraw("@a", ent.getEnterMsg());
            actions.particle(ent.getParticle(), player.x(), player.y() + 1, player.z(), 60);
            applyEffects(ent, player.uuid(), durationSec * 20);
        }

        PossessionActive pa = new PossessionActive();
        pa.setEntityId(entityId);
        pa.setPlayerUuid(playerUuid);
        pa.setPlayerName(name);
        pa.setStartedAt(Instant.now());
        pa.setEndsAt(Instant.now().plusSeconds(durationSec));
        pa.setLastSpeak(Instant.EPOCH);
        return activeRepo.save(pa);
    }

    public void endNow(PossessionActive pa) {
        PossessionEntity ent = entityRepo.findById(pa.getEntityId()).orElse(null);
        EngineActions.PlayerInfo player = actions.findPlayer(pa.getPlayerUuid());
        if (ent != null && player != null) {
            actions.tellraw("@a", ent.getExitMsg());
            try {
                JsonNode arr = mapper.readTree(ent.getEffectsJson() == null ? "[]" : ent.getEffectsJson());
                if (arr.isArray()) for (JsonNode e : arr)
                    actions.runCommand("effect clear " + player.name() + " " + e.path("effect").asText());
            } catch (Exception ignored) {}
        }
        try { activeRepo.delete(pa); } catch (Exception ignored) {}
    }

    public void endById(Long id) {
        activeRepo.findById(id).ifPresent(this::endNow);
    }

    // --- Public API ---
    public List<PossessionEntity> listEntities() { return entityRepo.findAll(); }
    public PossessionEntity saveEntity(PossessionEntity e) { return entityRepo.save(e); }
    public void deleteEntity(String id) { entityRepo.deleteById(id); }
    public List<PossessionActive> listActive() { return activeRepo.findByEndsAtAfter(Instant.now()); }
}
