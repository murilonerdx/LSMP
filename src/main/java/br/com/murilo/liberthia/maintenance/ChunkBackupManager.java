package br.com.murilo.liberthia.maintenance;

import br.com.murilo.liberthia.LiberthiaMod;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

/**
 * Sistema de backup/restauração de chunks + desligamento gracioso do servidor.
 *
 * <p>Motivação: chunks corrompidas quebrando o mapa. Este manager:
 * <ul>
 *   <li><b>Backup datado</b>: salva o mundo e copia TODOS os {@code .mca}
 *       (region/entities/poi de todas as dimensões) pra
 *       {@code <serverDir>/liberthia_chunk_backups/<data>/}, espelhando os
 *       caminhos relativos ao mundo.</li>
 *   <li><b>Desligamento gracioso</b>: salva → backup → avisa com contagem
 *       (proporcional ao nº de players) → kicka todos → espera 10s → para o
 *       servidor (halt). O relançamento é manual (ou via wrapper opcional).</li>
 *   <li><b>Restauração segura</b>: o comando só AGENDA quais region restaurar;
 *       a troca dos {@code .mca} acontece em {@link #applyPendingRestores} no
 *       {@code ServerAboutToStartEvent}, ANTES do mundo carregar — quando os
 *       arquivos não estão abertos/locked. Faz cópia de segurança do arquivo
 *       atual (mesmo corrompido) antes de sobrescrever.</li>
 * </ul>
 *
 * <p>Identificador de uma region = seu caminho relativo ao mundo, normalizado
 * com {@code /}: ex. {@code region/r.0.0.mca}, {@code DIM-1/region/r.-1.0.mca},
 * {@code dimensions/liberthia/spirit_world/region/r.0.0.mca}.
 */
public final class ChunkBackupManager {

    private ChunkBackupManager() {}

    public static final String BACKUP_DIR = "liberthia_chunk_backups";
    public static final String PENDING_FILE = "liberthia_pending_restores.txt";
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    private static final AtomicBoolean SHUTDOWN_IN_PROGRESS = new AtomicBoolean(false);

    /** Resultado de um backup (nome da pasta + nº de .mca copiados). */
    public record BackupResult(String name, int files, boolean ok) {}

    // ───────────────────────── paths ─────────────────────────

    public static Path worldRoot(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT);
    }

    /** Diretório do servidor (pai do mundo). Onde ficam backups + pending file. */
    public static Path serverRoot(MinecraftServer server) {
        Path wr = worldRoot(server);
        Path parent = wr.getParent();
        return parent != null ? parent : wr;
    }

    public static Path backupRoot(MinecraftServer server) {
        return serverRoot(server).resolve(BACKUP_DIR);
    }

    public static Path pendingFile(MinecraftServer server) {
        return serverRoot(server).resolve(PENDING_FILE);
    }

    // ───────────────────────── backup ─────────────────────────

    /**
     * Copia todos os {@code .mca} do mundo pra uma pasta datada. NÃO salva o
     * mundo — quem chama deve ter feito {@code saveEverything} antes (pra os
     * arquivos em disco estarem atualizados). IO puro (pode rodar fora da main).
     */
    public static BackupResult createBackup(MinecraftServer server) {
        Path wr = worldRoot(server);
        String name = LocalDateTime.now().format(TS);
        Path dest = backupRoot(server).resolve(name);
        final int[] count = {0};
        try {
            Files.createDirectories(dest);
            try (Stream<Path> walk = Files.walk(wr)) {
                walk.filter(p -> p.toString().endsWith(".mca") && Files.isRegularFile(p))
                        .forEach(mca -> {
                            try {
                                Path rel = wr.relativize(mca);
                                Path target = dest.resolve(rel.toString());
                                Files.createDirectories(target.getParent());
                                Files.copy(mca, target,
                                        StandardCopyOption.REPLACE_EXISTING,
                                        StandardCopyOption.COPY_ATTRIBUTES);
                                count[0]++;
                            } catch (IOException e) {
                                LiberthiaMod.LOGGER.warn("[ChunkBackup] falha ao copiar {}: {}", mca, e.toString());
                            }
                        });
            }
        } catch (IOException e) {
            LiberthiaMod.LOGGER.error("[ChunkBackup] backup '{}' falhou: {}", name, e.toString());
            return new BackupResult(name, count[0], false);
        }
        LiberthiaMod.LOGGER.info("[ChunkBackup] backup '{}' criado com {} arquivos .mca em {}", name, count[0], dest);
        return new BackupResult(name, count[0], true);
    }

    /** Lista as pastas de backup (mais recentes primeiro). */
    public static List<String> listBackups(MinecraftServer server) {
        Path root = backupRoot(server);
        if (!Files.isDirectory(root)) return List.of();
        try (Stream<Path> s = Files.list(root)) {
            return s.filter(Files::isDirectory)
                    .map(p -> p.getFileName().toString())
                    .filter(n -> !n.startsWith("_pre_restore_"))
                    .sorted(Comparator.reverseOrder())
                    .toList();
        } catch (IOException e) {
            return List.of();
        }
    }

    /** Lista os caminhos relativos dos .mca de um backup (ex. {@code region/r.0.0.mca}). */
    public static List<String> listRegions(MinecraftServer server, String backup) {
        Path dir = backupRoot(server).resolve(backup);
        if (!Files.isDirectory(dir)) return List.of();
        try (Stream<Path> s = Files.walk(dir)) {
            return s.filter(p -> p.toString().endsWith(".mca") && Files.isRegularFile(p))
                    .map(p -> dir.relativize(p).toString().replace('\\', '/'))
                    .sorted()
                    .toList();
        } catch (IOException e) {
            return List.of();
        }
    }

    // ───────────────────── restauração (agendada) ─────────────────────

    /** Agenda a restauração de uma region (aplica no próximo boot). */
    public static boolean scheduleRestore(MinecraftServer server, String backup, String relPath) {
        String rel = relPath.replace('\\', '/');
        Path src = backupRoot(server).resolve(backup).resolve(rel);
        if (!Files.isRegularFile(src)) return false;
        String line = backup + "|" + rel;
        try {
            List<String> existing = readPendingLines(server);
            if (!existing.contains(line)) {
                Files.createDirectories(pendingFile(server).getParent());
                Files.write(pendingFile(server), List.of(line), StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            }
            return true;
        } catch (IOException e) {
            LiberthiaMod.LOGGER.error("[ChunkBackup] scheduleRestore falhou: {}", e.toString());
            return false;
        }
    }

    /** Linhas cruas do pending file ({@code backup|rel}). */
    public static List<String> readPendingLines(MinecraftServer server) {
        Path f = pendingFile(server);
        if (!Files.isRegularFile(f)) return List.of();
        try {
            List<String> out = new ArrayList<>();
            for (String l : Files.readAllLines(f, StandardCharsets.UTF_8)) {
                String t = l.trim();
                if (!t.isEmpty() && !t.startsWith("#")) out.add(t);
            }
            return out;
        } catch (IOException e) {
            return List.of();
        }
    }

    /** Apaga todas as restaurações agendadas. */
    public static boolean clearPending(MinecraftServer server) {
        try {
            return Files.deleteIfExists(pendingFile(server));
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Aplica as restaurações agendadas: copia cada {@code .mca} do backup por
     * cima do arquivo vivo do mundo. Chamado em {@code ServerAboutToStartEvent},
     * antes do mundo carregar. Faz cópia de segurança do arquivo atual antes.
     */
    public static void applyPendingRestores(MinecraftServer server) {
        Path pending = pendingFile(server);
        if (!Files.isRegularFile(pending)) return;
        List<String> lines = readPendingLines(server);
        if (lines.isEmpty()) {
            try { Files.deleteIfExists(pending); } catch (IOException ignored) {}
            return;
        }
        Path wr = worldRoot(server);
        String stamp = LocalDateTime.now().format(TS);
        Path preRestore = backupRoot(server).resolve("_pre_restore_" + stamp);
        int applied = 0, failed = 0;
        for (String line : lines) {
            int sep = line.indexOf('|');
            if (sep < 0) continue;
            String backup = line.substring(0, sep);
            String rel = line.substring(sep + 1);
            Path src = backupRoot(server).resolve(backup).resolve(rel);
            Path dst = wr.resolve(rel);
            try {
                if (!Files.isRegularFile(src)) {
                    failed++;
                    LiberthiaMod.LOGGER.warn("[ChunkBackup] restore: fonte ausente {}", src);
                    continue;
                }
                // cópia de segurança do arquivo ATUAL (mesmo que corrompido) antes de sobrescrever
                if (Files.isRegularFile(dst)) {
                    Path safe = preRestore.resolve(rel);
                    Files.createDirectories(safe.getParent());
                    Files.copy(dst, safe, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
                }
                Files.createDirectories(dst.getParent());
                Files.copy(src, dst, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
                applied++;
                LiberthiaMod.LOGGER.info("[ChunkBackup] restaurado '{}' <- backup '{}'", rel, backup);
            } catch (IOException e) {
                failed++;
                LiberthiaMod.LOGGER.error("[ChunkBackup] restore falhou '{}': {}", rel, e.toString());
            }
        }
        // arquiva o pending file pra não re-aplicar
        try {
            Path archived = pending.resolveSibling(PENDING_FILE + ".applied-" + stamp);
            Files.move(pending, archived, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            try { Files.deleteIfExists(pending); } catch (IOException ignored) {}
        }
        LiberthiaMod.LOGGER.info("[ChunkBackup] restauração de boot: {} aplicada(s), {} falha(s). Backup dos antigos em {}",
                applied, failed, preRestore);
    }

    // ───────────────────── desligamento gracioso ─────────────────────

    public static boolean isShutdownInProgress() {
        return SHUTDOWN_IN_PROGRESS.get();
    }

    /**
     * Dispara a sequência: salva → backup → contagem (proporcional aos players)
     * → kick all → espera 10s → para o servidor. Roda numa thread daemon (a
     * contagem usa sleeps); as ações de mundo são despachadas pra main thread.
     *
     * @return false se já houver uma sequência em andamento.
     */
    public static boolean startGracefulShutdown(MinecraftServer server, String reason) {
        if (!SHUTDOWN_IN_PROGRESS.compareAndSet(false, true)) return false;
        int players = server.getPlayerList().getPlayerCount();
        // delay proporcional ao nº de players (mín 10s, máx 60s) — dá tempo do
        // backup terminar e de todos verem o aviso.
        int warn = Math.max(10, Math.min(60, 10 + players * 3));
        Thread t = new Thread(() -> runShutdownSequence(server, reason, warn), "Liberthia-Maintenance-Shutdown");
        t.setDaemon(true);
        t.start();
        return true;
    }

    private static void runShutdownSequence(MinecraftServer server, String reason, int warnSeconds) {
        try {
            broadcast(server, "§6[Manutenção] §eIniciando backup e desligamento (§c~" + warnSeconds + "s§e)...");
            // 1. salva tudo (main thread) e espera o flush
            runOnMainBlocking(server, () -> server.saveEverything(false, true, true), 120);
            broadcast(server, "§6[Manutenção] §eMundo salvo. Copiando chunks pro backup...");
            // 2. backup (esta thread = fora da main, IO pesado não trava o tick)
            BackupResult res = createBackup(server);
            if (res.ok()) {
                broadcast(server, "§6[Manutenção] §aBackup criado: §f" + res.name() + " §7(" + res.files() + " arquivos)");
            } else {
                broadcast(server, "§c[Manutenção] Backup FALHOU — veja o log. Continuando o desligamento...");
            }
            // 3. contagem regressiva
            int[] marks = {60, 45, 30, 20, 15, 10, 5, 4, 3, 2, 1};
            for (int s = warnSeconds; s > 0; s--) {
                for (int m : marks) {
                    if (m == s) {
                        broadcast(server, "§6[Manutenção] §cDesconexão em §e" + s + "s§c — saia em local seguro!");
                        break;
                    }
                }
                sleep(1000);
            }
            // 4. kicka todos (main thread)
            runOnMain(server, () -> kickAll(server, reason));
            // 5. espera 10s após o kick
            LiberthiaMod.LOGGER.info("[Manutenção] todos kickados. Aguardando 10s antes do halt...");
            sleep(10_000);
            // 6. para o servidor (main thread). O processo SAI — relançamento manual.
            LiberthiaMod.LOGGER.info("[Manutenção] parando o servidor (halt).");
            runOnMain(server, () -> server.halt(false));
        } catch (Throwable e) {
            LiberthiaMod.LOGGER.error("[Manutenção] sequência de desligamento falhou: {}", e.toString());
        } finally {
            SHUTDOWN_IN_PROGRESS.set(false);
        }
    }

    private static void kickAll(MinecraftServer server, String reason) {
        Component c = Component.literal(reason);
        for (ServerPlayer p : new ArrayList<>(server.getPlayerList().getPlayers())) {
            p.connection.disconnect(c);
        }
    }

    private static void broadcast(MinecraftServer server, String msg) {
        runOnMain(server, () -> server.getPlayerList().broadcastSystemMessage(Component.literal(msg), false));
    }

    // ───────────────────── thread helpers ─────────────────────

    public static void runOnMain(MinecraftServer server, Runnable r) {
        if (server.isSameThread()) r.run();
        else server.execute(r);
    }

    public static void runOnMainBlocking(MinecraftServer server, Runnable r, int timeoutSec) {
        if (server.isSameThread()) { r.run(); return; }
        CountDownLatch latch = new CountDownLatch(1);
        server.execute(() -> {
            try { r.run(); } finally { latch.countDown(); }
        });
        try {
            latch.await(timeoutSec, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
