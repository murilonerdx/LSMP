package br.com.murilo.liberthia.admin.engine.snapshot;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Endpoint pra ver players por frequência de visita. Usa os snapshots horários
 * (tabela player_snapshots) pra inferir presença ao longo do tempo.
 *
 * Métricas calculadas:
 *   - totalSnapshots: # de snapshots horários (≈ horas jogadas)
 *   - distinctDays: # de dias únicos em que apareceu
 *   - estimatedSessions: gap > 2h = nova sessão (heurística)
 *   - firstSeenTs / lastSeenTs: range de presença
 *   - daysSinceLastSeen: quantos dias sem aparecer
 *
 * IMPORTANTE: como snapshots são horários, há margem de erro de ±30min por
 * sessão. Players que entram e saem em < 1h podem não aparecer no snapshot
 * (azar). Ainda assim, é a melhor estimativa que temos sem hook de login
 * dedicado no mod.
 */
@RestController
@RequestMapping("/api/players/visits")
public class PlayerVisitsController {

    private final JdbcTemplate jdbc;

    public PlayerVisitsController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Lista TODOS os players já vistos, com estatísticas de visita.
     * Sort: rare (poucos snapshots) | active (muitos) | recent (lastSeen)
     *       | inactive (longe sem ver) | newest (firstSeen recente).
     */
    @GetMapping
    public Map<String, Object> list(
            @RequestParam(defaultValue = "rare") String sort,
            @RequestParam(defaultValue = "500") int limit) {

        // SQL agregando por player. Heurística de "sessions": cada gap > 7200s
        // (2h) entre 2 snapshots conta como nova sessão. Como snapshots são
        // a cada 1h, se a pessoa joga 3h direto, há 3 snapshots seguidos.
        // Pra calcular sessions usa window function (LAG).
        //
        // Usamos timestamptz extract pra epoch — funciona em Postgres.
        String sql = """
            WITH ranked AS (
                SELECT
                    uuid, name, ts,
                    EXTRACT(EPOCH FROM ts) AS ts_epoch,
                    LAG(EXTRACT(EPOCH FROM ts)) OVER (PARTITION BY uuid ORDER BY ts) AS prev_epoch
                FROM player_snapshots
            ),
            sessioned AS (
                SELECT
                    uuid, name, ts, ts_epoch,
                    CASE WHEN prev_epoch IS NULL OR (ts_epoch - prev_epoch) > 7200
                         THEN 1 ELSE 0 END AS new_session
                FROM ranked
            )
            SELECT
                uuid,
                MAX(name) AS name,
                COUNT(*) AS total_snapshots,
                COUNT(DISTINCT DATE(ts)) AS distinct_days,
                SUM(new_session) AS estimated_sessions,
                MIN(EXTRACT(EPOCH FROM ts) * 1000)::bigint AS first_seen_ts,
                MAX(EXTRACT(EPOCH FROM ts) * 1000)::bigint AS last_seen_ts
            FROM sessioned
            GROUP BY uuid
            """;

        List<Map<String, Object>> rows = jdbc.queryForList(sql);

        // Calcula daysSinceLastSeen no Java
        long now = System.currentTimeMillis();
        for (Map<String, Object> r : rows) {
            long last = ((Number) r.get("last_seen_ts")).longValue();
            long daysAgo = (now - last) / (24L * 60 * 60 * 1000);
            r.put("days_since_last_seen", daysAgo);
            // Categoria pra UI
            long total = ((Number) r.get("total_snapshots")).longValue();
            String tier;
            if (total <= 3) tier = "rare";          // 1-3h total = visitante raro
            else if (total <= 12) tier = "casual";   // 4-12h
            else if (total <= 50) tier = "regular";  // 13-50h
            else tier = "veteran";                   // 50h+
            r.put("tier", tier);
        }

        // Ordenação
        Comparator<Map<String, Object>> cmp = switch (sort) {
            case "active" -> Comparator.comparingLong(
                    (Map<String, Object> m) -> ((Number) m.get("total_snapshots")).longValue()).reversed();
            case "recent" -> Comparator.comparingLong(
                    (Map<String, Object> m) -> ((Number) m.get("last_seen_ts")).longValue()).reversed();
            case "inactive" -> Comparator.comparingLong(
                    (Map<String, Object> m) -> ((Number) m.get("days_since_last_seen")).longValue()).reversed();
            case "newest" -> Comparator.comparingLong(
                    (Map<String, Object> m) -> ((Number) m.get("first_seen_ts")).longValue()).reversed();
            default /* rare */ -> Comparator.comparingLong(
                    (Map<String, Object> m) -> ((Number) m.get("total_snapshots")).longValue());
        };
        rows.sort(cmp);
        if (rows.size() > limit) rows = rows.subList(0, limit);

        // Stats agregadas
        long totalPlayers = rows.size();
        long rareCount = rows.stream().filter(r -> "rare".equals(r.get("tier"))).count();
        long casualCount = rows.stream().filter(r -> "casual".equals(r.get("tier"))).count();
        long regularCount = rows.stream().filter(r -> "regular".equals(r.get("tier"))).count();
        long veteranCount = rows.stream().filter(r -> "veteran".equals(r.get("tier"))).count();

        return Map.of(
                "players", rows,
                "count", totalPlayers,
                "sort", sort,
                "stats", Map.of(
                        "rare", rareCount,
                        "casual", casualCount,
                        "regular", regularCount,
                        "veteran", veteranCount
                )
        );
    }

    /** Detalhes de UM player — todas as datas que apareceu. */
    @GetMapping("/{uuid}")
    public Map<String, Object> detail(@PathVariable String uuid) {
        String sql = """
            SELECT
                EXTRACT(EPOCH FROM ts) * 1000 AS ts_ms,
                ts,
                name
            FROM player_snapshots
            WHERE uuid = ?
            ORDER BY ts DESC
            LIMIT 1000
            """;
        List<Map<String, Object>> snaps = jdbc.queryForList(sql, uuid);
        return Map.of(
                "uuid", uuid,
                "snapshots", snaps,
                "count", snaps.size()
        );
    }
}
