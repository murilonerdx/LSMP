package br.com.murilo.liberthia.admin.engine.madness;

import br.com.murilo.liberthia.admin.engine.EngineActions;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

/**
 * Engine da sanidade. Roda no backend continuamente:
 *  - Decay automático por minuto
 *  - Aplica efeitos baseado em faixa de sanidade
 *  - Whispers podem ir SÓ pro player afetado ou BROADCAST a todos
 *  - FX no mundo (partículas em pos aleatória, armor_stand fantasma, lightning, falling_block)
 *  - Mensagens rotacionam aleatoriamente do pool
 *
 * Tudo persistido em PostgreSQL. Independente da página web estar aberta.
 */
@Service
public class MadnessEngine {

    private static final Logger LOG = LoggerFactory.getLogger(MadnessEngine.class);

    private final MadnessStateRepository stateRepo;
    private final MadnessConfigRepository configRepo;
    private final EngineActions actions;
    private final ObjectMapper mapper = new ObjectMapper();
    private final Random rng = new Random();

    private long lastTickMs = 0;

    public MadnessEngine(MadnessStateRepository stateRepo, MadnessConfigRepository configRepo, EngineActions actions) {
        this.stateRepo = stateRepo;
        this.configRepo = configRepo;
        this.actions = actions;
    }

    @PostConstruct
    public void init() {
        LOG.info("MadnessEngine ✓ ready (config via SQL seed)");
    }

    @Scheduled(fixedDelay = 2000, initialDelay = 5000)
    public void tick() {
        MadnessConfig cfg;
        try { cfg = configRepo.findById(1).orElse(null); }
        catch (Exception e) { LOG.debug("madness config read fail: {}", e.getMessage()); return; }
        if (cfg == null || !cfg.isEnabled()) return;

        long now = System.currentTimeMillis();
        if (now - lastTickMs < cfg.getTickIntervalSec() * 1000L) return;
        lastTickMs = now;

        // 1) Decay automático pra todos os online + garante state row pra cada player
        List<EngineActions.PlayerInfo> online = actions.getPlayers();
        for (var p : online) {
            MadnessState s;
            try { s = stateRepo.findById(p.uuid()).orElse(null); }
            catch (Exception e) { continue; }
            if (s == null) {
                s = new MadnessState(p.uuid(), p.name(), 100.0);
                try { stateRepo.save(s); } catch (Exception ignored) {}
                continue;
            }
            double minSinceLast = s.getLastTick() == null ? 0 : (now - s.getLastTick().toEpochMilli()) / 60000.0;
            double newSanity = Math.max(0, s.getSanity() - cfg.getDecayPerMinute() * minSinceLast);
            s.setSanity(newSanity);
            s.setLastTick(Instant.now());
            s.setName(p.name());
            try { stateRepo.save(s); } catch (Exception ignored) {}
        }

        // 2) Aplica efeitos baseado em zona pra cada player online
        for (var p : online) {
            MadnessState s = stateRepo.findById(p.uuid()).orElse(null);
            if (s == null) continue;
            String zone = zoneFor(s.getSanity());
            applyZoneEffects(p, zone, cfg);
            if (cfg.isWorldFxEnabled() && !"lucido".equals(zone) && rng.nextDouble() < cfg.getWorldFxChance()) {
                fireRandomWorldFx(p, cfg, zone);
            }
        }
    }

    public static String zoneFor(double s) {
        if (s >= 80) return "lucido";
        if (s >= 60) return "inquieto";
        if (s >= 40) return "perturbado";
        if (s >= 20) return "histerico";
        return "consumido";
    }

    private void applyZoneEffects(EngineActions.PlayerInfo p, String zone, MadnessConfig cfg) {
        // Pega mensagem aleatória da zona
        String msg = pickRandomMessage(zone, cfg);
        String snd = pickRandomSound(zone, cfg);

        // Executa comandos custom configurados pra essa zona. Cada comando aceita
        // {player} como placeholder e roda como op no servidor MC.
        String[] cmds = parseZoneArray(cfg.getZoneCommandsJson(), zone);
        if (cmds != null) {
            for (String raw : cmds) {
                if (raw == null || raw.isBlank()) continue;
                String cmd = raw.replace("{player}", p.name()).replace("{target}", p.name());
                actions.runCommand(cmd);
            }
        }

        switch (zone) {
            case "inquieto":
                if (msg != null && rng.nextDouble() < 0.35) sendWhisper(p, msg, cfg);
                if (snd != null) actions.sound(p.uuid(), snd, 0.6, 0.9 + rng.nextDouble() * 0.3);
                break;
            case "perturbado":
                if (rng.nextDouble() < 0.7) {
                    actions.effect(p.uuid(), "minecraft:nausea", 200, 0);
                    actions.title(p.uuid(), " ", "§5§o...algo respira...", 5, 30, 10);
                    if (snd != null) actions.sound(p.uuid(), snd, 0.7, 0.6);
                    if (msg != null) sendWhisper(p, msg, cfg);
                }
                break;
            case "histerico":
                actions.effect(p.uuid(), "minecraft:blindness", 60, 0);
                actions.particle("minecraft:smoke", p.x(), p.y() + 1, p.z(), 30);
                if (rng.nextDouble() < 0.5) {
                    String[] mobs = {"minecraft:zombie", "minecraft:skeleton", "minecraft:husk", "minecraft:phantom"};
                    String mob = mobs[rng.nextInt(mobs.length)];
                    double ang = rng.nextDouble() * Math.PI * 2;
                    double r = 6 + rng.nextDouble() * 6;
                    actions.spawnEntity(mob,
                            p.x() + Math.cos(ang) * r, p.y(),
                            p.z() + Math.sin(ang) * r, 1,
                            p.dimension().replace("minecraft:", ""));
                }
                if (snd != null) actions.sound(p.uuid(), snd, 1.0, 0.4);
                if (msg != null) sendWhisper(p, msg, cfg);
                break;
            case "consumido":
                actions.effect(p.uuid(), "minecraft:darkness", 120, 0);
                actions.effect(p.uuid(), "minecraft:slowness", 80, 1);
                actions.particle("minecraft:sculk_soul", p.x(), p.y() + 1, p.z(), 60);
                if (rng.nextDouble() < 0.4) actions.lightning(p.uuid());
                actions.title(p.uuid(), "§5§l? ? ?", "§8§o— você não está mais aqui —", 0, 25, 10);
                if (snd != null) actions.sound(p.uuid(), snd, 0.9, 0.5);
                if (msg != null) sendWhisper(p, msg, cfg);
                break;
        }
    }

    private void sendWhisper(EngineActions.PlayerInfo p, String message, MadnessConfig cfg) {
        if ("everyone".equalsIgnoreCase(cfg.getWhisperBroadcastMode())) {
            // Broadcast pra todos - prefixa com nome do player afetado
            String prefixed = "§8§o[" + p.name() + "] " + message;
            actions.tellraw("@a", prefixed);
        } else {
            actions.tellraw(p.name(), message);
        }
    }

    /** Spawn de FX aleatórios no mundo perto do player. */
    private void fireRandomWorldFx(EngineActions.PlayerInfo p, MadnessConfig cfg, String zone) {
        String[] fxTypes = parseStringArray(cfg.getWorldFxTypesJson());
        if (fxTypes.length == 0) return;
        String fx = fxTypes[rng.nextInt(fxTypes.length)];

        double ang = rng.nextDouble() * Math.PI * 2;
        double dist = 3 + rng.nextDouble() * 12;
        double fxX = p.x() + Math.cos(ang) * dist;
        double fxZ = p.z() + Math.sin(ang) * dist;
        double fxY = p.y() + rng.nextDouble() * 4 - 2;
        String dim = p.dimension().replace("minecraft:", "");

        switch (fx) {
            case "random_particle":
                String[] particles = {"minecraft:sculk_soul", "minecraft:smoke", "minecraft:soul_fire_flame", "minecraft:end_rod", "minecraft:portal", "minecraft:warped_spore"};
                actions.particle(particles[rng.nextInt(particles.length)], fxX, fxY + 1, fxZ, 40);
                break;
            case "ghost_armor_stand":
                String[] names = {"???", "Notch", "Herobrine", "Steve", "Alex"};
                String name = names[rng.nextInt(names.length)];
                String tag = "liberthia_madness_ghost";
                String cmd = String.format(Locale.US,
                        "summon armor_stand %.2f %.2f %.2f " +
                        "{CustomName:'\"§7§o%s\"',CustomNameVisible:1b,Invulnerable:1b,NoGravity:1b,ShowArms:1b," +
                        "Tags:[\"%s\"],Rotation:[%df,0f]," +
                        "ArmorItems:[{},{},{},{id:\"minecraft:player_head\",Count:1b,tag:{SkullOwner:\"%s\"}}]}",
                        fxX, p.y(), fxZ, name, tag, rng.nextInt(360), name);
                actions.runCommand(cmd);
                // Despawn dele depois de 20s
                scheduleDespawn(tag);
                break;
            case "random_sound":
                String[] sounds = {"minecraft:entity.warden.heartbeat", "minecraft:entity.ghast.scream", "minecraft:ambient.cave", "minecraft:entity.warden.angry"};
                actions.sound(p.uuid(), sounds[rng.nextInt(sounds.length)], 0.7, 0.4 + rng.nextDouble() * 0.5);
                break;
            case "falling_block":
                String[] blocks = {"minecraft:soul_sand", "minecraft:obsidian", "minecraft:black_concrete"};
                String blk = blocks[rng.nextInt(blocks.length)];
                actions.runCommand(String.format(Locale.US,
                        "summon falling_block %.2f %.2f %.2f {BlockState:{Name:\"%s\"},Time:1}",
                        fxX, p.y() + 8, fxZ, blk));
                break;
            case "random_lightning":
                if ("consumido".equals(zone) || rng.nextDouble() < 0.3) {
                    actions.runCommand(String.format(Locale.US, "summon lightning_bolt %.2f %.2f %.2f", fxX, p.y(), fxZ));
                }
                break;
        }
    }

    private void scheduleDespawn(String tag) {
        new Thread(() -> {
            try { Thread.sleep(20_000); } catch (InterruptedException ignored) {}
            actions.runCommand("kill @e[type=armor_stand,tag=" + tag + "]");
        }, "madness-despawn").start();
    }

    private String pickRandomMessage(String zone, MadnessConfig cfg) {
        String[] arr = parseZoneArray(cfg.getZoneMessagesJson(), zone);
        if (arr.length == 0) return null;
        return arr[rng.nextInt(arr.length)];
    }

    private String pickRandomSound(String zone, MadnessConfig cfg) {
        String[] arr = parseZoneArray(cfg.getZoneSoundsJson(), zone);
        if (arr.length == 0) return null;
        return arr[rng.nextInt(arr.length)];
    }

    private String[] parseZoneArray(String json, String zone) {
        if (json == null || json.isBlank()) return new String[0];
        try {
            JsonNode n = mapper.readTree(json);
            JsonNode arr = n.get(zone);
            if (arr == null || !arr.isArray()) return new String[0];
            String[] out = new String[arr.size()];
            for (int i = 0; i < arr.size(); i++) out[i] = arr.get(i).asText();
            return out;
        } catch (Exception e) { return new String[0]; }
    }

    private String[] parseStringArray(String json) {
        if (json == null || json.isBlank()) return new String[0];
        try {
            JsonNode n = mapper.readTree(json);
            if (!n.isArray()) return new String[0];
            String[] out = new String[n.size()];
            for (int i = 0; i < n.size(); i++) out[i] = n.get(i).asText();
            return out;
        } catch (Exception e) { return new String[0]; }
    }

    private String defaultZoneMessages() {
        ObjectNode root = mapper.createObjectNode();
        root.set("inquieto", arr(
                "§8§o...alguém te chama?",
                "§8§oa pele formiga.",
                "§8§o...você foi observado.",
                "§8§ohá algo aqui.",
                "§8§oa lâmpada fraqueja.",
                "§8§oo silêncio é denso.",
                "§8§o— olhe pra trás. devagar."
        ));
        root.set("perturbado", arr(
                "§5§o...algo respira.",
                "§5§oeles te conhecem agora.",
                "§5§o— a sombra cresce —",
                "§5§oo chão fica frio.",
                "§5§oele sabe seu nome."
        ));
        root.set("histerico", arr(
                "§c§oCORRA.",
                "§c§oele tá vindo.",
                "§c§o— a porta abriu —",
                "§c§oeles estão dentro de você.",
                "§c§oolha pra cima §lAGORA§r§c§o."
        ));
        root.set("consumido", arr(
                "§4§l— você não está mais aqui —",
                "§4§lo véu se rasgou.",
                "§4§leles te chamam pelo §nverdadeiro§r§4§l nome.",
                "§4§l— o mundo é deles agora —",
                "§4§lresponda."
        ));
        return root.toString();
    }

    private String defaultZoneSounds() {
        ObjectNode root = mapper.createObjectNode();
        root.set("inquieto", arr("minecraft:ambient.cave", "minecraft:block.sculk.charge", "minecraft:entity.allay.ambient_with_item"));
        root.set("perturbado", arr("minecraft:entity.warden.heartbeat", "minecraft:ambient.warped_forest.loop"));
        root.set("histerico", arr("minecraft:entity.warden.angry", "minecraft:entity.ghast.scream", "minecraft:ambient.cave"));
        root.set("consumido", arr("minecraft:entity.warden.angry", "minecraft:entity.wither.spawn", "minecraft:entity.ender_dragon.growl"));
        return root.toString();
    }

    private ArrayNode arr(String... lines) {
        ArrayNode a = mapper.createArrayNode();
        for (String s : lines) a.add(s);
        return a;
    }

    // ===== API pro controller =====

    public List<MadnessState> getAllState() { return stateRepo.findAll(); }

    public MadnessConfig getConfig() {
        return configRepo.findById(1).orElseGet(() -> {
            MadnessConfig c = new MadnessConfig();
            c.setId(1);
            return configRepo.save(c);
        });
    }

    public MadnessConfig saveConfig(MadnessConfig c) {
        c.setId(1);
        return configRepo.save(c);
    }

    public void setSanity(String uuid, double v) {
        MadnessState s = stateRepo.findById(uuid).orElse(new MadnessState(uuid, "?", 100));
        s.setSanity(Math.max(0, Math.min(100, v)));
        s.setLastTick(Instant.now());
        EngineActions.PlayerInfo p = actions.findPlayer(uuid);
        if (p != null) s.setName(p.name());
        stateRepo.save(s);
    }

    public void deltaSanity(String uuid, double d) {
        MadnessState s = stateRepo.findById(uuid).orElse(null);
        double cur = s == null ? 100 : s.getSanity();
        setSanity(uuid, cur + d);
    }

    public void resetAll(double v) {
        List<MadnessState> all = stateRepo.findAll();
        for (MadnessState s : all) {
            s.setSanity(Math.max(0, Math.min(100, v)));
            s.setLastTick(Instant.now());
        }
        stateRepo.saveAll(all);
    }

    /** Força aplicação imediata da zona atual (botão "manifestar" do painel). */
    public void manifest(String uuid) {
        MadnessState s = stateRepo.findById(uuid).orElse(null);
        if (s == null) return;
        EngineActions.PlayerInfo p = actions.findPlayer(uuid);
        if (p == null) return;
        MadnessConfig cfg = getConfig();
        applyZoneEffects(p, zoneFor(s.getSanity()), cfg);
        if (cfg.isWorldFxEnabled()) fireRandomWorldFx(p, cfg, zoneFor(s.getSanity()));
    }
}
