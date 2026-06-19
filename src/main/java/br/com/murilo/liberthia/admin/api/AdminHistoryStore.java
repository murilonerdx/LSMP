package br.com.murilo.liberthia.admin.api;

import br.com.murilo.liberthia.LiberthiaMod;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.UUID;

/**
 * Ring-buffer in-memory de chat + comandos. Persiste append-only em
 * world/serverconfig/liberthia_history/{chat,commands}.jsonl
 *
 * Cada arquivo é uma sequência de objetos JSON, um por linha (JSONL).
 * Frontend pode ler por janelas (since/limit).
 */
public final class AdminHistoryStore {

    private static final int RAM_LIMIT = 5000;
    private static final Deque<JsonObject> chat = new ArrayDeque<>(RAM_LIMIT);
    private static final Deque<JsonObject> commands = new ArrayDeque<>(RAM_LIMIT);
    private static final Deque<JsonObject> siteCommands = new ArrayDeque<>(RAM_LIMIT);
    private static volatile Path chatFile;
    private static volatile Path cmdFile;
    private static volatile Path siteCmdFile;

    private AdminHistoryStore() {}

    /** Inicializa caminhos de persistência (chamado quando server liga). */
    public static synchronized void init(Path worldRoot) {
        try {
            Path dir = worldRoot.resolve("serverconfig").resolve("liberthia_history");
            Files.createDirectories(dir);
            chatFile = dir.resolve("chat.jsonl");
            cmdFile = dir.resolve("commands.jsonl");
            siteCmdFile = dir.resolve("site_commands.jsonl");
            // Carrega últimas 1000 entradas em RAM ao subir
            loadTail(chatFile, chat, 1000);
            loadTail(cmdFile, commands, 1000);
            loadTail(siteCmdFile, siteCommands, 1000);
            LiberthiaMod.LOGGER.info("[AdminHistory] init: chat={} cmd={} (RAM: {}/{})",
                    chatFile, cmdFile, chat.size(), commands.size());
        } catch (Exception e) {
            LiberthiaMod.LOGGER.warn("[AdminHistory] init failed: {}", e.getMessage());
        }
    }

    private static void loadTail(Path file, Deque<JsonObject> ring, int max) {
        try {
            if (!Files.exists(file)) return;
            var lines = Files.readAllLines(file);
            int from = Math.max(0, lines.size() - max);
            for (int i = from; i < lines.size(); i++) {
                String l = lines.get(i).trim();
                if (l.isEmpty()) continue;
                try {
                    JsonObject o = com.google.gson.JsonParser.parseString(l).getAsJsonObject();
                    ring.addLast(o);
                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}
    }

    public static synchronized void recordChat(UUID uuid, String name, String message) {
        JsonObject o = new JsonObject();
        long ts = System.currentTimeMillis();
        o.addProperty("ts", ts);
        o.addProperty("uuid", uuid == null ? "" : uuid.toString());
        o.addProperty("name", name);
        o.addProperty("message", message);
        chat.addLast(o);
        while (chat.size() > RAM_LIMIT) chat.pollFirst();
        appendLine(chatFile, o);
    }

    public static synchronized void recordCommand(UUID uuid, String name, String command, boolean isPlayer) {
        JsonObject o = new JsonObject();
        long ts = System.currentTimeMillis();
        o.addProperty("ts", ts);
        o.addProperty("uuid", uuid == null ? "" : uuid.toString());
        o.addProperty("name", name);
        o.addProperty("command", command);
        o.addProperty("isPlayer", isPlayer);
        commands.addLast(o);
        while (commands.size() > RAM_LIMIT) commands.pollFirst();
        appendLine(cmdFile, o);
    }

    private static void appendLine(Path file, JsonObject o) {
        if (file == null) return;
        try {
            String line = o.toString() + System.lineSeparator();
            Files.writeString(file, line, java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.APPEND);
        } catch (Exception e) {
            LiberthiaMod.LOGGER.debug("[AdminHistory] append failed: {}", e.getMessage());
        }
    }

    /** Retorna chat filtrado por uuid/since/limit (mais recente primeiro). */
    public static synchronized JsonArray queryChat(String uuid, long since, int limit) {
        return queryRing(chat, uuid, since, limit);
    }

    public static synchronized JsonArray queryCommands(String uuid, long since, int limit) {
        return queryRing(commands, uuid, since, limit);
    }

    /** Comandos disparados pelo painel web. Origin = "site" / "broadcaster" / "scripts" / etc. */
    public static synchronized void recordSiteCommand(String origin, String command, int result) {
        JsonObject o = new JsonObject();
        o.addProperty("ts", System.currentTimeMillis());
        o.addProperty("origin", origin == null ? "site" : origin);
        o.addProperty("command", command);
        o.addProperty("result", result);
        siteCommands.addLast(o);
        while (siteCommands.size() > RAM_LIMIT) siteCommands.pollFirst();
        appendLine(siteCmdFile, o);
    }

    public static synchronized JsonArray querySiteCommands(long since, int limit) {
        return queryRing(siteCommands, null, since, limit);
    }

    private static JsonArray queryRing(Deque<JsonObject> ring, String uuid, long since, int limit) {
        JsonArray arr = new JsonArray();
        var it = ring.descendingIterator();
        int count = 0;
        while (it.hasNext() && count < limit) {
            JsonObject o = it.next();
            long ts = o.has("ts") ? o.get("ts").getAsLong() : 0L;
            if (since > 0 && ts < since) continue;
            if (uuid != null && !uuid.isEmpty()) {
                String u = o.has("uuid") ? o.get("uuid").getAsString() : "";
                if (!uuid.equals(u)) continue;
            }
            arr.add(o);
            count++;
        }
        return arr;
    }
}
