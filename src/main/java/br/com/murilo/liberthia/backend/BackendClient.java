package br.com.murilo.liberthia.backend;

import br.com.murilo.liberthia.capability.IInfectionData;
import br.com.murilo.liberthia.config.LiberthiaConfig;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.minecraft.server.level.ServerPlayer;

public final class BackendClient {
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "liberthia-backend");
        thread.setDaemon(true);
        return thread;
    });

    private static volatile HttpClient client = buildClient();

    private BackendClient() {
    }

    public static void reloadFromConfig() {
        client = buildClient();
    }

    public static void sendSnapshot(ServerPlayer player, IInfectionData data) {
        if (!LiberthiaConfig.SERVER.backendEnabled.get()) {
            return;
        }

        String baseUrl = LiberthiaConfig.SERVER.backendBaseUrl.get();
        if (baseUrl == null || baseUrl.isBlank()) {
            return;
        }

        String target = baseUrl + LiberthiaConfig.SERVER.snapshotPath.get();
        String body = "{"
                + "\"playerUuid\":\"" + escape(player.getUUID().toString()) + "\","
                + "\"playerName\":\"" + escape(player.getGameProfile().getName()) + "\","
                + "\"level\":\"" + escape(player.level().dimension().location().toString()) + "\","
                + "\"infection\":" + data.getInfection() + ","
                + "\"stage\":" + data.getStage() + ","
                + "\"permanentHealthPenalty\":" + data.getPermanentHealthPenalty() + ","
                + "\"x\":" + player.getBlockX() + ","
                + "\"y\":" + player.getBlockY() + ","
                + "\"z\":" + player.getBlockZ() + ","
                + "\"timestamp\":" + System.currentTimeMillis()
                + "}";

        HttpRequest request = HttpRequest.newBuilder(URI.create(target))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofMillis(LiberthiaConfig.SERVER.requestTimeoutMs.get()))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        client.sendAsync(request, java.net.http.HttpResponse.BodyHandlers.discarding())
                .exceptionally(ex -> null);
    }

    private static HttpClient buildClient() {
        return HttpClient.newBuilder()
                .executor(EXECUTOR)
                .connectTimeout(Duration.ofMillis(LiberthiaConfig.SERVER.connectTimeoutMs.get()))
                .build();
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
