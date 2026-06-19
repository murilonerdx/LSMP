package br.com.murilo.liberthia.admin.engine.storage;

import br.com.murilo.liberthia.admin.mod.ModBridgeClient;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Rankings de storage por player. Conta items dos mods de armazenamento
 * (SophisticatedBackpacks, SophisticatedStorage, IronChest, AE2, Tom's Storage)
 * nos inventários dos players online.
 *
 * Limitação: só consegue contar o que está no INVENTÁRIO do player no momento.
 * Pra contar chests no mundo, precisaria scan de chunks — não dá sem mexer
 * no mod. Como compensação, fazemos ranking por backpack/cosmetics carregados.
 */
@RestController
@RequestMapping("/api/storage-rankings")
public class StorageRankingsController {

    private static final Logger LOG = LoggerFactory.getLogger(StorageRankingsController.class);

    /** Prefixos de items de storage que contam pro ranking. */
    private static final List<String> STORAGE_PREFIXES = List.of(
            "sophisticatedbackpacks:",
            "sophisticatedstorage:",
            "ironchest:",
            "ae2:",
            "appliedenergistics2:",
            "toms_storage:"
    );

    private final ModBridgeClient mod;

    public StorageRankingsController(ModBridgeClient mod) {
        this.mod = mod;
    }

    @GetMapping("/top")
    public Map<String, Object> top(@RequestParam(required = false, defaultValue = "10") int limit) {
        try {
            // /api/players retorna {"players":[...]} — entra no envelope
            JsonNode resp = mod.getPlayers();
            JsonNode players = resp == null ? null : resp.path("players");
            List<Map<String, Object>> rows = new ArrayList<>();
            if (players != null && players.isArray()) {
                for (JsonNode p : players) {
                    String uuid = p.path("uuid").asText("");
                    String name = p.path("name").asText("?");
                    if (uuid.isEmpty()) continue;

                    int totalCount = 0;
                    int backpacks = 0;
                    int storage = 0;
                    Map<String, Integer> byItem = new LinkedHashMap<>();
                    try {
                        JsonNode inv = mod.getInventory(uuid);
                        if (inv != null) {
                            totalCount += scanItems(inv.path("main"), byItem);
                            totalCount += scanItems(inv.path("armor"), byItem);
                            totalCount += scanItems(inv.path("offhand"), byItem);
                            // Conta especificamente backpacks/storage
                            backpacks = countWithPrefix(byItem, "sophisticatedbackpacks:");
                            storage = countWithPrefix(byItem, "sophisticatedstorage:");
                        }
                    } catch (Exception ignored) {}

                    if (totalCount == 0) continue; // pula quem não tem nada

                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("uuid", uuid);
                    row.put("name", name);
                    row.put("dimension", p.path("dimension").asText(""));
                    row.put("total", totalCount);
                    row.put("backpacks", backpacks);
                    row.put("storage", storage);
                    row.put("byItem", byItem);
                    rows.add(row);
                }
            }
            rows.sort((a, b) -> Integer.compare(
                    ((Number) b.get("total")).intValue(),
                    ((Number) a.get("total")).intValue()));
            if (rows.size() > limit) rows = rows.subList(0, limit);
            return Map.of("ranking", rows, "count", rows.size());
        } catch (Exception e) {
            LOG.warn("[StorageRanking] top falhou: {}", e.getMessage());
            return Map.of("ranking", List.of(), "error", e.getMessage());
        }
    }

    private int scanItems(JsonNode items, Map<String, Integer> tally) {
        int total = 0;
        if (items == null || !items.isArray()) return 0;
        for (JsonNode it : items) {
            String id = it.path("id").asText("");
            if (matches(id)) {
                int c = it.path("count").asInt(1);
                tally.merge(id, c, Integer::sum);
                total += c;
            }
        }
        return total;
    }

    private boolean matches(String id) {
        for (String prefix : STORAGE_PREFIXES) if (id.startsWith(prefix)) return true;
        return false;
    }

    private int countWithPrefix(Map<String, Integer> byItem, String prefix) {
        return byItem.entrySet().stream()
                .filter(e -> e.getKey().startsWith(prefix))
                .mapToInt(Map.Entry::getValue).sum();
    }
}
