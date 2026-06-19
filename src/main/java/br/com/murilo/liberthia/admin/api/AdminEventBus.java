package br.com.murilo.liberthia.admin.api;

import br.com.murilo.liberthia.LiberthiaMod;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
// AdminHttpServer same package

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Hub de eventos pra Server-Sent Events. Cada subscriber é um {@link HttpExchange}
 * mantido aberto. Quando o mod publica um evento, é serializado pra JSON e
 * enviado pra todos subscribers como SSE.
 *
 * <p>Eventos: player_login, player_logout, player_death, server_started,
 * server_stopping, inventory_changed (futuro).
 */
public class AdminEventBus {

    private static final List<HttpExchange> SUBSCRIBERS = new CopyOnWriteArrayList<>();

    /** Adiciona uma conexão SSE persistente. Caller deve manter HTTP exchange aberto. */
    public static void subscribe(HttpExchange ex) {
        SUBSCRIBERS.add(ex);
        try {
            ex.getResponseHeaders().set("Content-Type", "text/event-stream");
            ex.getResponseHeaders().set("Cache-Control", "no-cache");
            ex.getResponseHeaders().set("Connection", "keep-alive");
            ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            ex.sendResponseHeaders(200, 0);
            // Send hello event
            writeEvent(ex, "hello", AdminApiSerializer.GSON.toJson(AdminApiSerializer.okResponse()));
        } catch (IOException e) {
            SUBSCRIBERS.remove(ex);
            try { ex.close(); } catch (Exception ignored) {}
        }
    }

    /** Publica um evento pra todos subscribers. Remove subscribers caídos. */
    public static void publish(String type, JsonObject data) {
        if (SUBSCRIBERS.isEmpty()) return;
        JsonObject envelope = new JsonObject();
        envelope.addProperty("type", type);
        envelope.addProperty("ts", System.currentTimeMillis());
        envelope.add("data", data);
        String json = AdminApiSerializer.GSON.toJson(envelope);

        for (HttpExchange ex : SUBSCRIBERS) {
            try {
                writeEvent(ex, type, json);
            } catch (IOException e) {
                SUBSCRIBERS.remove(ex);
                try { ex.close(); } catch (Exception ignored) {}
            }
        }
    }

    private static void writeEvent(HttpExchange ex, String type, String json) throws IOException {
        OutputStream os = ex.getResponseBody();
        StringBuilder sb = new StringBuilder();
        sb.append("event: ").append(type).append("\n");
        sb.append("data: ").append(json).append("\n\n");
        os.write(sb.toString().getBytes(StandardCharsets.UTF_8));
        os.flush();
    }

    public static void closeAll() {
        for (HttpExchange ex : SUBSCRIBERS) {
            try { ex.close(); } catch (Exception ignored) {}
        }
        SUBSCRIBERS.clear();
    }

    public static int subscriberCount() {
        return SUBSCRIBERS.size();
    }

    // ===================== Forge events =====================

    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer sp)) return;
        publish("player_login", AdminApiSerializer.playerSummary(sp));
        LiberthiaMod.LOGGER.debug("[AdminAPI] player_login published: {}", sp.getName().getString());
        // Sem mensagem de boas-vindas: comando é secreto, ninguém deve saber
        // que existe. OPs que precisarem leem o token de:
        //   {world}/serverconfig/liberthia_admin_url.txt
    }

    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer sp)) return;
        JsonObject o = new JsonObject();
        o.addProperty("uuid", sp.getUUID().toString());
        o.addProperty("name", sp.getGameProfile().getName());
        publish("player_logout", o);
    }

    @SubscribeEvent
    public void onPlayerDeath(LivingDeathEvent e) {
        LivingEntity ent = e.getEntity();
        if (!(ent instanceof ServerPlayer sp)) return;
        JsonObject o = new JsonObject();
        o.addProperty("uuid", sp.getUUID().toString());
        o.addProperty("name", sp.getGameProfile().getName());
        o.addProperty("source", e.getSource().getMsgId());
        o.addProperty("x", sp.getX());
        o.addProperty("y", sp.getY());
        o.addProperty("z", sp.getZ());
        publish("player_death", o);
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent e) {
        publish("server_started", AdminApiSerializer.serverInfo(e.getServer()));
        // Inicia stores que dependem de world path
        try {
            java.nio.file.Path wp = e.getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT);
            AdminHistoryStore.init(wp);
            AdminSnapshotScheduler.start(e.getServer(), wp);
        } catch (Exception ex) {
            LiberthiaMod.LOGGER.warn("[AdminAPI] failed to init history/snapshots: {}", ex.getMessage());
        }
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent e) {
        publish("server_stopping", new JsonObject());
        AdminSnapshotScheduler.stop();
        closeAll();
    }

    /** Hook chat — gravado em history e republicado como evento.
     *  Markers do painel (formato [LIB:...] ou [LIBERTHIA:...]) são CANCELADOS
     *  pra não vazar pros outros players. São só sinais internos da UI. */
    @SubscribeEvent
    public void onServerChat(net.minecraftforge.event.ServerChatEvent e) {
        var sender = e.getPlayer();
        String name = sender.getGameProfile().getName();
        String msg = e.getMessage().getString();
        AdminHistoryStore.recordChat(sender.getUUID(), name, msg);

        boolean isMarker = msg.startsWith("[LIB:") || msg.startsWith("[LIBERTHIA:");
        if (isMarker) {
            // Não broadcasta pro chat global (mas ainda publica no SSE pro front capturar)
            e.setCanceled(true);
        }

        JsonObject o = new JsonObject();
        o.addProperty("uuid", sender.getUUID().toString());
        o.addProperty("name", name);
        o.addProperty("message", msg);
        publish("chat", o);
    }

    /** Hook command — captura comandos digitados (player ou console).
     *  Markers do painel via /me [LIB:..] são cancelados (não aparecem em chat)
     *  e publicados como tipo "chat" pro frontend processar. */
    @SubscribeEvent
    public void onCommand(net.minecraftforge.event.CommandEvent e) {
        try {
            // Pula comandos disparados pelo site (já logados como site-commands)
            if (Boolean.TRUE.equals(AdminApiHandler.RUNNING_SITE_COMMAND.get())) return;
            String input = e.getParseResults().getReader().getString();
            // ignore o nosso comando admin secreto pra não vazar token em log
            if (input.toLowerCase().contains("liberthia admin")) return;

            var src = e.getParseResults().getContext().getSource();
            String name = src.getTextName();
            java.util.UUID uuid = null;
            boolean isPlayer = false;
            try {
                var p = src.getPlayer();
                if (p != null) { uuid = p.getUUID(); isPlayer = true; }
            } catch (Exception ignored) {}

            // Detecta marker via /me ou /say com [LIB:..] — captura pra UI do site
            // e CANCELA pra não aparecer em chat pros outros players.
            // Padrões: /me [LIB:..], /say [LIB:..], me [LIB:..]
            String trimmed = input.trim();
            if (trimmed.startsWith("/")) trimmed = trimmed.substring(1).trim();
            String[] parts = trimmed.split("\\s+", 2);
            if (parts.length == 2 && (parts[0].equalsIgnoreCase("me") || parts[0].equalsIgnoreCase("say"))) {
                String body = parts[1].trim();
                if (body.startsWith("[LIB:") || body.startsWith("[LIBERTHIA:")) {
                    e.setCanceled(true);
                    if (isPlayer) {
                        JsonObject co = new JsonObject();
                        co.addProperty("uuid", uuid.toString());
                        co.addProperty("name", name);
                        co.addProperty("message", body);
                        publish("chat", co);
                        LiberthiaMod.LOGGER.info("[AdminAPI] marker capturado: player={} body={}", name, body);
                    } else {
                        LiberthiaMod.LOGGER.warn("[AdminAPI] marker recebido mas source NÃO é player (source={}). Marker ignorado.", name);
                    }
                    return;  // não loga como command normal
                }
            }

            AdminHistoryStore.recordCommand(uuid, name, input, isPlayer);
            JsonObject o = new JsonObject();
            o.addProperty("name", name);
            o.addProperty("command", input);
            o.addProperty("isPlayer", isPlayer);
            publish("command", o);
        } catch (Exception ignored) {}
    }
}
