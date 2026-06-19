package br.com.murilo.liberthia.admin.mod;

import br.com.murilo.liberthia.admin.config.ModBridgeConfig;
import br.com.murilo.liberthia.admin.engine.EngineEventBus;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Cliente Server-Sent Events: conecta no mod /api/events/sse e re-broadcast
 * no WebSocket. Auto-reconnect em caso de desconexão.
 */
@Component
public class ModSseListener {

    private static final Logger LOG = LoggerFactory.getLogger(ModSseListener.class);

    private final ModBridgeConfig config;
    private final ModRegistry registry;
    private final LiveEventBroadcaster broadcaster;
    private final EngineEventBus engineBus;
    private volatile boolean running = false;
    private Thread thread;

    public ModSseListener(ModBridgeConfig config, ModRegistry registry, LiveEventBroadcaster broadcaster, EngineEventBus engineBus) {
        this.config = config;
        this.registry = registry;
        this.broadcaster = broadcaster;
        this.engineBus = engineBus;
    }

    @PostConstruct
    public void start() {
        running = true;
        thread = new Thread(this::loop, "ModSseListener");
        thread.setDaemon(true);
        thread.start();
    }

    @PreDestroy
    public void stop() {
        running = false;
        if (thread != null) thread.interrupt();
    }

    private void loop() {
        HttpClient http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        int consecutiveFailures = 0;

        while (running) {
            // Re-lê do registry a cada iteração (mod pode ter se re-registrado com URL nova)
            String url = registry.getUrl() + "/api/events/sse";
            String token = registry.getToken() == null ? "" : registry.getToken();
            try {
                if (consecutiveFailures < 2) LOG.info("Conectando SSE: {}", url);
                HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                        .header("X-Liberthia-Token", token)
                        .header("Accept", "text/event-stream")
                        .timeout(Duration.ofMinutes(60))
                        .GET().build();
                HttpResponse<java.io.InputStream> resp = http.send(req, HttpResponse.BodyHandlers.ofInputStream());
                if (resp.statusCode() != 200) {
                    if (consecutiveFailures < 2) LOG.warn("SSE retornou status {}; tentando de novo em {}ms", resp.statusCode(), config.getSseReconnectMs());
                    else LOG.debug("SSE status {} (silenciado, retry contínuo)", resp.statusCode());
                    consecutiveFailures++;
                    Thread.sleep(config.getSseReconnectMs());
                    continue;
                }

                LOG.info("SSE conectado ao mod ✓");
                consecutiveFailures = 0;
                BufferedReader r = new BufferedReader(new InputStreamReader(resp.body(), StandardCharsets.UTF_8));
                String line;
                StringBuilder data = new StringBuilder();
                while (running && (line = r.readLine()) != null) {
                    if (line.isEmpty()) {
                        if (data.length() > 0) {
                            String payload = data.toString();
                            broadcaster.broadcast(payload);
                            // Também distribui para engines de background
                            engineBus.publish(payload);
                            data.setLength(0);
                        }
                    } else if (line.startsWith("data: ")) {
                        data.append(line.substring(6));
                    }
                }
                LOG.info("SSE stream encerrado pelo mod, reconectando...");
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            } catch (java.net.ConnectException ce) {
                if (consecutiveFailures == 0) {
                    LOG.warn("Mod não acessível em {} (esperando o servidor MC subir, retry a cada {}ms)",
                            registry.getUrl(), config.getSseReconnectMs());
                } else {
                    LOG.debug("Mod offline (silenciado)");
                }
                consecutiveFailures++;
            } catch (Exception e) {
                if (consecutiveFailures < 2) LOG.warn("SSE error: {}", e.getMessage());
                else LOG.debug("SSE error silenciado: {}", e.getMessage());
                consecutiveFailures++;
            }
            try { Thread.sleep(config.getSseReconnectMs()); }
            catch (InterruptedException ie) { Thread.currentThread().interrupt(); break; }
        }
        LOG.info("ModSseListener parado");
    }
}
