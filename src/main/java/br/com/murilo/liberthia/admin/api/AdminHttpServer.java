package br.com.murilo.liberthia.admin.api;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.config.LiberthiaConfig;
import com.sun.net.httpserver.HttpServer;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.UUID;
import java.util.concurrent.Executors;

/**
 * Servidor HTTP embutido. Hooks em ServerStartedEvent (sobe) e
 * ServerStoppingEvent (desliga).
 *
 * <p>Usa {@link com.sun.net.httpserver.HttpServer} (JDK built-in, zero deps).
 * Roda em pool de threads próprio — handlers enfileiram chamadas no main
 * thread via {@link AdminApiHandler#runOnMain}.
 */
public class AdminHttpServer {

    private static volatile HttpServer activeServer;
    private static volatile String activeToken;

    public static String getActiveToken() {
        return activeToken;
    }

    public static boolean isRunning() {
        return activeServer != null;
    }

    /** Liga programaticamente. Idempotente — se já tá rodando, retorna true. */
    public static synchronized boolean startNow() {
        if (activeServer != null) return true;
        return doStart();
    }

    /** Desliga programaticamente. */
    public static synchronized void stopNow() {
        AdminEventBus.closeAll();
        AdminBackendRegistrar.stop();
        HttpServer s = activeServer;
        if (s != null) {
            try { s.stop(0); } catch (Exception ignored) {}
            activeServer = null;
            activeToken = null;
            LiberthiaMod.LOGGER.info("[AdminAPI] HTTP server desligado (manual)");
        }
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        if (!LiberthiaConfig.SERVER.adminApiEnabled.get()) {
            LiberthiaMod.LOGGER.info("[AdminAPI] disabled by config");
            return;
        }
        doStart();
    }

    private static boolean doStart() {
        // v0.1.22: token agora tem PRIORIDADE de fonte clara, pra evitar a
        // armadilha clássica de "tem 2 configs diferentes" (Forge SERVER configs
        // ficam em world/serverconfig/, NÃO em config/ — e admin tipicamente
        // edita o errado). Ordem:
        //   1. System property: -Dliberthia.admin.token=...
        //   2. Env var: LIBERTHIA_ADMIN_TOKEN=...
        //   3. Config per-world: world/serverconfig/liberthia-server.toml
        //   4. Se vazio em todas: gera UUID e salva no config
        // O log mostra explicitamente DE ONDE veio o token usado.
        String token = System.getProperty("liberthia.admin.token");
        String source = "system property -Dliberthia.admin.token";
        if (token == null || token.isBlank()) {
            token = System.getenv("LIBERTHIA_ADMIN_TOKEN");
            source = "env var LIBERTHIA_ADMIN_TOKEN";
        }
        if (token == null || token.isBlank()) {
            token = LiberthiaConfig.SERVER.adminApiToken.get();
            source = "config world/serverconfig/liberthia-server.toml";
        }
        if (token == null || token.isBlank()) {
            token = UUID.randomUUID().toString();
            LiberthiaConfig.SERVER.adminApiToken.set(token);
            LiberthiaConfig.SERVER.adminApiToken.save();
            source = "gerado novo (salvo em world/serverconfig/liberthia-server.toml)";
            LiberthiaMod.LOGGER.info("[AdminAPI] gerado token novo (salvo em world/serverconfig/liberthia-server.toml)");
        }
        activeToken = token;
        LiberthiaMod.LOGGER.info("[AdminAPI] token source: {}", source);

        int port = LiberthiaConfig.SERVER.adminApiPort.get();
        String bind = LiberthiaConfig.SERVER.adminApiBindAddress.get();

        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(bind, port), 0);
            server.createContext("/api/", new AdminApiHandler());
            // Endpoint de healthcheck simples (sem auth)
            server.createContext("/health", ex -> {
                byte[] body = "{\"status\":\"ok\",\"mod\":\"liberthia\"}".getBytes();
                ex.getResponseHeaders().set("Content-Type", "application/json");
                ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                ex.sendResponseHeaders(200, body.length);
                ex.getResponseBody().write(body);
                ex.close();
            });
            // Pool de 4 threads — suficiente pra mod admin
            server.setExecutor(Executors.newFixedThreadPool(4, r -> {
                Thread t = new Thread(r, "Liberthia-AdminAPI");
                t.setDaemon(true);
                return t;
            }));
            server.start();
            activeServer = server;
            LiberthiaMod.LOGGER.info("[AdminAPI] listening on {}:{} (token=*****{})",
                    bind, port, token.length() > 4 ? token.substring(token.length() - 4) : "");
            LiberthiaMod.LOGGER.info("[AdminAPI] full token (compartilha com o backend): {}", token);
            writeAdminUrlFile(token, port);
            // Auto-registra no backend Spring Boot (se backend_url configurado)
            AdminBackendRegistrar.start();
            return true;
        } catch (IOException e) {
            LiberthiaMod.LOGGER.error("[AdminAPI] falha ao iniciar HTTP server em {}:{}", bind, port, e);
            return false;
        }
    }

    /** Escreve URL + token num arquivo .txt no disco pra admin ler sem chat. */
    private static void writeAdminUrlFile(String token, int port) {
        try {
            String localIp = detectLocalIpStatic();
            String configuredBackend = LiberthiaConfig.SERVER.adminBackendUrl.get();
            String configuredVoice = LiberthiaConfig.SERVER.voiceBackendUrl.get();
            boolean autoRegister = LiberthiaConfig.SERVER.adminBackendAutoRegister.get();

            String content = "Liberthia Admin Panel\n" +
                    "======================\n\n" +
                    "Mod URL (local):  http://127.0.0.1:" + port + "\n" +
                    "Mod URL (LAN):    http://" + localIp + ":" + port + "\n" +
                    "Token:            " + token + "\n\n" +
                    "Backend Spring Boot\n" +
                    "-------------------\n" +
                    "Admin backend URL: " + (configuredBackend == null || configuredBackend.isBlank()
                            ? "(VAZIO — auto-register desligado)"
                            : configuredBackend) + "\n" +
                    "Voice backend URL: " + (configuredVoice == null || configuredVoice.isBlank()
                            ? "(VAZIO — uploads de voz desligados)"
                            : configuredVoice) + "\n" +
                    "Auto-register:     " + (autoRegister ? "ON (registra a cada 60s)" : "OFF") + "\n\n" +
                    "Cole no backend/src/main/resources/application.yml:\n" +
                    "  mod.url:   http://" + localIp + ":" + port + "\n" +
                    "  mod.token: " + token + "\n\n" +
                    "Frontend dev: http://" + localIp + ":5173\n\n" +
                    "Healthcheck (sem auth): curl http://" + localIp + ":" + port + "/health\n" +
                    "(arquivo regenerado a cada start do servidor)\n";
            java.nio.file.Path file = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer()
                    .getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT)
                    .resolve("serverconfig").resolve("liberthia_admin_url.txt");
            java.nio.file.Files.createDirectories(file.getParent());
            java.nio.file.Files.writeString(file, content);
            LiberthiaMod.LOGGER.info("[AdminAPI] URL + token salvos em {}", file);
        } catch (Exception e) {
            LiberthiaMod.LOGGER.warn("[AdminAPI] falha ao escrever URL file: {}", e.getMessage());
        }
    }

    private static String detectLocalIpStatic() {
        try {
            java.util.Enumeration<java.net.NetworkInterface> ifs = java.net.NetworkInterface.getNetworkInterfaces();
            for (java.net.NetworkInterface ni : java.util.Collections.list(ifs)) {
                if (ni.isLoopback() || !ni.isUp()) continue;
                for (java.net.InetAddress addr : java.util.Collections.list(ni.getInetAddresses())) {
                    if (addr.isLoopbackAddress()) continue;
                    if (addr.getHostAddress().contains(":")) continue;
                    return addr.getHostAddress();
                }
            }
        } catch (Exception ignored) {}
        return "127.0.0.1";
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        AdminEventBus.closeAll();
        AdminBackendRegistrar.stop();
        HttpServer server = activeServer;
        if (server != null) {
            try {
                server.stop(1);
                LiberthiaMod.LOGGER.info("[AdminAPI] HTTP server desligado");
            } catch (Exception e) {
                LiberthiaMod.LOGGER.error("[AdminAPI] erro desligando HTTP server", e);
            }
            activeServer = null;
            activeToken = null;
        }
    }
}
