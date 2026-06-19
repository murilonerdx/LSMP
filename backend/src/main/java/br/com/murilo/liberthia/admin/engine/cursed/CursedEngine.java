package br.com.murilo.liberthia.admin.engine.cursed;

import br.com.murilo.liberthia.admin.engine.EngineActions;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

@Service
public class CursedEngine {

    private static final Logger LOG = LoggerFactory.getLogger(CursedEngine.class);

    private final CurseRepository curseRepo;
    private final CurseBindingRepository bindRepo;
    private final EngineActions actions;
    private final ObjectMapper mapper = new ObjectMapper();
    private final Random rng = new Random();

    public CursedEngine(CurseRepository curseRepo, CurseBindingRepository bindRepo, EngineActions actions) {
        this.curseRepo = curseRepo;
        this.bindRepo = bindRepo;
        this.actions = actions;
    }

    @PostConstruct
    public void init() {
        LOG.info("CursedEngine ✓ ready (curses via SQL seed)");
    }

    @Scheduled(fixedDelay = 2000, initialDelay = 6000)
    public void tick() {
        Instant now = Instant.now();
        List<CurseBinding> bindings;
        try { bindings = bindRepo.findAll(); }
        catch (Exception e) { return; }

        for (CurseBinding b : bindings) {
            Curse c = curseRepo.findById(b.getCurseId()).orElse(null);
            if (c == null) continue;
            if (b.getLastTick() != null && now.toEpochMilli() - b.getLastTick().toEpochMilli() < c.getTickIntervalSec() * 1000L) continue;
            EngineActions.PlayerInfo p = actions.findPlayer(b.getPlayerUuid());
            if (p == null) continue;

            // Aplica effects
            try {
                JsonNode eff = mapper.readTree(c.getCurseEffectsJson() == null ? "[]" : c.getCurseEffectsJson());
                if (eff.isArray()) for (JsonNode e : eff)
                    actions.effect(p.uuid(),
                            e.path("effect").asText("minecraft:nausea"),
                            e.path("durationSec").asInt(10) * 20,
                            e.path("amplifier").asInt(0));
            } catch (Exception ignored) {}

            // Whisper random
            try {
                JsonNode w = mapper.readTree(c.getWhispersJson() == null ? "[]" : c.getWhispersJson());
                if (w.isArray() && w.size() > 0) {
                    actions.tellraw(p.name(), w.get(rng.nextInt(w.size())).asText());
                }
            } catch (Exception ignored) {}

            actions.particle(c.getParticle(), p.x(), p.y() + 1, p.z(), 25);
            actions.sound(p.uuid(), c.getSound(), 0.6, 0.6 + rng.nextDouble() * 0.5);

            b.setLastTick(now);
            try { bindRepo.save(b); } catch (Exception ignored) {}
        }
    }

    /** Forja item via /give e adiciona binding. */
    public CurseBinding forge(String curseId, String playerUuid) {
        Curse c = curseRepo.findById(curseId).orElseThrow();
        EngineActions.PlayerInfo p = actions.findPlayer(playerUuid);
        if (p == null) throw new IllegalArgumentException("player offline");

        String nbt = buildNbt(c);
        actions.runCommand("give " + p.name() + " " + c.getItemId() + nbt + " 1");
        actions.title(p.uuid(), c.getEmoji(), c.getDisplayName(), 10, 50, 20);
        actions.sound(p.uuid(), c.getSound(), 1.0, 0.5);
        actions.tellraw(p.name(), "§4§o— você recebeu algo que não quer largar —");

        CurseBinding b = new CurseBinding();
        b.setPlayerUuid(p.uuid());
        b.setPlayerName(p.name());
        b.setCurseId(curseId);
        b.setStartedAt(Instant.now());
        b.setLastTick(Instant.EPOCH);
        return bindRepo.save(b);
    }

    private String buildNbt(Curse c) {
        StringBuilder sb = new StringBuilder("{display:{Name:'");
        sb.append(jsonText(c.getDisplayName())).append("'");
        try {
            JsonNode lore = mapper.readTree(c.getLoreJson() == null ? "[]" : c.getLoreJson());
            if (lore.isArray() && lore.size() > 0) {
                sb.append(",Lore:[");
                for (int i = 0; i < lore.size(); i++) {
                    if (i > 0) sb.append(',');
                    sb.append("'").append(jsonText(lore.get(i).asText())).append("'");
                }
                sb.append(']');
            }
        } catch (Exception ignored) {}
        sb.append('}');
        try {
            JsonNode en = mapper.readTree(c.getEnchantmentsJson() == null ? "[]" : c.getEnchantmentsJson());
            if (en.isArray() && en.size() > 0) {
                sb.append(",Enchantments:[");
                for (int i = 0; i < en.size(); i++) {
                    if (i > 0) sb.append(',');
                    sb.append("{id:\"").append(en.get(i).path("id").asText()).append("\",lvl:").append(en.get(i).path("level").asInt(1)).append('}');
                }
                sb.append(']');
            }
        } catch (Exception ignored) {}
        sb.append('}');
        return sb.toString();
    }

    private String jsonText(String s) {
        String escaped = (s == null ? "" : s).replace("\\", "\\\\").replace("\"", "\\\"");
        return "{\"text\":\"" + escaped + "\",\"italic\":false}";
    }

    public void unbind(Long bindingId) { bindRepo.deleteById(bindingId); }

    public List<Curse> listCurses() { return curseRepo.findAll(); }
    public Curse saveCurse(Curse c) { return curseRepo.save(c); }
    public void deleteCurse(String id) { curseRepo.deleteById(id); }
    public List<CurseBinding> listBindings() { return bindRepo.findAll(); }
}
