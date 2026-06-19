package br.com.murilo.liberthia.admin.api.hooks;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.admin.api.AdminHttpServer;
import br.com.murilo.liberthia.config.LiberthiaConfig;
import com.google.gson.JsonObject;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.entity.EntityTravelToDimensionEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Hooks que alimentam features de comunidade do backend (Backrooms, Quotes, Memorial).
 *
 * <p>Cada hook é leve: detecta o evento, monta JSON e enfileira POST async no
 * backend. Falha silenciosamente — features de comunidade são best-effort.
 *
 * <p>Registrado manualmente em LiberthiaMod (não via @EventBusSubscriber, que
 * sabemos ser frágil aqui).
 */
public class CommunityHooks {

    private static final ScheduledExecutorService EXEC = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "Liberthia-CommunityHooks");
        t.setDaemon(true);
        return t;
    });

    public CommunityHooks() {}

    /** Backrooms: detecta dimension change pra/de levels backrooms. */
    @SubscribeEvent
    public void onDimensionChange(EntityTravelToDimensionEvent event) {
        try {
            if (!(event.getEntity() instanceof ServerPlayer p)) return;
            String to = event.getDimension().location().toString();
            String from = p.level().dimension().location().toString();
            boolean toBackrooms = to.startsWith("backrooms:");
            boolean fromBackrooms = from.startsWith("backrooms:");
            if (!toBackrooms && !fromBackrooms) return;

            String levelId = toBackrooms ? to : from;
            String eventType = toBackrooms ? "enter" : "exit";

            JsonObject body = new JsonObject();
            body.addProperty("playerUuid", p.getUUID().toString());
            body.addProperty("playerName", p.getGameProfile().getName());
            body.addProperty("levelId", levelId);
            body.addProperty("eventType", eventType);
            body.addProperty("x", p.getX());
            body.addProperty("y", p.getY());
            body.addProperty("z", p.getZ());
            post("/api/backrooms", body);
        } catch (Throwable t) {
            LiberthiaMod.LOGGER.debug("[CommunityHooks] backrooms error: {}", t.toString());
        }
    }

    /** Chat capture: salva mensagens não-comando como quotes. */
    @SubscribeEvent
    public void onChat(ServerChatEvent event) {
        try {
            String msg = event.getMessage().getString();
            if (msg == null || msg.isBlank() || msg.startsWith("/") || msg.length() > 500) return;
            ServerPlayer p = event.getPlayer();
            if (p == null) return;

            JsonObject body = new JsonObject();
            body.addProperty("playerUuid", p.getUUID().toString());
            body.addProperty("playerName", p.getGameProfile().getName());
            body.addProperty("text", msg);
            body.addProperty("x", p.getX());
            body.addProperty("y", p.getY());
            body.addProperty("z", p.getZ());
            body.addProperty("dimension", p.level().dimension().location().toString());
            post("/api/quotes", body);
        } catch (Throwable t) {
            LiberthiaMod.LOGGER.debug("[CommunityHooks] quote error: {}", t.toString());
        }
    }

    /** Death: cria entry no cemitério. permanent=hardcore mode. */
    @SubscribeEvent
    public void onDeath(LivingDeathEvent event) {
        try {
            if (!(event.getEntity() instanceof ServerPlayer p)) return;
            String cause = event.getSource() == null ? "unknown" : event.getSource().getMsgId();
            // se atacante é player conhecido, anota
            if (event.getSource() != null && event.getSource().getEntity() instanceof ServerPlayer killer) {
                cause = "killed by " + killer.getGameProfile().getName();
            }
            boolean hardcore = p.serverLevel().getServer().isHardcore();

            JsonObject body = new JsonObject();
            body.addProperty("playerUuid", p.getUUID().toString());
            body.addProperty("playerName", p.getGameProfile().getName());
            body.addProperty("causeOfDeath", cause);
            body.addProperty("dimension", p.level().dimension().location().toString());
            body.addProperty("x", (int) p.getX());
            body.addProperty("y", (int) p.getY());
            body.addProperty("z", (int) p.getZ());
            body.addProperty("permanent", hardcore);
            post("/api/memorials", body);
        } catch (Throwable t) {
            LiberthiaMod.LOGGER.debug("[CommunityHooks] memorial error: {}", t.toString());
        }
    }

    /** POST async pro backend; silencia exceptions. */
    private static void post(String path, JsonObject body) {
        EXEC.submit(() -> {
            try {
                String backendUrl = LiberthiaConfig.SERVER.adminBackendUrl.get();
                if (backendUrl == null || backendUrl.isBlank()) return;
                backendUrl = backendUrl.replaceAll("/+$", "");
                String token = AdminHttpServer.getActiveToken();
                HttpURLConnection conn = (HttpURLConnection) new URL(backendUrl + path).openConnection();
                conn.setRequestMethod("POST");
                conn.setConnectTimeout(3000);
                conn.setReadTimeout(3000);
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                if (token != null) conn.setRequestProperty("X-Liberthia-Token", token);
                byte[] payload = body.toString().getBytes(StandardCharsets.UTF_8);
                try (OutputStream os = conn.getOutputStream()) { os.write(payload); }
                int code = conn.getResponseCode();
                if (code >= 400) {
                    LiberthiaMod.LOGGER.debug("[CommunityHooks] {} returned {}", path, code);
                }
                conn.disconnect();
            } catch (Exception ignored) {
                // backend offline = ok, ignora
            }
        });
    }
}
