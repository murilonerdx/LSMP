package br.com.murilo.liberthia.admin.engine.autogift;

import br.com.murilo.liberthia.admin.mod.ModBridgeClient;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Calcula rankings de players pra cada métrica suportada pelo AutoGift.
 *
 * Métricas implementadas:
 *   VOICE_DURATION_MS  — soma de durationMs por player (VoiceClip)
 *   VOICE_CLIPS        — count de clips por player
 *   VOICE_BYTES        — soma de bytes
 *   CHAT_MESSAGES      — count de ChatLog por player
 *   INVENTORY_ITEMS    — total de items no inventário (cross com mod, só online)
 *   DEATHS             — count de DeathLog
 *   ITEMS_GIVEN        — count de CommandLog que tem "give "
 *
 * Cada `rank` retorna List<Entry> com {playerUuid, playerName, value}, ordenado desc.
 */
@Service
public class AutoGiftMetricService {

    private static final Logger LOG = LoggerFactory.getLogger(AutoGiftMetricService.class);

    @PersistenceContext
    private EntityManager em;

    private final ModBridgeClient mod;

    public AutoGiftMetricService(ModBridgeClient mod) {
        this.mod = mod;
    }

    public record Entry(String playerUuid, String playerName, long value) {}

    /** Retorna top N players da métrica especificada. */
    public List<Entry> rank(String metric, int topN, long minValue) {
        List<Entry> all = switch (metric == null ? "" : metric.toUpperCase()) {
            case "VOICE_DURATION_MS" -> rankSql(
                    "SELECT player_uuid, MAX(player_name), COALESCE(SUM(duration_ms),0) " +
                            "FROM voice_clips WHERE upload_status='DONE' " +
                            "GROUP BY player_uuid ORDER BY 3 DESC");
            case "VOICE_CLIPS" -> rankSql(
                    "SELECT player_uuid, MAX(player_name), COUNT(*) FROM voice_clips " +
                            "WHERE upload_status='DONE' GROUP BY player_uuid ORDER BY 3 DESC");
            case "VOICE_BYTES" -> rankSql(
                    "SELECT player_uuid, MAX(player_name), COALESCE(SUM(size_bytes),0) " +
                            "FROM voice_clips WHERE upload_status='DONE' " +
                            "GROUP BY player_uuid ORDER BY 3 DESC");
            case "CHAT_MESSAGES" -> rankSql(
                    "SELECT uuid, MAX(name), COUNT(*) FROM chat_logs " +
                            "WHERE uuid IS NOT NULL AND uuid <> '' " +
                            "GROUP BY uuid ORDER BY 3 DESC");
            case "DEATHS" -> rankSql(
                    "SELECT player_uuid, MAX(player_name), COUNT(*) FROM memorials " +
                            "WHERE player_uuid IS NOT NULL GROUP BY player_uuid ORDER BY 3 DESC");
            case "INVENTORY_ITEMS" -> rankInventoryItems();
            default -> List.of();
        };
        return all.stream()
                .filter(e -> e.value() >= minValue)
                .limit(Math.max(1, topN))
                .toList();
    }

    @SuppressWarnings("unchecked")
    private List<Entry> rankSql(String sql) {
        try {
            List<Object[]> rows = em.createNativeQuery(sql).getResultList();
            List<Entry> out = new ArrayList<>();
            for (Object[] r : rows) {
                String uuid = r[0] == null ? "" : r[0].toString();
                String name = r[1] == null ? uuid : r[1].toString();
                long value = r[2] == null ? 0 : ((Number) r[2]).longValue();
                if (uuid.isBlank()) continue;
                out.add(new Entry(uuid, name, value));
            }
            return out;
        } catch (Exception e) {
            LOG.warn("[AutoGift] rankSql falhou: {}", e.getMessage());
            return List.of();
        }
    }

    /** Inventory items — cross com mod, só players ONLINE no momento. */
    private List<Entry> rankInventoryItems() {
        try {
            JsonNode resp = mod.getPlayers();
            JsonNode players = resp == null ? null : resp.path("players");
            if (players == null || !players.isArray()) return List.of();
            List<Entry> out = new ArrayList<>();
            for (JsonNode p : players) {
                String uuid = p.path("uuid").asText("");
                String name = p.path("name").asText("?");
                if (uuid.isEmpty()) continue;
                long total = 0;
                try {
                    JsonNode inv = mod.getInventory(uuid);
                    if (inv != null) {
                        total += countItems(inv.path("main"));
                        total += countItems(inv.path("armor"));
                        total += countItems(inv.path("offhand"));
                    }
                } catch (Exception ignored) {}
                out.add(new Entry(uuid, name, total));
            }
            out.sort((a, b) -> Long.compare(b.value(), a.value()));
            return out;
        } catch (Exception e) {
            LOG.warn("[AutoGift] rankInventoryItems falhou: {}", e.getMessage());
            return List.of();
        }
    }

    private long countItems(JsonNode items) {
        if (items == null || !items.isArray()) return 0;
        long total = 0;
        for (JsonNode it : items) total += it.path("count").asInt(0);
        return total;
    }

    public static List<Map<String, Object>> describeMetrics() {
        return List.of(
                Map.of("id", "VOICE_DURATION_MS", "label", "🎙 Voice — duração total",
                        "desc", "Soma de ms falados", "unit", "ms"),
                Map.of("id", "VOICE_CLIPS", "label", "🎙 Voice — qtd de clipes",
                        "desc", "Quantos clipes o player gravou", "unit", "clipes"),
                Map.of("id", "VOICE_BYTES", "label", "🎙 Voice — bytes salvos",
                        "desc", "Tamanho total dos áudios", "unit", "bytes"),
                Map.of("id", "CHAT_MESSAGES", "label", "💬 Chat — mensagens",
                        "desc", "Quantas mensagens no chat", "unit", "msgs"),
                Map.of("id", "DEATHS", "label", "💀 Mortes — quantas vezes morreu",
                        "desc", "Conta a partir dos memorials", "unit", "mortes"),
                Map.of("id", "INVENTORY_ITEMS", "label", "🎒 Inventário — total de items",
                        "desc", "Soma de items no inventário (só players online)", "unit", "items")
        );
    }
}
