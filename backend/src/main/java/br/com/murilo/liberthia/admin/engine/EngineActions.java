package br.com.murilo.liberthia.admin.engine;

import br.com.murilo.liberthia.admin.mod.ModBridgeClient;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Helper que as engines de background usam pra:
 *   - Listar players (cacheado, refresh a cada 2s)
 *   - Disparar comandos no mod (api.command / sound / particle / effect / etc)
 *
 * Swallow de errors: as engines não devem crashar por erro de comunicação com o mod.
 * Tudo é logado em DEBUG silenciosamente.
 */
@Service
public class EngineActions {

    private static final Logger LOG = LoggerFactory.getLogger(EngineActions.class);

    private final ModBridgeClient mod;
    private final AtomicReference<List<PlayerInfo>> cachedPlayers = new AtomicReference<>(List.of());

    public EngineActions(ModBridgeClient mod) {
        this.mod = mod;
    }

    public record PlayerInfo(String uuid, String name, String dimension, double x, double y, double z) {}

    @Scheduled(fixedDelay = 2000, initialDelay = 1000)
    public void refreshPlayers() {
        try {
            JsonNode resp = mod.getPlayers();
            List<PlayerInfo> next = new ArrayList<>();
            if (resp.has("players") && resp.get("players").isArray()) {
                for (JsonNode p : resp.get("players")) {
                    JsonNode pos = p.get("position");
                    next.add(new PlayerInfo(
                            p.path("uuid").asText(),
                            p.path("name").asText(),
                            p.path("dimension").asText("minecraft:overworld"),
                            pos == null ? 0 : pos.path("x").asDouble(),
                            pos == null ? 0 : pos.path("y").asDouble(),
                            pos == null ? 0 : pos.path("z").asDouble()
                    ));
                }
            }
            cachedPlayers.set(next);
        } catch (Exception e) {
            LOG.debug("refreshPlayers fail: {}", e.getMessage());
        }
    }

    public List<PlayerInfo> getPlayers() { return cachedPlayers.get(); }

    public PlayerInfo findPlayer(String uuid) {
        for (PlayerInfo p : cachedPlayers.get()) if (p.uuid.equals(uuid)) return p;
        return null;
    }

    public PlayerInfo findPlayerByName(String name) {
        for (PlayerInfo p : cachedPlayers.get()) if (p.name.equalsIgnoreCase(name)) return p;
        return null;
    }

    // ===== Ações no mod =====

    public void runCommand(String cmd) {
        try { mod.runCommand(cmd); }
        catch (Exception e) { LOG.debug("runCommand fail '{}': {}", cmd, e.getMessage()); }
    }

    public void title(String uuid, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("title", title);
            body.put("subtitle", subtitle);
            body.put("fadeIn", fadeIn);
            body.put("stay", stay);
            body.put("fadeOut", fadeOut);
            mod.title(uuid, body);
        } catch (Exception e) { LOG.debug("title fail: {}", e.getMessage()); }
    }

    public void sound(String uuid, String sound, double volume, double pitch) {
        try {
            mod.sound(uuid, Map.of("sound", sound, "volume", volume, "pitch", pitch));
        } catch (Exception e) { LOG.debug("sound fail: {}", e.getMessage()); }
    }

    public void effect(String uuid, String effect, int durationTicks, int amplifier) {
        try {
            mod.applyEffect(uuid, Map.of("effect", effect, "duration", durationTicks, "amplifier", amplifier));
        } catch (Exception e) { LOG.debug("effect fail: {}", e.getMessage()); }
    }

    public void particle(String particle, double x, double y, double z, int count) {
        try {
            mod.particle(Map.of("particle", particle, "x", x, "y", y, "z", z, "count", count));
        } catch (Exception e) { LOG.debug("particle fail: {}", e.getMessage()); }
    }

    public void lightning(String uuid) {
        try { mod.lightning(uuid); }
        catch (Exception e) { LOG.debug("lightning fail: {}", e.getMessage()); }
    }

    public void spawnEntity(String entity, double x, double y, double z, int count, String dimension) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("entity", entity);
            body.put("x", x);
            body.put("y", y);
            body.put("z", z);
            body.put("count", count);
            if (dimension != null) body.put("dimension", dimension);
            mod.spawnEntity(body);
        } catch (Exception e) { LOG.debug("spawn fail: {}", e.getMessage()); }
    }

    /**
     * Spawna um ClonePlayerEntity (entidade que renderiza como player de verdade,
     * com PlayerModel + armor + skin do `playerName`). Aceita tag pra cleanup
     * posterior via {@link #killByTag(String)}.
     */
    public void spawnPlayerClone(String playerName, double x, double y, double z, String dimension, float rotation, String tag, String customName) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("x", x);
            body.put("y", y);
            body.put("z", z);
            if (dimension != null) body.put("dimension", dimension);
            if (playerName != null) body.put("playerName", playerName);
            body.put("rotation", rotation);
            if (tag != null) body.put("tag", tag);
            if (customName != null) body.put("customName", customName);
            mod.spawnPlayerClone(body);
        } catch (Exception e) { LOG.warn("spawnPlayerClone fail: {}", e.getMessage()); }
    }

    /** Mata todas as entidades com a tag dada (qualquer dim). */
    public void killByTag(String tag) {
        try { mod.killByTag(Map.of("tag", tag)); }
        catch (Exception e) { LOG.debug("killByTag '{}' fail: {}", tag, e.getMessage()); }
    }

    public void teleport(String uuid, double x, double y, double z, String dim) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("x", x);
            body.put("y", y);
            body.put("z", z);
            if (dim != null && !dim.isBlank()) body.put("dimension", dim);
            mod.teleport(uuid, body);
        } catch (Exception e) { LOG.debug("teleport fail: {}", e.getMessage()); }
    }

    public void giveItem(String uuid, String itemId, int count) {
        try {
            mod.giveItem(uuid, Map.of("item", itemId, "count", count));
        } catch (Exception e) { LOG.debug("give fail: {}", e.getMessage()); }
    }

    public void weather(String type, int duration) {
        try { mod.worldWeather(Map.of("type", type, "duration", duration)); }
        catch (Exception e) { LOG.debug("weather fail: {}", e.getMessage()); }
    }

    public void worldTime(int time) {
        try { mod.worldTime(Map.of("time", time)); }
        catch (Exception e) { LOG.debug("time fail: {}", e.getMessage()); }
    }

    /** tellraw global ou pra player específico. */
    public void tellraw(String target, String text) {
        String safeText = text == null ? "" : text.replace("\\", "\\\\").replace("\"", "\\\"");
        String json = "{\"text\":\"" + safeText + "\"}";
        runCommand("tellraw " + target + " " + json);
    }
}
