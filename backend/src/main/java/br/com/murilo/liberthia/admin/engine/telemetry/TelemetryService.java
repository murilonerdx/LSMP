package br.com.murilo.liberthia.admin.engine.telemetry;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cache em memória dos snapshots de telemetria recebidos do mod.
 *
 * Por que NÃO persiste em DB:
 *  - Dado é volátil — chega a cada 10s do mod, sobrescreve o anterior.
 *  - Histórico longo já fica no mod em /world/liberthia/telemetry/*.jsonl
 *  - Frontend só precisa do estado atual + curto histórico em memória.
 *
 * TTL: snapshot fica "stale" depois de 30s sem update do mod (player saiu
 * do server ou mod desligou). Marca offline mas não remove do cache —
 * frontend pode mostrar "última vez visto há X" sem refresh hard.
 *
 * Config global ligado/desligado:
 *  - Lido/escrito via setEnabled() — propagado pro mod no próximo push
 *    (não força, é hint).
 */
@Service
public class TelemetryService {

    /** TTL antes de marcar player como "offline/stale" (ms). */
    private static final long STALE_TTL_MS = 30_000;
    /** Remove total do cache depois de X ms. */
    private static final long CLEANUP_TTL_MS = 5 * 60_000;

    private final Map<String, Map<String, Object>> snapshots = new ConcurrentHashMap<>();
    private volatile boolean enabled = true;
    private volatile long lastBatchReceived = 0;
    private volatile int lastBatchSize = 0;

    /** Recebe um batch do mod e atualiza o cache. */
    public synchronized void ingestBatch(List<Map<String, Object>> snaps) {
        lastBatchReceived = System.currentTimeMillis();
        lastBatchSize = snaps == null ? 0 : snaps.size();
        if (snaps == null) return;
        for (Map<String, Object> snap : snaps) {
            String uuid = String.valueOf(snap.get("uuid"));
            if (uuid == null || uuid.isBlank()) continue;
            snapshots.put(uuid, snap);
        }
        // Limpeza: remove snapshots muito antigos
        long cutoff = System.currentTimeMillis() - CLEANUP_TTL_MS;
        snapshots.entrySet().removeIf(e -> {
            Object lu = e.getValue().get("lastUpdate");
            if (!(lu instanceof Number n)) return true;
            return n.longValue() < cutoff;
        });
    }

    /** Lista todos os players com snapshot — marca staleness baseado em TTL. */
    public List<Map<String, Object>> listPlayers() {
        long now = System.currentTimeMillis();
        List<Map<String, Object>> out = new ArrayList<>(snapshots.size());
        for (Map<String, Object> snap : snapshots.values()) {
            Map<String, Object> copy = new LinkedHashMap<>(snap);
            Object lu = snap.get("lastUpdate");
            long age = now - (lu instanceof Number n ? n.longValue() : 0);
            copy.put("ageMs", age);
            copy.put("stale", age > STALE_TTL_MS);
            out.add(copy);
        }
        // Ordena: mais recentes primeiro
        out.sort((a, b) -> {
            long la = a.get("lastUpdate") instanceof Number n ? n.longValue() : 0;
            long lb = b.get("lastUpdate") instanceof Number n ? n.longValue() : 0;
            return Long.compare(lb, la);
        });
        return out;
    }

    public Map<String, Object> getPlayer(String uuid) {
        Map<String, Object> snap = snapshots.get(uuid);
        if (snap == null) return null;
        long now = System.currentTimeMillis();
        Map<String, Object> copy = new LinkedHashMap<>(snap);
        Object lu = snap.get("lastUpdate");
        long age = now - (lu instanceof Number n ? n.longValue() : 0);
        copy.put("ageMs", age);
        copy.put("stale", age > STALE_TTL_MS);
        return copy;
    }

    public Map<String, Object> status() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("enabled", enabled);
        m.put("playerCount", snapshots.size());
        m.put("lastBatchReceived", lastBatchReceived);
        m.put("lastBatchSize", lastBatchSize);
        m.put("ageOfLastBatchMs", lastBatchReceived == 0 ? -1 : System.currentTimeMillis() - lastBatchReceived);
        m.put("ts", System.currentTimeMillis());
        return m;
    }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean v) { this.enabled = v; }
}
