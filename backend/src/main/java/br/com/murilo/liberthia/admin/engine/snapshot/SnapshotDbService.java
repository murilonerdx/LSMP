package br.com.murilo.liberthia.admin.engine.snapshot;

import br.com.murilo.liberthia.admin.engine.EngineActions;
import br.com.murilo.liberthia.admin.mod.ModBridgeClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

@Repository
interface PlayerSnapshotRepository extends JpaRepository<PlayerSnapshot, Long> {
    @Query("SELECT s FROM PlayerSnapshot s WHERE s.uuid = :uuid ORDER BY s.ts DESC")
    List<PlayerSnapshot> findByUuidDesc(@Param("uuid") String uuid);

    @Modifying
    @Transactional
    @Query("DELETE FROM PlayerSnapshot s WHERE s.ts < :cutoff")
    int deleteOld(@Param("cutoff") Instant cutoff);
}

@Service
class SnapshotDbService {
    private static final Logger LOG = LoggerFactory.getLogger(SnapshotDbService.class);
    /** A cada 1 hora. Em ms para deixar explícito o intervalo. */
    private static final long HOURLY_MS = 3_600_000L;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final PlayerSnapshotRepository repo;
    private final ModBridgeClient mod;
    private final EngineActions actions;

    SnapshotDbService(PlayerSnapshotRepository repo, ModBridgeClient mod, EngineActions actions) {
        this.repo = repo;
        this.mod = mod;
        this.actions = actions;
    }

    /**
     * Cron horário: captura snapshot de cada player online e persiste no banco.
     *
     * - `fixedDelay`: 1h após terminar a execução anterior (não 1h fixo entre starts).
     *   Garante que se o fetch demorar, não acumula chamadas.
     * - `initialDelay`: 2min após boot pra dar tempo do mod conectar via SSE
     *   e o cache de players popular.
     *
     * Cada snapshot é JSON completo (inventário + posição + dimensão + nome),
     * armazenado no campo `data_json` da tabela `player_snapshots`. O cleanup
     * diário descarta snapshots > 30 dias.
     */
    @Scheduled(fixedDelay = HOURLY_MS, initialDelay = 120_000L)
    public void autoSnapshot() {
        var players = actions.getPlayers();
        if (players.isEmpty()) {
            LOG.info("[Snapshot] tick horário: 0 players online, nada a fazer");
            return;
        }
        int ok = 0, fail = 0;
        for (var p : players) {
            try {
                JsonNode inv = mod.getInventory(p.uuid());
                if (inv == null) { fail++; continue; }
                String json = buildSnapshotJson(p, inv);
                repo.save(new PlayerSnapshot(p.uuid(), p.name(), json));
                ok++;
            } catch (Exception e) {
                fail++;
                LOG.warn("[Snapshot] {} ({}) falhou: {}", p.name(), p.uuid(), e.getMessage());
            }
        }
        LOG.info("[Snapshot] tick horário concluído: {} ok, {} fail (de {} online)", ok, fail, players.size());
    }

    private String buildSnapshotJson(EngineActions.PlayerInfo p, JsonNode inv) {
        ObjectNode root = MAPPER.createObjectNode();
        root.put("uuid", p.uuid());
        root.put("name", p.name());
        root.put("ts", Instant.now().toString());
        root.put("dimension", p.dimension());
        ObjectNode pos = root.putObject("position");
        pos.put("x", p.x());
        pos.put("y", p.y());
        pos.put("z", p.z());
        root.set("inventory", inv);
        return root.toString();
    }

    /** Cleanup diário às 4h: descarta snapshots > 30 dias. */
    @Scheduled(cron = "0 0 4 * * *")
    public void cleanup() {
        try {
            int removed = repo.deleteOld(Instant.now().minus(30, ChronoUnit.DAYS));
            if (removed > 0) LOG.info("[Snapshot] cleanup removeu {} snapshots antigos (>30d)", removed);
        } catch (Exception e) {
            LOG.warn("[Snapshot] cleanup falhou: {}", e.getMessage());
        }
    }

    public List<PlayerSnapshot> list(String uuid) { return repo.findByUuidDesc(uuid); }
    public PlayerSnapshot get(Long id) { return repo.findById(id).orElse(null); }

    public PlayerSnapshot runNow(String uuid) {
        var p = actions.findPlayer(uuid);
        if (p == null) throw new IllegalArgumentException("player offline");
        try {
            JsonNode inv = mod.getInventory(uuid);
            String json = buildSnapshotJson(p, inv);
            PlayerSnapshot s = repo.save(new PlayerSnapshot(uuid, p.name(), json));
            LOG.info("[Snapshot] manual run-now: {} → snapshot #{}", p.name(), s.getId());
            return s;
        } catch (Exception e) {
            throw new RuntimeException("snapshot fail: " + e.getMessage(), e);
        }
    }

    /** Snapshot de todos os players online sob demanda (botão "snapshot all"). */
    public int runAllNow() {
        var players = actions.getPlayers();
        int ok = 0;
        for (var p : players) {
            try { runNow(p.uuid()); ok++; }
            catch (Exception ignored) {}
        }
        LOG.info("[Snapshot] runAllNow manual: {}/{}", ok, players.size());
        return ok;
    }
}

@RestController
@RequestMapping("/api/snapshot-db")
class SnapshotDbController {

    private final SnapshotDbService svc;
    SnapshotDbController(SnapshotDbService svc) { this.svc = svc; }

    @GetMapping("/list/{uuid}")
    public Map<String, Object> list(@PathVariable String uuid) {
        return Map.of("snapshots", svc.list(uuid));
    }

    @GetMapping("/get/{id}")
    public PlayerSnapshot get(@PathVariable Long id) { return svc.get(id); }

    @PostMapping("/run-now/{uuid}")
    public PlayerSnapshot runNow(@PathVariable String uuid) { return svc.runNow(uuid); }

    /** Força snapshot manual de todos os players online (não espera o tick horário). */
    @PostMapping("/run-now-all")
    public Map<String, Object> runAllNow() {
        return Map.of("ok", true, "count", svc.runAllNow());
    }
}
