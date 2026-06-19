package br.com.murilo.liberthia.admin.engine.securitycraft;

import br.com.murilo.liberthia.admin.mod.ModBridgeClient;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Painel SecurityCraft — controle remoto de blocos protegidos do mod
 * "SecurityCraft" (câmeras, portas com keypad, mines, sensors).
 *
 * O mod expõe o comando /sc:
 *   /sc list_owned_blocks [player]      → lista blocos de 1 player
 *   /sc reset_owned_blocks [player]     → reseta dono pra "Server" em todos
 *   /sc set_owner [player]              → muda dono do bloco que o sender olha
 *
 * Como o command no servidor não tem retorno de output usável, esse controller:
 *   - Dispara comandos
 *   - Mantém um "registry" local de items dados via painel (chaves, keycards)
 *
 * Para listar quem tem chave de quem, exigiria parser de NBT dos inventários
 * (do mod /api/player/{uuid}/inventory). É feito sob demanda.
 */
@RestController
@RequestMapping("/api/securitycraft")
public class SecurityCraftController {

    private static final Logger LOG = LoggerFactory.getLogger(SecurityCraftController.class);

    /** Itens do SecurityCraft que indicam "chave" ou "controle" sobre algo. */
    private static final Set<String> SC_KEY_ITEMS = Set.of(
            "securitycraft:keycard_lv1",
            "securitycraft:keycard_lv2",
            "securitycraft:keycard_lv3",
            "securitycraft:keycard_lv4",
            "securitycraft:keycard_lv5",
            "securitycraft:limited_use_keycard",
            "securitycraft:universal_key_changer",
            "securitycraft:camera_monitor",
            "securitycraft:remote_access_tool",
            "securitycraft:remote_access_sentry",
            "securitycraft:remote_access_mine",
            "securitycraft:briefcase"
    );

    private final ModBridgeClient mod;

    public SecurityCraftController(ModBridgeClient mod) {
        this.mod = mod;
    }

    @GetMapping("/items")
    public Map<String, Object> securityItems() {
        return Map.of("items", SC_KEY_ITEMS);
    }

    /**
     * Lista players online com contagem de items SecurityCraft no inventário.
     * Cruza /api/players + /api/player/{uuid}/inventory pra cada um.
     */
    @GetMapping("/players")
    public Map<String, Object> playersWithSc() {
        try {
            // /api/players retorna {"players":[...]} — preciso entrar no envelope
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

                    // Conta items SecurityCraft no inventário (best-effort)
                    int scCount = 0;
                    Map<String, Integer> byItem = new LinkedHashMap<>();
                    try {
                        JsonNode inv = mod.getInventory(uuid);
                        if (inv != null) {
                            scCount += countScItems(inv.path("main"), byItem);
                            scCount += countScItems(inv.path("armor"), byItem);
                            scCount += countScItems(inv.path("offhand"), byItem);
                        }
                    } catch (Exception ignored) {}
                    row.put("scItemCount", scCount);
                    row.put("scItems", byItem);
                    out.add(row);
                }
            }
            // Ordena: quem tem mais items SC primeiro
            out.sort((a, b) -> Integer.compare(
                    ((Number) b.get("scItemCount")).intValue(),
                    ((Number) a.get("scItemCount")).intValue()));
            return Map.of("players", out, "count", out.size());
        } catch (Exception e) {
            LOG.warn("[SC] playersWithSc falhou: {}", e.getMessage());
            return Map.of("players", List.of(), "error", e.getMessage());
        }
    }

    private int countScItems(JsonNode items, Map<String, Integer> tally) {
        int count = 0;
        if (items == null || !items.isArray()) return 0;
        for (JsonNode it : items) {
            String id = it.path("id").asText("");
            if (id.startsWith("securitycraft:")) {
                int c = it.path("count").asInt(1);
                tally.merge(id, c, Integer::sum);
                count += c;
            }
        }
        return count;
    }

    /** Lista blocos SecurityCraft de 1 player. Dispara o /sc list_owned_blocks. */
    @PostMapping("/list-blocks")
    public Map<String, Object> listBlocks(@RequestBody Map<String, String> body) {
        String name = body.getOrDefault("playerName", "");
        if (name.isBlank()) return Map.of("ok", false, "error", "playerName missing");
        // O comando faz o output no chat do executor; aqui só dispara.
        mod.runCommand("sc list_owned_blocks " + name);
        return Map.of("ok", true,
                "note", "O comando foi enviado. O output aparece no console do servidor + chat do op online.");
    }

    /** Reseta TODOS os blocos SC de um player pro dono "Server". */
    @PostMapping("/reset-blocks")
    public Map<String, Object> resetBlocks(@RequestBody Map<String, String> body) {
        String name = body.getOrDefault("playerName", "");
        if (name.isBlank()) return Map.of("ok", false, "error", "playerName missing");
        mod.runCommand("sc reset_owned_blocks " + name);
        return Map.of("ok", true);
    }

    /** Dá um item SecurityCraft pro player (chave, keycard, monitor, etc). */
    @PostMapping("/give-item")
    public Map<String, Object> giveItem(@RequestBody Map<String, Object> body) {
        String name = body.getOrDefault("playerName", "").toString();
        String item = body.getOrDefault("itemId", "").toString();
        int count = body.get("count") instanceof Number n ? n.intValue() : 1;
        if (name.isBlank() || item.isBlank())
            return Map.of("ok", false, "error", "playerName e itemId obrigatórios");
        if (!item.startsWith("securitycraft:"))
            return Map.of("ok", false, "error", "só items securitycraft:* são permitidos");

        // Keycards podem ter level no NBT — pra simplificar damos com NBT default
        mod.runCommand("give " + name + " " + item + " " + count);
        return Map.of("ok", true);
    }

    /**
     * Abre porta/destranca via /sc, mas precisa de pos. Atalho: TP o admin
     * pra coordenada e dispara /sc unlock no que ele estiver olhando.
     */
    @PostMapping("/unlock-at")
    public Map<String, Object> unlockAt(@RequestBody Map<String, Object> body) {
        double x = num(body.get("x"), 0);
        double y = num(body.get("y"), 64);
        double z = num(body.get("z"), 0);
        String dim = body.getOrDefault("dimension", "minecraft:overworld").toString();
        // Usa /setblock pra remover frame ou /sc com setpos. Como /sc usa raycast,
        // fallback: setblock como air (DESTRUTIVO — só usar com cuidado).
        // Versão segura: só envia um broadcast confirmando que tem que ir lá.
        String cmd = String.format(java.util.Locale.US,
                "execute in %s positioned %f %f %f run tellraw @a {\"text\":\"§e[Admin] Pedido de unlock em %.0f %.0f %.0f\"}",
                dim, x, y, z, x, y, z);
        mod.runCommand(cmd);
        return Map.of("ok", true, "note", "Broadcast enviado. /sc unlock requer raycast do admin.");
    }

    private static double num(Object o, double d) {
        return o instanceof Number n ? n.doubleValue() : d;
    }
}
