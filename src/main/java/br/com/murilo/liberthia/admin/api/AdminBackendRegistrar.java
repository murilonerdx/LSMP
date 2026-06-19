package br.com.murilo.liberthia.admin.api;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.config.LiberthiaConfig;
import com.google.gson.JsonObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Auto-registra o mod no backend Spring Boot via POST /api/mod/register.
 * Permite que o backend descubra dinamicamente onde o mod está rodando —
 * útil quando backend é hospedado em cloud e mod tá em casa.
 *
 * Roda em scheduler próprio (a cada 60s); idempotente. Falhas são silenciadas
 * em debug pra não poluir log se backend tá offline.
 */
public final class AdminBackendRegistrar {

    private static volatile ScheduledExecutorService scheduler;
    private static volatile int silenceCounter = 0;

    private AdminBackendRegistrar() {}

    public static synchronized void start() {
        if (!LiberthiaConfig.SERVER.adminBackendAutoRegister.get()) return;
        String backendUrl = LiberthiaConfig.SERVER.adminBackendUrl.get();
        if (backendUrl == null || backendUrl.isBlank()) {
            LiberthiaMod.LOGGER.info("[AdminAPI] backend_url vazio — skip auto-register");
            return;
        }
        if (scheduler != null) return;
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "Liberthia-BackendRegistrar");
            t.setDaemon(true);
            return t;
        });
        // Primeiro register imediato, depois a cada 60s (heartbeat)
        scheduler.scheduleAtFixedRate(AdminBackendRegistrar::doRegister, 2, 60, TimeUnit.SECONDS);
    }

    public static synchronized void stop() {
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
    }

    private static void doRegister() {
        try {
            String backendUrl = LiberthiaConfig.SERVER.adminBackendUrl.get();
            if (backendUrl == null || backendUrl.isBlank()) return;
            String token = AdminHttpServer.getActiveToken();
            if (token == null) return;
            int port = LiberthiaConfig.SERVER.adminApiPort.get();
            // Preferência: admin_api.public_address (DDNS/IP público) — necessário
            // quando o backend está em VPS/cloud e o MC tá atrás de NAT.
            // Fallback: IP LAN local (só funciona se backend está na mesma rede).
            String configured = LiberthiaConfig.SERVER.adminApiPublicAddress.get();
            String myUrl;
            boolean usingPublic = configured != null && !configured.isBlank();
            if (usingPublic) {
                String raw = configured.trim();
                // Se o user já passou URL completa (com scheme), respeita —
                // necessário pra túneis tipo cloudflared (https) e ngrok (tcp://).
                if (raw.startsWith("http://") || raw.startsWith("https://")) {
                    myUrl = raw.replaceAll("/+$", "");
                } else {
                    String host = raw
                            .replaceFirst("/.*$", "")
                            .replaceFirst(":\\d+$", "");
                    myUrl = "http://" + host + ":" + port;
                }
            } else {
                myUrl = "http://" + detectLocalIp() + ":" + port;
            }
            // 1x log INFO no primeiro register pra deixar claro qual URL foi anunciada
            if (silenceCounter == 0) {
                LiberthiaMod.LOGGER.info("[AdminAPI] registrando no backend como {} ({})",
                        myUrl, usingPublic ? "public_address" : "LAN — backend público não vai alcançar!");
            }

            JsonObject body = new JsonObject();
            body.addProperty("url", myUrl);
            body.addProperty("token", token);

            String fullUrl = backendUrl.replaceAll("/+$", "") + "/api/mod/register";
            HttpURLConnection conn = (HttpURLConnection) new URL(fullUrl).openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");

            byte[] payload = body.toString().getBytes(StandardCharsets.UTF_8);
            try (OutputStream os = conn.getOutputStream()) { os.write(payload); }

            int code = conn.getResponseCode();
            if (code == 200) {
                if (silenceCounter == 0) {
                    LiberthiaMod.LOGGER.info("[AdminAPI] registrado no backend {} ✓", backendUrl);
                }
                silenceCounter = (silenceCounter + 1) % 30; // a cada 30 batidas, log de novo
            } else {
                if (silenceCounter < 2) {
                    LiberthiaMod.LOGGER.warn("[AdminAPI] backend register retornou {} pra {}", code, fullUrl);
                }
                silenceCounter++;
            }
            conn.disconnect();
        } catch (Exception e) {
            if (silenceCounter < 2) {
                LiberthiaMod.LOGGER.warn("[AdminAPI] backend register falhou: {} (vai retry em 60s)", e.getMessage());
            }
            silenceCounter++;
        }
    }

    private static String detectLocalIp() {
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
}
