package br.com.murilo.liberthia.admin.engine.pehkui;

import br.com.murilo.liberthia.admin.mod.ModBridgeClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Pehkui — modificação de escalas dos players. Reusa o endpoint genérico
 * /api/command do mod pra disparar comandos /scale set / /scale reset.
 *
 * Endpoints expostos pro painel:
 *   POST /api/pehkui/scale            — aplica 1 tipo de escala em 1 player
 *   POST /api/pehkui/bulk             — aplica várias escalas em 1 player
 *   POST /api/pehkui/reset            — reseta 1 ou todos os tipos
 *   POST /api/pehkui/preset/{id}/apply — aplica preset salvo em 1 player
 *   GET  /api/pehkui/presets          — lista presets
 *   POST /api/pehkui/presets          — cria preset
 *   PUT  /api/pehkui/presets/{id}     — atualiza preset
 *   DELETE /api/pehkui/presets/{id}   — remove preset
 *   GET  /api/pehkui/eligible         — players online com DM acima de threshold
 *
 * Lista completa de scale types em PEHKUI_TYPES.
 */
@RestController
@RequestMapping("/api/pehkui")
public class PehkuiController {

    private static final Logger LOG = LoggerFactory.getLogger(PehkuiController.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** Todos os tipos de escala suportados pelo Pehkui 3.x. */
    public static final List<String> PEHKUI_TYPES = List.of(
            // tamanho / hitbox / visão
            "pehkui:base", "pehkui:width", "pehkui:height", "pehkui:eye_height",
            "pehkui:hitbox_width", "pehkui:hitbox_height",
            "pehkui:visibility", "pehkui:hostile_visibility", "pehkui:third_person",
            // combate
            "pehkui:attack", "pehkui:reach", "pehkui:defense", "pehkui:knockback",
            "pehkui:projectile_attack", "pehkui:projectile_knockback",
            // mobilidade
            "pehkui:motion", "pehkui:jump", "pehkui:step_height",
            "pehkui:flight", "pehkui:swim_speed",
            // física
            "pehkui:gravity", "pehkui:fall_damage",
            // itens / projéteis
            "pehkui:held_item", "pehkui:held_item_x", "pehkui:held_item_y", "pehkui:held_item_z",
            "pehkui:projectile", "pehkui:projectile_x", "pehkui:projectile_y", "pehkui:projectile_z",
            // recursos
            "pehkui:health", "pehkui:drops", "pehkui:experience_dropped",
            "pehkui:held_item_use_duration"
    );

    private final PehkuiPresetRepository presets;
    private final ModBridgeClient mod;

    public PehkuiController(PehkuiPresetRepository presets, ModBridgeClient mod) {
        this.presets = presets;
        this.mod = mod;
    }

    @GetMapping("/types")
    public Map<String, Object> types() {
        return Map.of("types", PEHKUI_TYPES);
    }

    /**
     * Players online com matter info. Frontend usa pra montar a sidebar.
     * Retorna combinação de /api/players + /api/matter/{uuid} pra cada um.
     */
    @GetMapping("/eligible")
    public Map<String, Object> eligible(@RequestParam(required = false, defaultValue = "0") double minDm) {
        try {
            // /api/players do mod retorna {"players":[...]} — objeto envelopando array,
            // NÃO array direto. Tem que entrar no .path("players").
            JsonNode resp = mod.getPlayers();
            JsonNode players = resp == null ? null : resp.path("players");
            List<Map<String, Object>> out = new ArrayList<>();
            if (players != null && players.isArray()) {
                for (JsonNode p : players) {
                    String uuid = p.path("uuid").asText("");
                    String name = p.path("name").asText("?");
                    if (uuid.isEmpty()) continue;
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("uuid", uuid);
                    row.put("name", name);
                    row.put("dimension", p.path("dimension").asText(""));
                    row.put("health", p.path("health").asDouble(20));
                    // position é nested em {position:{x,y,z}}, não no root
                    JsonNode pos = p.path("position");
                    row.put("posX", pos.path("x").asDouble(0));
                    row.put("posY", pos.path("y").asDouble(64));
                    row.put("posZ", pos.path("z").asDouble(0));
                    // matter (DM/WM/YM) — opcional, ignora se falhar
                    double dm = 0, wm = 0, ym = 0;
                    try {
                        JsonNode m = mod.getMatter(uuid);
                        if (m != null) {
                            dm = m.path("dm").asDouble(0);
                            wm = m.path("wm").asDouble(0);
                            ym = m.path("ym").asDouble(0);
                        }
                    } catch (Exception ignored) {}
                    row.put("dm", dm);
                    row.put("wm", wm);
                    row.put("ym", ym);
                    if (dm >= minDm) out.add(row);
                }
            }
            // Ordena por DM desc — quem tem mais DM aparece primeiro
            out.sort((a, b) -> Double.compare(
                    ((Number) b.get("dm")).doubleValue(),
                    ((Number) a.get("dm")).doubleValue()));
            return Map.of("players", out, "count", out.size());
        } catch (Exception e) {
            LOG.warn("[Pehkui] eligible falhou: {}", e.getMessage());
            return Map.of("players", List.of(), "count", 0, "error", e.getMessage());
        }
    }

    public record ScaleRequest(
            String playerName, String scaleType, double scale,
            boolean persistent, int delayTicks) {}

    /**
     * Aplica 1 escala em 1 player. Usa o selector @e[type=player,name=...] que é mais
     * robusto que só o nome — funciona mesmo com nomes com caracteres especiais,
     * e o Pehkui aceita NPCs (LNPCXX) que não passam por @p/@a vanilla.
     */
    @PostMapping("/scale")
    public Map<String, Object> scale(@RequestBody ScaleRequest req) {
        if (req.playerName == null || req.playerName.isBlank())
            return Map.of("ok", false, "error", "playerName missing");
        if (!PEHKUI_TYPES.contains(req.scaleType))
            return Map.of("ok", false, "error", "scaleType inválido: " + req.scaleType);

        String target = scaleTarget(req.playerName);
        String cmd;
        if (req.delayTicks > 0) {
            cmd = String.format(Locale.US, "scale set_delayed %s %.4f %d %s",
                    req.scaleType, req.scale, req.delayTicks, target);
        } else {
            // Pehkui 3.8.2 (versão do servidor) NÃO TEM `set_persistent` — esse
            // subcomando foi removido em versões antigas. `scale set` JÁ É
            // persistente no Pehkui 3.x — sobrevive a relog porque é armazenado
            // no NBT do player automaticamente.
            // Antes: o frontend mandava persistent=true → backend usava set_persistent
            // → mod retornava OK silencioso (result:0) → nada acontecia in-game.
            cmd = String.format(Locale.US, "scale set %s %.4f %s",
                    req.scaleType, req.scale, target);
        }
        LOG.info("[Pehkui] /scale → {}", cmd);
        JsonNode result = mod.runCommand(cmd);
        // result:1 = comando aplicou em 1 entity. result:0 = nada afetado (comando
        // pode estar errado OU player offline OU scaleType inválido).
        return Map.of("ok", true, "cmd", cmd, "modResult", result == null ? "null" : result.toString());
    }

    public record BulkRequest(String playerName, Map<String, Double> scales, boolean persistent) {}

    /** Aplica várias escalas de uma vez no mesmo player. */
    @PostMapping("/bulk")
    public Map<String, Object> bulk(@RequestBody BulkRequest req) {
        if (req.playerName == null || req.playerName.isBlank())
            return Map.of("ok", false, "error", "playerName missing");
        if (req.scales == null || req.scales.isEmpty())
            return Map.of("ok", false, "error", "scales empty");
        String target = scaleTarget(req.playerName);
        List<String> applied = new ArrayList<>();
        for (var e : req.scales.entrySet()) {
            if (!PEHKUI_TYPES.contains(e.getKey())) continue;
            // Sempre `scale set` (set_persistent não existe no Pehkui 3.8.2 — ver fix em /scale)
            String cmd = String.format(Locale.US, "scale set %s %.4f %s",
                    e.getKey(), e.getValue(), target);
            LOG.info("[Pehkui] /bulk → {}", cmd);
            mod.runCommand(cmd);
            applied.add(cmd);
        }
        return Map.of("ok", true, "applied", applied, "count", applied.size());
    }

    /** Reset de 1 tipo ou de TODOS (scaleType=null|"all"). */
    @PostMapping("/reset")
    public Map<String, Object> reset(@RequestBody Map<String, Object> body) {
        String name = body.getOrDefault("playerName", "").toString();
        String type = body.get("scaleType") == null ? null : body.get("scaleType").toString();
        if (name.isBlank()) return Map.of("ok", false, "error", "playerName missing");

        String target = scaleTarget(name);
        if (type == null || "all".equalsIgnoreCase(type)) {
            for (String t : PEHKUI_TYPES) mod.runCommand("scale reset " + t + " " + target);
            return Map.of("ok", true, "resetAll", true, "count", PEHKUI_TYPES.size());
        }
        if (!PEHKUI_TYPES.contains(type))
            return Map.of("ok", false, "error", "scaleType inválido: " + type);
        mod.runCommand("scale reset " + type + " " + target);
        return Map.of("ok", true, "scaleType", type);
    }

    /**
     * Normaliza o target pro comando /scale. Se o nome vier "puro", deixa
     * passar — Pehkui aceita nome literal. Espaços no nome viram problema:
     * em produção players humanos não têm espaço, mas NPCs podem.
     */
    private static String scaleTarget(String name) {
        // Nome literal funciona em 1.20.1 — não precisa @ selector
        // contanto que não tenha espaço.
        return name.trim();
    }

    // =========================================================================
    // PRESETS
    // =========================================================================

    @GetMapping("/presets")
    public List<PehkuiPreset> listPresets() {
        return presets.findAllByOrderByNameAsc();
    }

    @PostMapping("/presets")
    public PehkuiPreset createPreset(@RequestBody PehkuiPreset p) {
        if (p.getName() == null || p.getName().isBlank())
            throw new IllegalArgumentException("nome obrigatório");
        return presets.save(p);
    }

    @PutMapping("/presets/{id}")
    public ResponseEntity<PehkuiPreset> updatePreset(@PathVariable Long id, @RequestBody PehkuiPreset body) {
        return presets.findById(id).map(p -> {
            if (body.getName() != null) p.setName(body.getName());
            if (body.getEmoji() != null) p.setEmoji(body.getEmoji());
            if (body.getDescription() != null) p.setDescription(body.getDescription());
            if (body.getScalesJson() != null) p.setScalesJson(body.getScalesJson());
            p.setDmCost(body.getDmCost());
            p.setDurationSec(body.getDurationSec());
            return ResponseEntity.ok(presets.save(p));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/presets/{id}")
    public Map<String, Object> deletePreset(@PathVariable Long id) {
        presets.deleteById(id);
        return Map.of("ok", true);
    }

    /** Aplica preset salvo em 1 player. Se durationSec > 0, agenda reset. */
    @PostMapping("/presets/{id}/apply")
    public Map<String, Object> applyPreset(@PathVariable Long id, @RequestBody Map<String, String> body) {
        PehkuiPreset p = presets.findById(id).orElse(null);
        if (p == null) return Map.of("ok", false, "error", "preset não existe");
        String name = body.getOrDefault("playerName", "");
        if (name.isBlank()) return Map.of("ok", false, "error", "playerName missing");

        try {
            JsonNode scales = MAPPER.readTree(p.getScalesJson());
            List<String> applied = new ArrayList<>();
            scales.fields().forEachRemaining(e -> {
                String type = e.getKey();
                double val = e.getValue().asDouble(1.0);
                if (!PEHKUI_TYPES.contains(type)) return;
                // `scale set` (não set_persistent — esse não existe no Pehkui 3.8.2)
                String cmd = String.format(Locale.US, "scale set %s %.4f %s",
                        type, val, name);
                mod.runCommand(cmd);
                applied.add(cmd);
            });

            // Se preset tem duração — agenda reset em N ticks (20 ticks/s)
            if (p.getDurationSec() > 0) {
                int ticks = p.getDurationSec() * 20;
                for (String type : PEHKUI_TYPES) {
                    // Só reseta o que estava no preset (resto não foi mexido)
                    if (scales.has(type)) {
                        mod.runCommand(String.format(Locale.US, "scale set_delayed %s 1.0 %d %s",
                                type, ticks, name));
                    }
                }
            }
            return Map.of("ok", true, "preset", p.getName(), "applied", applied.size(),
                    "duration", p.getDurationSec());
        } catch (Exception e) {
            return Map.of("ok", false, "error", e.getMessage());
        }
    }
}
