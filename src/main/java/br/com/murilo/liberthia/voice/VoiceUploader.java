package br.com.murilo.liberthia.voice;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.admin.api.AdminHttpServer;
import br.com.murilo.liberthia.config.LiberthiaConfig;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Upload async de clipes de voz pro backend Spring Boot. Usa multipart-like
 * mas simples: dois requests sequenciais — primeiro POST de metadata em JSON
 * pra reservar um ID, depois PUT do binário.
 *
 * Pool de 2 threads pra evitar bloquear thread do SVC. Em caso de falha
 * (backend offline), o clipe é PERDIDO — sem retry persistente. Razoável
 * porque voz é efêmera; quem quiser persistência forte pode ligar um
 * disk-backed queue depois.
 */
public final class VoiceUploader {

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();

    private static final ExecutorService POOL = Executors.newFixedThreadPool(2, r -> {
        Thread t = new Thread(r, "LiberthiaVoice-Upload");
        t.setDaemon(true);
        return t;
    });

    private VoiceUploader() {}

    public static void uploadAsync(byte[] wavData, VoiceClipMetadata meta) {
        POOL.submit(() -> doUpload(wavData, meta));
    }

    /** Throttle pra log de "config faltando" — uma vez por minuto. */
    private static volatile long lastConfigWarnMs = 0L;

    private static void doUpload(byte[] wavData, VoiceClipMetadata meta) {
        try {
            String backendUrl = LiberthiaConfig.voiceBackendUrl();
            if (backendUrl == null || backendUrl.isBlank()) {
                long now = System.currentTimeMillis();
                if (now - lastConfigWarnMs > 60_000L) {
                    lastConfigWarnMs = now;
                    LiberthiaMod.LOGGER.warn("[LiberthiaVoice] ⚠ voice.backend_url NÃO CONFIGURADO " +
                            "(nem admin_api.backend_url). Clipes estão sendo descartados! " +
                            "Configure em config/liberthia-server.toml → seção [voice] → backend_url=\"http://ip:8090\"");
                }
                return;
            }
            // Normaliza: remove trailing slash pra não gerar URL com "//api/..." que
            // quebra o AuthFilter (startsWith("/api/...") falha quando vem "//api/...").
            backendUrl = backendUrl.replaceAll("/+$", "");
            LiberthiaMod.LOGGER.debug("[LiberthiaVoice] uploading {} bytes pra {}",
                    wavData.length, backendUrl);
            String secret = AdminHttpServer.getActiveToken(); // reuso do token admin pra autenticar

            // Step 1: POST metadata → backend retorna {id, uploadUrl}
            String metaJson = String.format(java.util.Locale.US,
                    "{\"playerUuid\":\"%s\",\"playerName\":\"%s\",\"ts\":%d,\"durationMs\":%d," +
                            "\"x\":%.2f,\"y\":%.2f,\"z\":%.2f,\"dimension\":\"%s\",\"sizeBytes\":%d}",
                    meta.playerUuid, escape(meta.playerName), meta.ts, meta.durationMs,
                    meta.x, meta.y, meta.z, meta.dimension, wavData.length);

            // IMPORTANTE: endpoints /api/mod/voice/* (não /api/voice/*) — esses
            // são públicos no AuthFilter e validados via X-Liberthia-Token.
            // /api/voice/* exige Bearer auth (browser flow) e retorna 401 aqui.
            HttpRequest reserveReq = HttpRequest.newBuilder(URI.create(backendUrl + "/api/mod/voice/reserve"))
                    .header("Content-Type", "application/json; charset=utf-8")
                    .header("X-Liberthia-Token", secret == null ? "" : secret)
                    .timeout(Duration.ofSeconds(5))
                    .POST(HttpRequest.BodyPublishers.ofString(metaJson, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> reserveResp = CLIENT.send(reserveReq, HttpResponse.BodyHandlers.ofString());
            if (reserveResp.statusCode() != 200) {
                LiberthiaMod.LOGGER.warn("[LiberthiaVoice] reserve falhou: HTTP {} body={}",
                        reserveResp.statusCode(), reserveResp.body());
                return;
            }
            // Parse manual super simples (sem Jackson dep no mod)
            String body = reserveResp.body();
            Long clipId = extractLongField(body, "id");
            if (clipId == null) {
                LiberthiaMod.LOGGER.debug("[LiberthiaVoice] reserve sem id: {}", body);
                return;
            }

            // Step 2: PUT do WAV
            HttpRequest uploadReq = HttpRequest.newBuilder(URI.create(backendUrl + "/api/mod/voice/clips/" + clipId + "/audio"))
                    .header("Content-Type", "audio/wav")
                    .header("X-Liberthia-Token", secret == null ? "" : secret)
                    .timeout(Duration.ofSeconds(15))
                    .PUT(HttpRequest.BodyPublishers.ofByteArray(wavData))
                    .build();

            HttpResponse<String> uploadResp = CLIENT.send(uploadReq, HttpResponse.BodyHandlers.ofString());
            if (uploadResp.statusCode() != 200) {
                LiberthiaMod.LOGGER.warn("[LiberthiaVoice] upload falhou: HTTP {} body={}",
                        uploadResp.statusCode(), uploadResp.body());
                return;
            }
            LiberthiaMod.LOGGER.debug("[LiberthiaVoice] clipe #{} salvo ({}ms de {})",
                    clipId, meta.durationMs, meta.playerName);
        } catch (Exception e) {
            LiberthiaMod.LOGGER.warn("[LiberthiaVoice] upload exception: {}", e.toString(), e);
        }
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    /** Extração super simples de "id":NUMBER de uma string JSON. Não usa parser. */
    private static Long extractLongField(String json, String field) {
        if (json == null) return null;
        String key = "\"" + field + "\"";
        int idx = json.indexOf(key);
        if (idx < 0) return null;
        idx += key.length();
        while (idx < json.length() && (json.charAt(idx) == ' ' || json.charAt(idx) == ':')) idx++;
        int end = idx;
        while (end < json.length() && Character.isDigit(json.charAt(end))) end++;
        if (end == idx) return null;
        try { return Long.parseLong(json.substring(idx, end)); }
        catch (Exception e) { return null; }
    }

    /** Helper que baixa um WAV salvo do backend pra playback in-game. */
    public static byte[] downloadClip(long clipId) {
        try {
            String backendUrl = LiberthiaConfig.voiceBackendUrl();
            if (backendUrl == null) return null;
            backendUrl = backendUrl.replaceAll("/+$", "");
            String secret = AdminHttpServer.getActiveToken();
            HttpRequest req = HttpRequest.newBuilder(URI.create(backendUrl + "/api/mod/voice/clips/" + clipId + "/audio"))
                    .header("X-Liberthia-Token", secret == null ? "" : secret)
                    .timeout(Duration.ofSeconds(10))
                    .GET().build();
            HttpResponse<byte[]> resp = CLIENT.send(req, HttpResponse.BodyHandlers.ofByteArray());
            if (resp.statusCode() == 200) return resp.body();
        } catch (Exception e) {
            LiberthiaMod.LOGGER.warn("[LiberthiaVoice] download clip #{} fail: {}", clipId, e.getMessage());
        }
        return null;
    }
}
