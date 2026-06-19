package br.com.murilo.liberthia.admin.api.hooks;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.admin.api.AdminHttpServer;
import br.com.murilo.liberthia.config.LiberthiaConfig;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Monitora a pasta world/exposures/ do mod Exposure. Quando aparece um PNG novo,
 * faz upload pro backend (POST /api/photos) automaticamente.
 *
 * Roda a cada 30s, evitando hammer no FS.
 */
public class ExposurePhotoWatcher {

    private static ScheduledExecutorService scheduler;
    private static final Set<String> seen = new HashSet<>();
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3)).build();

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        if (scheduler != null) return;
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "Liberthia-PhotoWatcher");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleAtFixedRate(this::scan, 30, 30, TimeUnit.SECONDS);
        LiberthiaMod.LOGGER.info("[PhotoWatcher] iniciado — escaneia world/exposures/ a cada 30s");
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        if (scheduler != null) { scheduler.shutdownNow(); scheduler = null; }
        seen.clear();
    }

    private void scan() {
        try {
            var server = ServerLifecycleHooks.getCurrentServer();
            if (server == null) return;
            // Mod Exposure salva em world/exposures/ por default
            Path exposuresDir = server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT)
                    .resolve("exposures");
            if (!Files.isDirectory(exposuresDir)) return;

            String backendUrl = LiberthiaConfig.SERVER.adminBackendUrl.get();
            if (backendUrl == null || backendUrl.isBlank()) return;
            backendUrl = backendUrl.replaceAll("/+$", "");
            String token = AdminHttpServer.getActiveToken();

            try (var stream = Files.list(exposuresDir)) {
                final String finalBackend = backendUrl;
                final String finalToken = token;
                stream.filter(p -> p.toString().endsWith(".png"))
                        .filter(p -> !seen.contains(p.getFileName().toString()))
                        .forEach(p -> uploadPhoto(p, finalBackend, finalToken));
            }
        } catch (Exception e) {
            LiberthiaMod.LOGGER.debug("[PhotoWatcher] scan error: {}", e.getMessage());
        }
    }

    private void uploadPhoto(Path file, String backendUrl, String token) {
        try {
            byte[] bytes = Files.readAllBytes(file);
            // Tenta extrair playerName do filename (Exposure usa <playername>_<timestamp>.png em alguns casos)
            String fname = file.getFileName().toString();
            String authorName = fname.contains("_") ? fname.substring(0, fname.indexOf('_')) : "server";

            // Multipart manual super simples
            String boundary = "----LiberthiaBoundary" + UUID.randomUUID();
            byte[] body = buildMultipart(boundary, bytes, fname, authorName);

            HttpRequest req = HttpRequest.newBuilder(URI.create(backendUrl + "/api/photos"))
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .header("X-Liberthia-Token", token == null ? "" : token)
                    .timeout(Duration.ofSeconds(15))
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                    .build();
            HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200) {
                seen.add(fname);
                LiberthiaMod.LOGGER.info("[PhotoWatcher] ↑ {} (foto subida)", fname);
            } else {
                LiberthiaMod.LOGGER.debug("[PhotoWatcher] {} upload fail HTTP {}", fname, resp.statusCode());
            }
        } catch (Exception e) {
            LiberthiaMod.LOGGER.debug("[PhotoWatcher] {} upload error: {}", file.getFileName(), e.getMessage());
        }
    }

    private byte[] buildMultipart(String boundary, byte[] fileBytes, String filename, String authorName) {
        var out = new java.io.ByteArrayOutputStream();
        try {
            // file
            String part1 = "--" + boundary + "\r\n"
                    + "Content-Disposition: form-data; name=\"file\"; filename=\"" + filename + "\"\r\n"
                    + "Content-Type: image/png\r\n\r\n";
            out.write(part1.getBytes(StandardCharsets.UTF_8));
            out.write(fileBytes);
            out.write("\r\n".getBytes(StandardCharsets.UTF_8));

            // authorName
            String part2 = "--" + boundary + "\r\n"
                    + "Content-Disposition: form-data; name=\"authorName\"\r\n\r\n"
                    + authorName + "\r\n";
            out.write(part2.getBytes(StandardCharsets.UTF_8));

            // title (derivado do filename)
            String title = filename.replaceFirst("\\.png$", "");
            String part3 = "--" + boundary + "\r\n"
                    + "Content-Disposition: form-data; name=\"title\"\r\n\r\n"
                    + title + "\r\n";
            out.write(part3.getBytes(StandardCharsets.UTF_8));

            // closing
            out.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        } catch (Exception ignored) {}
        return out.toByteArray();
    }
}
