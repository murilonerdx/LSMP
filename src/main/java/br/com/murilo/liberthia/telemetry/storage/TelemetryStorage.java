package br.com.murilo.liberthia.telemetry.storage;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.telemetry.events.PlayerEvent;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.server.MinecraftServer;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Persistência assíncrona de eventos em JSON Lines (JSONL).
 *
 * NÃO bloqueia o servidor — eventos são enfileirados na BlockingQueue e
 * um worker thread daemon faz flush em batch (~500 eventos ou 5s).
 *
 * Estrutura no disco:
 *   <world>/liberthia/telemetry/<yyyy-MM-dd>/<player-uuid>.jsonl
 *
 * Um arquivo por player POR DIA — facilita rotação/limpeza. Cada linha é
 * um evento JSON completo. Append-only. Sem schema migration drama.
 *
 * Por que JSONL (não SQL):
 *  - Append é O(1) sem locking de tabela
 *  - Eventos têm shape variável (data map) — flat schema seria perda
 *  - Pra análise offline, jq/spark/duckdb lêem JSONL nativamente
 *  - Quando precisar SQL, é trivial fazer um job de ingest
 */
public class TelemetryStorage {

    private static final int BATCH_SIZE = 500;
    private static final long FLUSH_INTERVAL_MS = 5_000;
    private static final int QUEUE_CAPACITY = 50_000;
    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final Path baseDir;
    private final BlockingQueue<PlayerEvent> queue = new ArrayBlockingQueue<>(QUEUE_CAPACITY);
    private final Gson gson = new GsonBuilder().disableHtmlEscaping().create();
    private final Thread worker;
    private volatile boolean running = true;
    private long droppedEvents = 0;

    public TelemetryStorage(MinecraftServer server) {
        this.baseDir = server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT)
                .resolve("liberthia").resolve("telemetry");
        try {
            Files.createDirectories(baseDir);
        } catch (IOException e) {
            LiberthiaMod.LOGGER.warn("[Telemetry] falha ao criar dir {}: {}", baseDir, e.getMessage());
        }
        this.worker = new Thread(this::workerLoop, "liberthia-telemetry-flush");
        this.worker.setDaemon(true);
        this.worker.start();
        LiberthiaMod.LOGGER.info("[Telemetry] storage iniciado em {}", baseDir);
    }

    /**
     * Enfileira evento. Se a fila tá cheia, descarta (não bloqueia o thread
     * que chamou — pode ser main thread do MC). Telemetria preferimos perder
     * eventos a travar o server.
     */
    public void enqueue(PlayerEvent e) {
        if (e == null || !running) return;
        if (!queue.offer(e)) {
            droppedEvents++;
            if (droppedEvents % 1000 == 1) {
                LiberthiaMod.LOGGER.warn("[Telemetry] fila cheia, {} eventos descartados no total", droppedEvents);
            }
        }
    }

    public void shutdown() {
        running = false;
        worker.interrupt();
        try {
            worker.join(2000);
        } catch (InterruptedException ignored) {}
        // Flush final
        try { flushBatch(drainAll()); } catch (Exception ignored) {}
        LiberthiaMod.LOGGER.info("[Telemetry] storage encerrado");
    }

    private void workerLoop() {
        List<PlayerEvent> batch = new ArrayList<>(BATCH_SIZE);
        while (running) {
            try {
                // Espera o primeiro evento (ou interrupt)
                PlayerEvent first = queue.poll(FLUSH_INTERVAL_MS, java.util.concurrent.TimeUnit.MILLISECONDS);
                if (first != null) {
                    batch.add(first);
                    queue.drainTo(batch, BATCH_SIZE - 1);
                }
                if (!batch.isEmpty()) {
                    flushBatch(batch);
                    batch.clear();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                LiberthiaMod.LOGGER.warn("[Telemetry] worker loop error: {}", e.getMessage());
                try { Thread.sleep(1000); } catch (InterruptedException ignored) { break; }
            }
        }
    }

    private List<PlayerEvent> drainAll() {
        List<PlayerEvent> out = new ArrayList<>(queue.size());
        queue.drainTo(out);
        return out;
    }

    private void flushBatch(List<PlayerEvent> batch) {
        // Agrupa por (dia, player) — minimiza opens de arquivo
        Map<String, List<PlayerEvent>> grouped = new HashMap<>();
        for (PlayerEvent e : batch) {
            String day = LocalDate.ofInstant(
                    java.time.Instant.ofEpochMilli(e.getTimestamp()), ZoneId.systemDefault()
            ).format(DAY_FMT);
            String key = day + "/" + e.getPlayerUuid();
            grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(e);
        }

        for (Map.Entry<String, List<PlayerEvent>> entry : grouped.entrySet()) {
            String[] parts = entry.getKey().split("/", 2);
            String day = parts[0];
            String uuid = parts[1];
            Path dir = baseDir.resolve(day);
            try {
                Files.createDirectories(dir);
            } catch (IOException ignored) {}
            Path file = dir.resolve(uuid + ".jsonl");
            try (BufferedWriter w = Files.newBufferedWriter(file,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
                for (PlayerEvent e : entry.getValue()) {
                    w.write(gson.toJson(e.toMap()));
                    w.write('\n');
                }
            } catch (IOException ex) {
                LiberthiaMod.LOGGER.warn("[Telemetry] falha ao escrever {}: {}", file, ex.getMessage());
            }
        }
    }

    public int queueSize() { return queue.size(); }
    public long droppedEvents() { return droppedEvents; }
}
