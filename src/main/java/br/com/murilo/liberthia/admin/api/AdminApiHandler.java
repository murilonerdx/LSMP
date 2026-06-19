package br.com.murilo.liberthia.admin.api;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.config.LiberthiaConfig;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Handler único pra todas rotas /api/*. Faz auth de token e dispatch baseado no path.
 *
 * <p>Operações que mexem no mundo são enfileiradas via {@link MinecraftServer#execute(Runnable)}
 * e o resultado é esperado via CompletableFuture (timeout 5s).
 */
public class AdminApiHandler implements HttpHandler {

    // v0.1.22 r15: 30s (era 5s) — tolera bursts de lag em servidores
    // pesados (200+ mods, Ketting, etc) sem cuspir TimeoutException.
    private static final long EXEC_TIMEOUT_MS = 30000;

    /**
     * Thread-local flag setado enquanto um comando do site executa.
     * Permite que AdminEventBus.onCommand pule o registro pra evitar duplicação
     * (já registramos como site_command direto no handler).
     */
    public static final ThreadLocal<Boolean> RUNNING_SITE_COMMAND = ThreadLocal.withInitial(() -> false);

    @Override
    public void handle(HttpExchange ex) throws IOException {
        try {
            // CORS preflight
            ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            ex.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS, DELETE");
            ex.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, X-Liberthia-Token");
            if ("OPTIONS".equals(ex.getRequestMethod())) {
                ex.sendResponseHeaders(204, -1);
                ex.close();
                return;
            }

            // Auth
            if (!checkAuth(ex)) {
                writeJson(ex, 401, AdminApiSerializer.errorResponse("invalid token"));
                return;
            }

            String path = ex.getRequestURI().getPath();
            String method = ex.getRequestMethod();

            // SSE — handler especial mantém conexão aberta
            if (path.equals("/api/events/sse") && "GET".equals(method)) {
                AdminEventBus.subscribe(ex);
                return; // não fecha — mantém aberto
            }

            // Routing
            if (path.equals("/api/server/info") && "GET".equals(method)) {
                handleServerInfo(ex);
            } else if (path.equals("/api/players") && "GET".equals(method)) {
                handlePlayers(ex);
            } else if (path.equals("/api/items") && "GET".equals(method)) {
                handleItems(ex);
            } else if (path.equals("/api/enchantments") && "GET".equals(method)) {
                handleEnchantments(ex);
            } else if (path.equals("/api/sounds") && "GET".equals(method)) {
                handleSounds(ex);
            } else if (path.equals("/api/particles") && "GET".equals(method)) {
                handleParticles(ex);
            } else if (path.equals("/api/effects") && "GET".equals(method)) {
                handleEffects(ex);
            } else if (path.equals("/api/entities") && "GET".equals(method)) {
                handleEntities(ex);
            } else if (path.equals("/api/command") && "POST".equals(method)) {
                handleCommand(ex);
            } else if (path.equals("/api/world/time") && "POST".equals(method)) {
                handleWorldTime(ex);
            } else if (path.equals("/api/world/weather") && "POST".equals(method)) {
                handleWorldWeather(ex);
            } else if (path.equals("/api/world/difficulty") && "POST".equals(method)) {
                handleWorldDifficulty(ex);
            } else if (path.equals("/api/server/operators") && "GET".equals(method)) {
                handleOperators(ex);
            } else if (path.equals("/api/server/bans") && "GET".equals(method)) {
                handleBans(ex);
            } else if (path.equals("/api/server/whitelist") && "GET".equals(method)) {
                handleWhitelist(ex);
            } else if (path.equals("/api/server/save") && "POST".equals(method)) {
                handleSaveAll(ex);
            } else if (path.equals("/api/server/broadcast") && "POST".equals(method)) {
                handleBroadcast(ex);
            } else if (path.equals("/api/server/backup") && "POST".equals(method)) {
                handleBackup(ex);
            } else if (path.equals("/api/world/spawn-entity") && "POST".equals(method)) {
                handleSpawnEntity(ex);
            } else if (path.equals("/api/world/spawn-player-clone") && "POST".equals(method)) {
                handleSpawnPlayerClone(ex);
            } else if (path.equals("/api/voice/play") && "POST".equals(method)) {
                handleVoicePlay(ex);
            } else if (path.equals("/api/world/kill-by-tag") && "POST".equals(method)) {
                handleKillByTag(ex);
            } else if (path.equals("/api/world/particle") && "POST".equals(method)) {
                handleParticle(ex);
            } else if (path.equals("/api/world/explosion") && "POST".equals(method)) {
                handleExplosion(ex);
            } else if (path.equals("/api/world/whisper") && "POST".equals(method)) {
                handleWhisper(ex);
            } else if (path.equals("/api/players/heal-all") && "POST".equals(method)) {
                handleHealAll(ex);
            } else if (path.equals("/api/history/chat") && "GET".equals(method)) {
                handleHistoryChat(ex);
            } else if (path.equals("/api/history/commands") && "GET".equals(method)) {
                handleHistoryCommands(ex);
            } else if (path.equals("/api/history/site-commands") && "GET".equals(method)) {
                handleHistorySiteCommands(ex);
            } else if (path.equals("/api/snapshot/run-now") && "POST".equals(method)) {
                handleSnapshotRunNow(ex);
            } else if (path.startsWith("/api/snapshot/list/") && "GET".equals(method)) {
                handleSnapshotList(ex, path);
            } else if (path.startsWith("/api/snapshot/get/") && "GET".equals(method)) {
                handleSnapshotGet(ex, path);
            } else if (path.equals("/api/map/chunks") && "GET".equals(method)) {
                handleMapChunks(ex);
            } else if (path.startsWith("/api/map/chunk/") && "GET".equals(method)) {
                handleMapChunk(ex, path);
            } else if (path.startsWith("/api/player/")) {
                handlePlayerSubresource(ex, path, method);
            } else if (path.startsWith("/api/matter/")) {
                handleMatter(ex, path, method);
            } else if (path.startsWith("/api/telemetry")) {
                handleTelemetry(ex, path, method);
            }
            // ─── r161: Spell / Magic / Observer-Clone endpoints ────────
            else if (path.equals("/api/spells/list") && "GET".equals(method)) {
                handleSpellsList(ex);
            } else if (path.equals("/api/spells/give") && "POST".equals(method)) {
                handleSpellGive(ex);
            } else if (path.equals("/api/spells/cast") && "POST".equals(method)) {
                handleSpellCast(ex);
            } else if (path.equals("/api/spells/create") && "POST".equals(method)) {
                handleSpellCreate(ex);
            } else if (path.startsWith("/api/magic/stats/")) {
                handleMagicStats(ex, path, method);
            } else if (path.equals("/api/world/spawn-observer-clone") && "POST".equals(method)) {
                handleSpawnObserverClone(ex);
            }
            else {
                writeJson(ex, 404, AdminApiSerializer.errorResponse("not found: " + method + " " + path));
            }
        } catch (Exception e) {
            LiberthiaMod.LOGGER.error("[AdminAPI] handler error", e);
            try {
                writeJson(ex, 500, AdminApiSerializer.errorResponse(e.getClass().getSimpleName() + ": " + e.getMessage()));
            } catch (Exception ignored) {}
        }
    }

    private boolean checkAuth(HttpExchange ex) {
        String expected = AdminHttpServer.getActiveToken();
        if (expected == null || expected.isEmpty()) return true; // disabled = no auth
        String got = ex.getRequestHeaders().getFirst("X-Liberthia-Token");
        if (got == null) {
            // fallback: ?token=...
            String q = ex.getRequestURI().getQuery();
            if (q != null) {
                for (String part : q.split("&")) {
                    if (part.startsWith("token=")) {
                        got = part.substring(6);
                        break;
                    }
                }
            }
        }
        return expected.equals(got);
    }

    // ===================== Endpoints =====================

    private void handleServerInfo(HttpExchange ex) throws IOException {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            writeJson(ex, 503, AdminApiSerializer.errorResponse("server not running"));
            return;
        }
        writeJson(ex, 200, AdminApiSerializer.serverInfo(server));
    }

    private void handlePlayers(HttpExchange ex) throws IOException {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            writeJson(ex, 503, AdminApiSerializer.errorResponse("server not running"));
            return;
        }
        JsonArray arr = new JsonArray();
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            arr.add(AdminApiSerializer.playerSummary(p));
        }
        JsonObject o = new JsonObject();
        o.add("players", arr);
        writeJson(ex, 200, o);
    }

    private void handleItems(HttpExchange ex) throws IOException {
        JsonObject o = new JsonObject();
        o.add("items", AdminApiSerializer.allItems());
        writeJson(ex, 200, o);
    }

    private void handleEnchantments(HttpExchange ex) throws IOException {
        JsonObject o = new JsonObject();
        o.add("enchantments", AdminApiSerializer.allEnchantments());
        writeJson(ex, 200, o);
    }

    private void handleSounds(HttpExchange ex) throws IOException {
        JsonObject o = new JsonObject();
        o.add("sounds", AdminApiSerializer.allSounds());
        writeJson(ex, 200, o);
    }

    private void handleParticles(HttpExchange ex) throws IOException {
        JsonObject o = new JsonObject();
        o.add("particles", AdminApiSerializer.allParticles());
        writeJson(ex, 200, o);
    }

    private void handleEffects(HttpExchange ex) throws IOException {
        JsonObject o = new JsonObject();
        o.add("effects", AdminApiSerializer.allEffects());
        writeJson(ex, 200, o);
    }

    private void handleEntities(HttpExchange ex) throws IOException {
        JsonObject o = new JsonObject();
        o.add("entities", AdminApiSerializer.allEntities());
        writeJson(ex, 200, o);
    }

    private void handleCommand(HttpExchange ex) throws IOException {
        JsonObject body = readJson(ex);
        if (body == null || !body.has("command")) {
            writeJson(ex, 400, AdminApiSerializer.errorResponse("missing 'command'"));
            return;
        }
        String cmd = body.get("command").getAsString();
        // origem opcional pra rastrear no histórico de comandos do site
        String origin = body.has("origin") ? body.get("origin").getAsString() : "site";
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            writeJson(ex, 503, AdminApiSerializer.errorResponse("server not running"));
            return;
        }
        Integer result = runOnMain(server, () -> {
            RUNNING_SITE_COMMAND.set(true);
            try {
                // withSuppressedOutput → não envia feedback pra console nem broadcasta
                // sucesso pra ops com sendCommandFeedback=true. Comando executa silencioso.
                CommandSourceStack source = server.createCommandSourceStack()
                        .withPermission(4)
                        .withSuppressedOutput();
                return server.getCommands().performPrefixedCommand(source, cmd);
            } finally {
                RUNNING_SITE_COMMAND.set(false);
            }
        });
        // v0.1.22 r15: result==null = server não pronto ou timeout. Retorna
        // 503 (Service Unavailable) pro backend não tratar como sucesso e
        // não loga no histórico como comando executado.
        if (result == null) {
            writeJson(ex, 503, AdminApiSerializer.errorResponse(
                    "server not ready or main thread busy — try again in a few seconds"));
            return;
        }
        // Loga no histórico de comandos do site (separado de chat/comandos in-game)
        AdminHistoryStore.recordSiteCommand(origin, cmd, result);

        JsonObject o = new JsonObject();
        o.addProperty("ok", true);
        o.addProperty("result", result);
        writeJson(ex, 200, o);
    }

    private void handlePlayerSubresource(HttpExchange ex, String path, String method) throws IOException {
        // Formato: /api/player/{uuid}/{action}
        String[] parts = path.substring("/api/player/".length()).split("/");
        if (parts.length < 2) {
            writeJson(ex, 400, AdminApiSerializer.errorResponse("malformed player path"));
            return;
        }
        UUID uuid;
        try { uuid = UUID.fromString(parts[0]); }
        catch (IllegalArgumentException e) {
            writeJson(ex, 400, AdminApiSerializer.errorResponse("invalid uuid"));
            return;
        }
        String action = parts[1];
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            writeJson(ex, 503, AdminApiSerializer.errorResponse("server not running"));
            return;
        }

        switch (action) {
            case "inventory" -> {
                if (!"GET".equals(method)) { writeJson(ex, 405, AdminApiSerializer.errorResponse("method")); return; }
                JsonObject o = runOnMain(server, () -> {
                    ServerPlayer p = server.getPlayerList().getPlayer(uuid);
                    return p == null ? null : AdminApiSerializer.inventorySnapshot(p);
                });
                if (o == null) writeJson(ex, 404, AdminApiSerializer.errorResponse("player not online"));
                else writeJson(ex, 200, o);
            }
            case "give" -> {
                if (!"POST".equals(method)) { writeJson(ex, 405, AdminApiSerializer.errorResponse("method")); return; }
                JsonObject body = readJson(ex);
                if (body == null || !body.has("item")) {
                    writeJson(ex, 400, AdminApiSerializer.errorResponse("missing 'item'")); return;
                }
                String itemId = body.get("item").getAsString();
                int count = body.has("count") ? body.get("count").getAsInt() : 1;
                JsonArray enchs = body.has("enchantments") ? body.getAsJsonArray("enchantments") : null;
                Boolean ok = runOnMain(server, () -> {
                    ServerPlayer p = server.getPlayerList().getPlayer(uuid);
                    if (p == null) return false;
                    ItemStack stack = AdminApiSerializer.buildItem(itemId, count, enchs);
                    if (stack.isEmpty()) return false;
                    boolean inserted = p.getInventory().add(stack);
                    if (!inserted) p.drop(stack, false);
                    return true;
                });
                writeJson(ex, ok ? 200 : 400,
                        ok ? AdminApiSerializer.okResponse()
                           : AdminApiSerializer.errorResponse("player offline or invalid item"));
            }
            case "remove" -> {
                if (!"POST".equals(method)) { writeJson(ex, 405, AdminApiSerializer.errorResponse("method")); return; }
                JsonObject body = readJson(ex);
                int slot = body != null && body.has("slot") ? body.get("slot").getAsInt() : -1;
                int count = body != null && body.has("count") ? body.get("count").getAsInt() : 64;
                Boolean ok = runOnMain(server, () -> {
                    ServerPlayer p = server.getPlayerList().getPlayer(uuid);
                    if (p == null) return false;
                    if (slot < 0 || slot >= p.getInventory().getContainerSize()) return false;
                    p.getInventory().removeItem(slot, count);
                    return true;
                });
                writeJson(ex, ok ? 200 : 400,
                        ok ? AdminApiSerializer.okResponse()
                           : AdminApiSerializer.errorResponse("invalid slot or offline"));
            }
            case "clear" -> {
                if (!"POST".equals(method)) { writeJson(ex, 405, AdminApiSerializer.errorResponse("method")); return; }
                Boolean ok = runOnMain(server, () -> {
                    ServerPlayer p = server.getPlayerList().getPlayer(uuid);
                    if (p == null) return false;
                    p.getInventory().clearContent();
                    return true;
                });
                writeJson(ex, ok ? 200 : 404,
                        ok ? AdminApiSerializer.okResponse()
                           : AdminApiSerializer.errorResponse("player offline"));
            }
            case "effect" -> {
                if (!"POST".equals(method)) { writeJson(ex, 405, AdminApiSerializer.errorResponse("method")); return; }
                JsonObject body = readJson(ex);
                if (body == null || !body.has("effect")) {
                    writeJson(ex, 400, AdminApiSerializer.errorResponse("missing 'effect'")); return;
                }
                String effId = body.get("effect").getAsString();
                int duration = body.has("duration") ? body.get("duration").getAsInt() : 600; // ticks
                int amplifier = body.has("amplifier") ? body.get("amplifier").getAsInt() : 0;
                Boolean ok = runOnMain(server, () -> {
                    ServerPlayer p = server.getPlayerList().getPlayer(uuid);
                    if (p == null) return false;
                    MobEffect eff = ForgeRegistries.MOB_EFFECTS.getValue(ResourceLocation.tryParse(effId));
                    if (eff == null) return false;
                    p.addEffect(new MobEffectInstance(eff, duration, amplifier));
                    return true;
                });
                writeJson(ex, ok ? 200 : 400,
                        ok ? AdminApiSerializer.okResponse()
                           : AdminApiSerializer.errorResponse("offline or invalid effect"));
            }
            case "teleport" -> {
                if (!"POST".equals(method)) { writeJson(ex, 405, AdminApiSerializer.errorResponse("method")); return; }
                JsonObject body = readJson(ex);
                if (body == null) { writeJson(ex, 400, AdminApiSerializer.errorResponse("missing body")); return; }
                double x = body.get("x").getAsDouble();
                double y = body.get("y").getAsDouble();
                double z = body.get("z").getAsDouble();
                String dim = body.has("dimension") ? body.get("dimension").getAsString() : null;
                Boolean ok = runOnMain(server, () -> {
                    ServerPlayer p = server.getPlayerList().getPlayer(uuid);
                    if (p == null) return false;
                    ServerLevel target = p.serverLevel();
                    if (dim != null) {
                        ResourceKey<Level> dk = ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,
                                ResourceLocation.tryParse(dim));
                        ServerLevel tl = server.getLevel(dk);
                        if (tl != null) target = tl;
                    }
                    p.teleportTo(target, x, y, z, p.getYRot(), p.getXRot());
                    return true;
                });
                writeJson(ex, ok ? 200 : 400,
                        ok ? AdminApiSerializer.okResponse()
                           : AdminApiSerializer.errorResponse("teleport failed"));
            }
            case "title" -> {
                if (!"POST".equals(method)) { writeJson(ex, 405, AdminApiSerializer.errorResponse("method")); return; }
                JsonObject body = readJson(ex);
                if (body == null) { writeJson(ex, 400, AdminApiSerializer.errorResponse("missing body")); return; }
                String title = body.has("title") ? body.get("title").getAsString() : "";
                String subtitle = body.has("subtitle") ? body.get("subtitle").getAsString() : "";
                int fadeIn = body.has("fadeIn") ? body.get("fadeIn").getAsInt() : 10;
                int stay = body.has("stay") ? body.get("stay").getAsInt() : 70;
                int fadeOut = body.has("fadeOut") ? body.get("fadeOut").getAsInt() : 20;
                Boolean ok = runOnMain(server, () -> {
                    ServerPlayer p = server.getPlayerList().getPlayer(uuid);
                    if (p == null) return false;
                    p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket(fadeIn, stay, fadeOut));
                    if (!subtitle.isEmpty()) {
                        p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket(Component.literal(subtitle)));
                    }
                    p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket(Component.literal(title)));
                    return true;
                });
                writeJson(ex, ok ? 200 : 404,
                        ok ? AdminApiSerializer.okResponse() : AdminApiSerializer.errorResponse("player offline"));
            }
            case "sound" -> {
                if (!"POST".equals(method)) { writeJson(ex, 405, AdminApiSerializer.errorResponse("method")); return; }
                JsonObject body = readJson(ex);
                if (body == null || !body.has("sound")) { writeJson(ex, 400, AdminApiSerializer.errorResponse("missing 'sound'")); return; }
                String soundId = body.get("sound").getAsString();
                float volume = body.has("volume") ? body.get("volume").getAsFloat() : 1f;
                float pitch = body.has("pitch") ? body.get("pitch").getAsFloat() : 1f;
                Boolean ok = runOnMain(server, () -> {
                    ServerPlayer p = server.getPlayerList().getPlayer(uuid);
                    if (p == null) return false;
                    net.minecraft.sounds.SoundEvent sound = ForgeRegistries.SOUND_EVENTS.getValue(ResourceLocation.tryParse(soundId));
                    if (sound == null) return false;
                    p.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(
                            net.minecraft.core.Holder.direct(sound),
                            net.minecraft.sounds.SoundSource.MASTER,
                            p.getX(), p.getY(), p.getZ(), volume, pitch, p.level().getRandom().nextLong()));
                    return true;
                });
                writeJson(ex, ok ? 200 : 400,
                        ok ? AdminApiSerializer.okResponse() : AdminApiSerializer.errorResponse("offline or invalid sound"));
            }
            case "lightning" -> {
                if (!"POST".equals(method)) { writeJson(ex, 405, AdminApiSerializer.errorResponse("method")); return; }
                Boolean ok = runOnMain(server, () -> {
                    ServerPlayer p = server.getPlayerList().getPlayer(uuid);
                    if (p == null) return false;
                    net.minecraft.world.entity.LightningBolt bolt = net.minecraft.world.entity.EntityType.LIGHTNING_BOLT.create(p.serverLevel());
                    if (bolt == null) return false;
                    bolt.moveTo(p.getX(), p.getY(), p.getZ());
                    bolt.setVisualOnly(false);
                    p.serverLevel().addFreshEntity(bolt);
                    return true;
                });
                writeJson(ex, ok ? 200 : 404,
                        ok ? AdminApiSerializer.okResponse() : AdminApiSerializer.errorResponse("offline"));
            }
            case "heal" -> {
                if (!"POST".equals(method)) { writeJson(ex, 405, AdminApiSerializer.errorResponse("method")); return; }
                Boolean ok = runOnMain(server, () -> {
                    ServerPlayer p = server.getPlayerList().getPlayer(uuid);
                    if (p == null) return false;
                    p.setHealth(p.getMaxHealth());
                    p.getFoodData().setFoodLevel(20);
                    p.getFoodData().setSaturation(20f);
                    p.clearFire();
                    p.removeAllEffects();
                    return true;
                });
                writeJson(ex, ok ? 200 : 404,
                        ok ? AdminApiSerializer.okResponse() : AdminApiSerializer.errorResponse("offline"));
            }
            case "xp" -> {
                if (!"POST".equals(method)) { writeJson(ex, 405, AdminApiSerializer.errorResponse("method")); return; }
                JsonObject body = readJson(ex);
                if (body == null) { writeJson(ex, 400, AdminApiSerializer.errorResponse("missing body")); return; }
                String mode = body.has("mode") ? body.get("mode").getAsString() : "add"; // add|set
                int levels = body.has("levels") ? body.get("levels").getAsInt() : 0;
                int points = body.has("points") ? body.get("points").getAsInt() : 0;
                Boolean ok = runOnMain(server, () -> {
                    ServerPlayer p = server.getPlayerList().getPlayer(uuid);
                    if (p == null) return false;
                    if ("set".equals(mode)) {
                        p.experienceLevel = 0;
                        p.experienceProgress = 0;
                        p.totalExperience = 0;
                        if (levels > 0) p.giveExperienceLevels(levels);
                        if (points > 0) p.giveExperiencePoints(points);
                    } else {
                        if (levels != 0) p.giveExperienceLevels(levels);
                        if (points != 0) p.giveExperiencePoints(points);
                    }
                    return true;
                });
                writeJson(ex, ok ? 200 : 404,
                        ok ? AdminApiSerializer.okResponse() : AdminApiSerializer.errorResponse("offline"));
            }
            case "gamemode" -> {
                if (!"POST".equals(method)) { writeJson(ex, 405, AdminApiSerializer.errorResponse("method")); return; }
                JsonObject body = readJson(ex);
                if (body == null || !body.has("mode")) { writeJson(ex, 400, AdminApiSerializer.errorResponse("missing 'mode'")); return; }
                String mode = body.get("mode").getAsString().toUpperCase();
                Boolean ok = runOnMain(server, () -> {
                    ServerPlayer p = server.getPlayerList().getPlayer(uuid);
                    if (p == null) return false;
                    try {
                        p.setGameMode(net.minecraft.world.level.GameType.valueOf(mode));
                        return true;
                    } catch (Exception e) { return false; }
                });
                writeJson(ex, ok ? 200 : 400,
                        ok ? AdminApiSerializer.okResponse() : AdminApiSerializer.errorResponse("offline or invalid mode"));
            }
            case "feed" -> {
                if (!"POST".equals(method)) { writeJson(ex, 405, AdminApiSerializer.errorResponse("method")); return; }
                Boolean ok = runOnMain(server, () -> {
                    ServerPlayer p = server.getPlayerList().getPlayer(uuid);
                    if (p == null) return false;
                    p.getFoodData().setFoodLevel(20);
                    p.getFoodData().setSaturation(20f);
                    return true;
                });
                writeJson(ex, ok ? 200 : 404,
                        ok ? AdminApiSerializer.okResponse() : AdminApiSerializer.errorResponse("offline"));
            }
            case "tp-to" -> {
                if (!"POST".equals(method)) { writeJson(ex, 405, AdminApiSerializer.errorResponse("method")); return; }
                JsonObject body = readJson(ex);
                if (body == null || !body.has("targetUuid")) { writeJson(ex, 400, AdminApiSerializer.errorResponse("missing 'targetUuid'")); return; }
                UUID targetUuid;
                try { targetUuid = UUID.fromString(body.get("targetUuid").getAsString()); }
                catch (Exception e) { writeJson(ex, 400, AdminApiSerializer.errorResponse("invalid targetUuid")); return; }
                Boolean ok = runOnMain(server, () -> {
                    ServerPlayer p = server.getPlayerList().getPlayer(uuid);
                    ServerPlayer target = server.getPlayerList().getPlayer(targetUuid);
                    if (p == null || target == null) return false;
                    p.teleportTo(target.serverLevel(), target.getX(), target.getY(), target.getZ(), target.getYRot(), target.getXRot());
                    return true;
                });
                writeJson(ex, ok ? 200 : 404,
                        ok ? AdminApiSerializer.okResponse() : AdminApiSerializer.errorResponse("offline"));
            }
            case "restore" -> {
                if (!"POST".equals(method)) { writeJson(ex, 405, AdminApiSerializer.errorResponse("method")); return; }
                JsonObject body = readJson(ex);
                if (body == null) { writeJson(ex, 400, AdminApiSerializer.errorResponse("missing body")); return; }
                long ts = body.has("ts") ? body.get("ts").getAsLong() : 0;
                boolean restoreInv = !body.has("restoreInventory") || body.get("restoreInventory").getAsBoolean();
                boolean restoreStats = body.has("restoreStats") && body.get("restoreStats").getAsBoolean();
                boolean restorePos = body.has("restorePosition") && body.get("restorePosition").getAsBoolean();
                JsonObject snap;
                if (body.has("snapshot")) snap = body.getAsJsonObject("snapshot");
                else snap = AdminSnapshotScheduler.readSnapshot(uuid.toString(), ts);
                if (snap == null) { writeJson(ex, 404, AdminApiSerializer.errorResponse("snapshot not found")); return; }
                JsonObject result = runOnMain(server, () -> AdminSnapshotScheduler.restoreFromSnapshot(
                        server.getPlayerList().getPlayer(uuid), snap, restoreInv, restoreStats, restorePos));
                if (result == null) writeJson(ex, 500, AdminApiSerializer.errorResponse("restore failed"));
                else writeJson(ex, 200, result);
            }
            case "kick" -> {
                if (!"POST".equals(method)) { writeJson(ex, 405, AdminApiSerializer.errorResponse("method")); return; }
                JsonObject body = readJson(ex);
                String reason = body != null && body.has("reason") ? body.get("reason").getAsString() : "Kicked by admin";
                Boolean ok = runOnMain(server, () -> {
                    ServerPlayer p = server.getPlayerList().getPlayer(uuid);
                    if (p == null) return false;
                    p.connection.disconnect(Component.literal(reason));
                    return true;
                });
                writeJson(ex, ok ? 200 : 404,
                        ok ? AdminApiSerializer.okResponse()
                           : AdminApiSerializer.errorResponse("player offline"));
            }
            case "freeze" -> {
                if (!"POST".equals(method)) { writeJson(ex, 405, AdminApiSerializer.errorResponse("method")); return; }
                Boolean ok = runOnMain(server, () -> {
                    ServerPlayer p = server.getPlayerList().getPlayer(uuid);
                    if (p == null) return false;
                    br.com.murilo.liberthia.freeze.FreezeManager.freeze(p);
                    return true;
                });
                writeJson(ex, ok ? 200 : 404,
                        ok ? AdminApiSerializer.okResponse()
                           : AdminApiSerializer.errorResponse("player offline"));
            }
            case "unfreeze" -> {
                if (!"POST".equals(method)) { writeJson(ex, 405, AdminApiSerializer.errorResponse("method")); return; }
                Boolean ok = runOnMain(server, () -> {
                    br.com.murilo.liberthia.freeze.FreezeManager.unfreeze(uuid);
                    return true;
                });
                writeJson(ex, ok ? 200 : 500,
                        ok ? AdminApiSerializer.okResponse()
                           : AdminApiSerializer.errorResponse("unfreeze failed"));
            }
            case "freeze-status" -> {
                if (!"GET".equals(method)) { writeJson(ex, 405, AdminApiSerializer.errorResponse("method")); return; }
                var state = br.com.murilo.liberthia.freeze.FreezeManager.getState(uuid);
                JsonObject o = new JsonObject();
                o.addProperty("frozen", state != null);
                if (state != null) {
                    o.addProperty("anchorX", state.anchorX);
                    o.addProperty("anchorY", state.anchorY);
                    o.addProperty("anchorZ", state.anchorZ);
                    o.addProperty("moveAttempts", state.moveAttempts);
                }
                writeJson(ex, 200, o);
            }
            default -> writeJson(ex, 404, AdminApiSerializer.errorResponse("unknown action: " + action));
        }
    }

    private void handleMatter(HttpExchange ex, String path, String method) throws IOException {
        UUID uuid;
        try {
            uuid = UUID.fromString(path.substring("/api/matter/".length()));
        } catch (IllegalArgumentException e) {
            writeJson(ex, 400, AdminApiSerializer.errorResponse("invalid uuid"));
            return;
        }
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) { writeJson(ex, 503, AdminApiSerializer.errorResponse("server not running")); return; }

        if ("GET".equals(method)) {
            JsonObject result = runOnMain(server, () -> {
                ServerPlayer p = server.getPlayerList().getPlayer(uuid);
                if (p == null) return null;
                JsonObject o = new JsonObject();
                p.getCapability(br.com.murilo.liberthia.matter.MatterProfileProvider.CAP).ifPresent(profile -> {
                    o.addProperty("dm", profile.getDark());
                    o.addProperty("wm", profile.getWhite());
                    o.addProperty("ym", profile.getYellow());
                    o.addProperty("type", profile.getActiveType().name());
                });
                return o;
            });
            if (result == null) writeJson(ex, 404, AdminApiSerializer.errorResponse("player offline"));
            else writeJson(ex, 200, result);
            return;
        }

        if ("POST".equals(method)) {
            JsonObject body = readJson(ex);
            if (body == null) { writeJson(ex, 400, AdminApiSerializer.errorResponse("missing body")); return; }
            Boolean ok = runOnMain(server, () -> {
                ServerPlayer p = server.getPlayerList().getPlayer(uuid);
                if (p == null) return false;
                p.getCapability(br.com.murilo.liberthia.matter.MatterProfileProvider.CAP).ifPresent(profile -> {
                    if (body.has("dm")) profile.setDark(body.get("dm").getAsFloat());
                    if (body.has("wm")) profile.setWhite(body.get("wm").getAsFloat());
                    if (body.has("ym")) profile.setYellow(body.get("ym").getAsFloat());
                });
                return true;
            });
            writeJson(ex, ok ? 200 : 404,
                    ok ? AdminApiSerializer.okResponse()
                       : AdminApiSerializer.errorResponse("player offline"));
            return;
        }
        writeJson(ex, 405, AdminApiSerializer.errorResponse("method not allowed"));
    }

    private void handleWorldTime(HttpExchange ex) throws IOException {
        JsonObject body = readJson(ex);
        if (body == null || !body.has("time")) { writeJson(ex, 400, AdminApiSerializer.errorResponse("missing 'time'")); return; }
        long time = body.get("time").getAsLong();
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) { writeJson(ex, 503, AdminApiSerializer.errorResponse("server not running")); return; }
        runOnMain(server, () -> {
            for (ServerLevel l : server.getAllLevels()) l.setDayTime(time);
            return true;
        });
        writeJson(ex, 200, AdminApiSerializer.okResponse());
    }

    private void handleWorldWeather(HttpExchange ex) throws IOException {
        JsonObject body = readJson(ex);
        if (body == null || !body.has("type")) { writeJson(ex, 400, AdminApiSerializer.errorResponse("missing 'type'")); return; }
        String type = body.get("type").getAsString(); // "clear", "rain", "thunder"
        int duration = body.has("duration") ? body.get("duration").getAsInt() : 6000;
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) { writeJson(ex, 503, AdminApiSerializer.errorResponse("server not running")); return; }
        runOnMain(server, () -> {
            ServerLevel ow = server.overworld();
            switch (type) {
                case "clear" -> ow.setWeatherParameters(duration, 0, false, false);
                case "rain"  -> ow.setWeatherParameters(0, duration, true, false);
                case "thunder" -> ow.setWeatherParameters(0, duration, true, true);
            }
            return true;
        });
        writeJson(ex, 200, AdminApiSerializer.okResponse());
    }

    private void handleWorldDifficulty(HttpExchange ex) throws IOException {
        JsonObject body = readJson(ex);
        if (body == null || !body.has("difficulty")) { writeJson(ex, 400, AdminApiSerializer.errorResponse("missing 'difficulty'")); return; }
        String diff = body.get("difficulty").getAsString().toLowerCase();
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) { writeJson(ex, 503, AdminApiSerializer.errorResponse("server not running")); return; }
        runOnMain(server, () -> {
            net.minecraft.world.Difficulty d;
            try { d = net.minecraft.world.Difficulty.valueOf(diff.toUpperCase()); }
            catch (Exception e) { return false; }
            server.setDifficulty(d, true);
            return true;
        });
        writeJson(ex, 200, AdminApiSerializer.okResponse());
    }

    private void handleOperators(HttpExchange ex) throws IOException {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) { writeJson(ex, 503, AdminApiSerializer.errorResponse("server not running")); return; }
        JsonArray arr = new JsonArray();
        for (String name : server.getPlayerList().getOps().getUserList()) {
            JsonObject o = new JsonObject();
            o.addProperty("name", name);
            arr.add(o);
        }
        JsonObject result = new JsonObject();
        result.add("operators", arr);
        writeJson(ex, 200, result);
    }

    private void handleBans(HttpExchange ex) throws IOException {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) { writeJson(ex, 503, AdminApiSerializer.errorResponse("server not running")); return; }
        JsonArray players = new JsonArray();
        for (String name : server.getPlayerList().getBans().getUserList()) {
            JsonObject o = new JsonObject();
            o.addProperty("name", name);
            players.add(o);
        }
        JsonArray ips = new JsonArray();
        for (String ip : server.getPlayerList().getIpBans().getUserList()) {
            JsonObject o = new JsonObject();
            o.addProperty("ip", ip);
            ips.add(o);
        }
        JsonObject result = new JsonObject();
        result.add("playerBans", players);
        result.add("ipBans", ips);
        writeJson(ex, 200, result);
    }

    private void handleWhitelist(HttpExchange ex) throws IOException {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) { writeJson(ex, 503, AdminApiSerializer.errorResponse("server not running")); return; }
        JsonArray arr = new JsonArray();
        for (String name : server.getPlayerList().getWhiteList().getUserList()) {
            JsonObject o = new JsonObject();
            o.addProperty("name", name);
            arr.add(o);
        }
        JsonObject result = new JsonObject();
        result.addProperty("enabled", server.getPlayerList().isUsingWhitelist());
        result.add("whitelist", arr);
        writeJson(ex, 200, result);
    }

    private void handleSaveAll(HttpExchange ex) throws IOException {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) { writeJson(ex, 503, AdminApiSerializer.errorResponse("server not running")); return; }
        runOnMain(server, () -> {
            server.saveAllChunks(true, true, true);
            return true;
        });
        writeJson(ex, 200, AdminApiSerializer.okResponse());
    }

    private void handleBroadcast(HttpExchange ex) throws IOException {
        JsonObject body = readJson(ex);
        if (body == null || !body.has("message")) { writeJson(ex, 400, AdminApiSerializer.errorResponse("missing 'message'")); return; }
        String msg = body.get("message").getAsString();
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) { writeJson(ex, 503, AdminApiSerializer.errorResponse("server not running")); return; }
        runOnMain(server, () -> {
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                p.sendSystemMessage(net.minecraft.network.chat.Component.literal(msg));
            }
            return true;
        });
        writeJson(ex, 200, AdminApiSerializer.okResponse());
    }

    private void handleSpawnEntity(HttpExchange ex) throws IOException {
        JsonObject body = readJson(ex);
        if (body == null || !body.has("entity")) { writeJson(ex, 400, AdminApiSerializer.errorResponse("missing 'entity'")); return; }
        String entityId = body.get("entity").getAsString();
        double x = body.get("x").getAsDouble();
        double y = body.get("y").getAsDouble();
        double z = body.get("z").getAsDouble();
        int count = body.has("count") ? body.get("count").getAsInt() : 1;
        String dim = body.has("dimension") ? body.get("dimension").getAsString() : null;
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) { writeJson(ex, 503, AdminApiSerializer.errorResponse("server not running")); return; }
        Boolean ok = runOnMain(server, () -> {
            ServerLevel level = server.overworld();
            if (dim != null) {
                ResourceKey<Level> dk = ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,
                        ResourceLocation.tryParse(dim));
                ServerLevel tl = server.getLevel(dk);
                if (tl != null) level = tl;
            }
            net.minecraft.world.entity.EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(ResourceLocation.tryParse(entityId));
            if (type == null) return false;
            for (int i = 0; i < Math.min(count, 50); i++) {
                net.minecraft.world.entity.Entity e = type.create(level);
                if (e == null) continue;
                e.moveTo(x, y, z, level.getRandom().nextFloat() * 360f, 0);
                level.addFreshEntity(e);
            }
            return true;
        });
        writeJson(ex, ok ? 200 : 400,
                ok ? AdminApiSerializer.okResponse() : AdminApiSerializer.errorResponse("invalid entity"));
    }

    /**
     * Spawna uma ClonePlayerEntity em (x,y,z) renderizada com a skin do player
     * cujo nome (ou UUID) foi passado. Aceita tag pra agrupamento (permite
     * /kill @e[tag=...] depois). NoAi, NoGravity, Invulnerable — entidade "estátua".
     *
     * Body: {x, y, z, dim?, playerName?, playerUuid?, rotation?, tag?, customName?}
     */
    private void handleSpawnPlayerClone(HttpExchange ex) throws IOException {
        JsonObject body = readJson(ex);
        if (body == null) { writeJson(ex, 400, AdminApiSerializer.errorResponse("missing body")); return; }
        double x = body.get("x").getAsDouble();
        double y = body.get("y").getAsDouble();
        double z = body.get("z").getAsDouble();
        String dim = body.has("dimension") ? body.get("dimension").getAsString() : null;
        String pName = body.has("playerName") ? body.get("playerName").getAsString() : null;
        String pUuidStr = body.has("playerUuid") ? body.get("playerUuid").getAsString() : null;
        float rotation = body.has("rotation") ? body.get("rotation").getAsFloat() : 0f;
        String tag = body.has("tag") ? body.get("tag").getAsString() : null;
        String customName = body.has("customName") ? body.get("customName").getAsString() : null;

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) { writeJson(ex, 503, AdminApiSerializer.errorResponse("server not running")); return; }

        String result = runOnMain(server, () -> {
            ServerLevel level = server.overworld();
            if (dim != null && !dim.isBlank()) {
                String full = dim.contains(":") ? dim : ("minecraft:" + dim);
                ResourceKey<Level> dk = ResourceKey.create(
                        net.minecraft.core.registries.Registries.DIMENSION,
                        ResourceLocation.tryParse(full));
                ServerLevel tl = server.getLevel(dk);
                if (tl != null) level = tl;
            }

            // Resolve UUID + nome do player
            java.util.UUID ownerUuid = null;
            String ownerName = pName;
            if (pUuidStr != null && !pUuidStr.isBlank()) {
                try { ownerUuid = java.util.UUID.fromString(pUuidStr); } catch (Exception ignored) {}
            }
            if (ownerUuid == null && pName != null && !pName.isBlank()) {
                // Tenta profile cache do servidor (offline-aware)
                var profile = server.getProfileCache().get(pName).orElse(null);
                if (profile != null) {
                    ownerUuid = profile.getId();
                    ownerName = profile.getName();
                }
            }
            if (ownerUuid == null) {
                // Fallback: gera UUID estável a partir do nome (offline-mode hash)
                String fallbackName = ownerName == null ? "Steve" : ownerName;
                ownerUuid = java.util.UUID.nameUUIDFromBytes(("OfflinePlayer:" + fallbackName).getBytes(java.nio.charset.StandardCharsets.UTF_8));
                ownerName = fallbackName;
            }

            br.com.murilo.liberthia.entity.ClonePlayerEntity clone =
                    br.com.murilo.liberthia.registry.ModEntities.CLONE_PLAYER.get().create(level);
            if (clone == null) return null;

            clone.moveTo(x, y, z, rotation, 0f);
            clone.setOwnerUuid(ownerUuid);
            clone.setOwnerName(ownerName);
            clone.setNoAi(true);
            clone.setInvulnerable(true);
            if (customName != null && !customName.isBlank()) {
                clone.setCustomName(net.minecraft.network.chat.Component.literal(customName));
                clone.setCustomNameVisible(true);
            }
            if (tag != null && !tag.isBlank()) {
                clone.addTag("liberthia_clone");
                for (String t : tag.split(",")) {
                    String trimmed = t.trim();
                    if (!trimmed.isEmpty()) clone.addTag(trimmed);
                }
            } else {
                clone.addTag("liberthia_clone");
            }

            level.addFreshEntity(clone);
            return clone.getUUID().toString();
        });

        if (result == null) {
            writeJson(ex, 500, AdminApiSerializer.errorResponse("failed to create clone"));
            return;
        }
        JsonObject ok = new JsonObject();
        ok.addProperty("ok", true);
        ok.addProperty("uuid", result);
        writeJson(ex, 200, ok);
    }

    /**
     * Toca um clipe de voz salvo no backend dentro do mundo. Body:
     *   {clipId, x, y, z, dimension, volume, category}
     * Mod fetcha o WAV do backend e replay via Simple Voice Chat.
     */
    private void handleVoicePlay(HttpExchange ex) throws IOException {
        JsonObject body = readJson(ex);
        if (body == null || !body.has("clipId")) {
            writeJson(ex, 400, AdminApiSerializer.errorResponse("missing 'clipId'"));
            return;
        }
        long clipId = body.get("clipId").getAsLong();
        double x = body.has("x") ? body.get("x").getAsDouble() : 0;
        double y = body.has("y") ? body.get("y").getAsDouble() : 64;
        double z = body.has("z") ? body.get("z").getAsDouble() : 0;
        String dim = body.has("dimension") ? body.get("dimension").getAsString() : "minecraft:overworld";
        float volume = body.has("volume") ? body.get("volume").getAsFloat() : 1.0f;
        String category = body.has("category") ? body.get("category").getAsString() : "liberthia_voice";

        if (!br.com.murilo.liberthia.voice.LiberthiaVoicePlugin.isActive()) {
            writeJson(ex, 503, AdminApiSerializer.errorResponse("voice plugin not active (Simple Voice Chat missing?)"));
            return;
        }

        java.util.UUID playId = br.com.murilo.liberthia.voice.VoicePlaybackManager.playAtLocation(
                clipId, dim, x, y, z, volume, category);
        if (playId == null) {
            writeJson(ex, 500, AdminApiSerializer.errorResponse("playback failed"));
            return;
        }
        JsonObject ok = new JsonObject();
        ok.addProperty("ok", true);
        ok.addProperty("playId", playId.toString());
        writeJson(ex, 200, ok);
    }

    /**
     * Mata entidades por tag scoreboard. Suporta múltiplas tags via vírgula
     * (todas precisam estar presentes — comportamento do selector tag=).
     */
    private void handleKillByTag(HttpExchange ex) throws IOException {
        JsonObject body = readJson(ex);
        if (body == null || !body.has("tag")) {
            writeJson(ex, 400, AdminApiSerializer.errorResponse("missing 'tag'"));
            return;
        }
        String tag = body.get("tag").getAsString().trim();
        if (tag.isEmpty()) { writeJson(ex, 400, AdminApiSerializer.errorResponse("empty tag")); return; }

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) { writeJson(ex, 503, AdminApiSerializer.errorResponse("server not running")); return; }

        Integer killed = runOnMain(server, () -> {
            int count = 0;
            for (ServerLevel level : server.getAllLevels()) {
                java.util.List<net.minecraft.world.entity.Entity> hits = new java.util.ArrayList<>();
                level.getEntities().getAll().forEach(e -> {
                    if (e.getTags().contains(tag)) hits.add(e);
                });
                for (var e : hits) { e.discard(); count++; }
            }
            return count;
        });
        JsonObject ok = new JsonObject();
        ok.addProperty("ok", true);
        ok.addProperty("killed", killed == null ? 0 : killed);
        writeJson(ex, 200, ok);
    }

    private void handleParticle(HttpExchange ex) throws IOException {
        JsonObject body = readJson(ex);
        if (body == null || !body.has("particle")) { writeJson(ex, 400, AdminApiSerializer.errorResponse("missing 'particle'")); return; }
        String pid = body.get("particle").getAsString();
        double x = body.get("x").getAsDouble();
        double y = body.get("y").getAsDouble();
        double z = body.get("z").getAsDouble();
        int count = body.has("count") ? body.get("count").getAsInt() : 30;
        double dx = body.has("dx") ? body.get("dx").getAsDouble() : 0.5;
        double dy = body.has("dy") ? body.get("dy").getAsDouble() : 0.5;
        double dz = body.has("dz") ? body.get("dz").getAsDouble() : 0.5;
        double speed = body.has("speed") ? body.get("speed").getAsDouble() : 0.05;
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) { writeJson(ex, 503, AdminApiSerializer.errorResponse("server not running")); return; }
        Boolean ok = runOnMain(server, () -> {
            net.minecraft.core.particles.ParticleType<?> raw = ForgeRegistries.PARTICLE_TYPES.getValue(ResourceLocation.tryParse(pid));
            if (!(raw instanceof net.minecraft.core.particles.ParticleOptions opt)) return false;
            for (ServerLevel l : server.getAllLevels()) {
                l.sendParticles(opt, x, y, z, count, dx, dy, dz, speed);
            }
            return true;
        });
        writeJson(ex, ok ? 200 : 400,
                ok ? AdminApiSerializer.okResponse() : AdminApiSerializer.errorResponse("invalid particle"));
    }

    private void handleExplosion(HttpExchange ex) throws IOException {
        JsonObject body = readJson(ex);
        if (body == null) { writeJson(ex, 400, AdminApiSerializer.errorResponse("missing body")); return; }
        double x = body.get("x").getAsDouble();
        double y = body.get("y").getAsDouble();
        double z = body.get("z").getAsDouble();
        float power = body.has("power") ? body.get("power").getAsFloat() : 4f;
        boolean fire = body.has("fire") && body.get("fire").getAsBoolean();
        boolean blockDamage = body.has("blockDamage") && body.get("blockDamage").getAsBoolean();
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) { writeJson(ex, 503, AdminApiSerializer.errorResponse("server not running")); return; }
        runOnMain(server, () -> {
            server.overworld().explode(null, x, y, z, Math.min(power, 16f), fire,
                    blockDamage ? Level.ExplosionInteraction.TNT : Level.ExplosionInteraction.NONE);
            return true;
        });
        writeJson(ex, 200, AdminApiSerializer.okResponse());
    }

    /**
     * Whisper proximity — envia mensagem (chat + opcional title/subtitle/sound)
     * APENAS pra players dentro de `radius` blocos do ponto (x,y,z) na dimensão.
     * Body: {x,y,z, radius, dimension?, message?, title?, subtitle?, sound?, speaker?}
     * Retorna: { ok, recipients: [uuid, ...] }
     */
    private void handleWhisper(HttpExchange ex) throws IOException {
        JsonObject body = readJson(ex);
        if (body == null) { writeJson(ex, 400, AdminApiSerializer.errorResponse("missing body")); return; }
        double x = body.get("x").getAsDouble();
        double y = body.get("y").getAsDouble();
        double z = body.get("z").getAsDouble();
        double radius = body.has("radius") ? body.get("radius").getAsDouble() : 12.0;
        double r2 = radius * radius;
        String dim = body.has("dimension") ? body.get("dimension").getAsString() : null;
        String message = body.has("message") ? body.get("message").getAsString() : null;
        String title = body.has("title") ? body.get("title").getAsString() : null;
        String subtitle = body.has("subtitle") ? body.get("subtitle").getAsString() : null;
        String sound = body.has("sound") ? body.get("sound").getAsString() : null;
        String speaker = body.has("speaker") ? body.get("speaker").getAsString() : null;

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) { writeJson(ex, 503, AdminApiSerializer.errorResponse("server not running")); return; }

        JsonArray recipients = runOnMain(server, () -> {
            JsonArray arr = new JsonArray();
            ServerLevel level = dim == null ? server.overworld() :
                    server.getLevel(net.minecraft.resources.ResourceKey.create(
                            net.minecraft.core.registries.Registries.DIMENSION,
                            net.minecraft.resources.ResourceLocation.tryParse(dim)));
            if (level == null) level = server.overworld();
            for (ServerPlayer p : level.players()) {
                double dx = p.getX() - x, dy = p.getY() - y, dz = p.getZ() - z;
                double d2 = dx * dx + dy * dy + dz * dz;
                if (d2 > r2) continue;
                // Chat: tellraw equivalente via system message (não polui chat global)
                if (message != null && !message.isEmpty()) {
                    String full = (speaker != null && !speaker.isEmpty())
                            ? "§e[" + speaker + "]§r " + message : message;
                    p.sendSystemMessage(net.minecraft.network.chat.Component.literal(full));
                }
                if (title != null) {
                    p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket(
                            net.minecraft.network.chat.Component.literal(title)));
                }
                if (subtitle != null) {
                    p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket(
                            net.minecraft.network.chat.Component.literal(subtitle)));
                }
                if (sound != null) {
                    var soundEvent = ForgeRegistries.SOUND_EVENTS.getValue(net.minecraft.resources.ResourceLocation.tryParse(sound));
                    if (soundEvent != null) {
                        p.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(
                                net.minecraft.core.Holder.direct(soundEvent),
                                net.minecraft.sounds.SoundSource.MASTER,
                                p.getX(), p.getY(), p.getZ(), 1f, 1f, p.level().getRandom().nextLong()));
                    }
                }
                JsonObject rec = new JsonObject();
                rec.addProperty("uuid", p.getUUID().toString());
                rec.addProperty("name", p.getGameProfile().getName());
                rec.addProperty("distance", Math.sqrt(d2));
                arr.add(rec);
            }
            return arr;
        });

        JsonObject o = new JsonObject();
        o.addProperty("ok", true);
        o.add("recipients", recipients == null ? new JsonArray() : recipients);
        writeJson(ex, 200, o);
    }

    private void handleHealAll(HttpExchange ex) throws IOException {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) { writeJson(ex, 503, AdminApiSerializer.errorResponse("server not running")); return; }
        Integer healed = runOnMain(server, () -> {
            int n = 0;
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                p.setHealth(p.getMaxHealth());
                p.getFoodData().setFoodLevel(20);
                p.getFoodData().setSaturation(20f);
                p.clearFire();
                p.removeAllEffects();
                n++;
            }
            return n;
        });
        JsonObject o = new JsonObject();
        o.addProperty("ok", true);
        o.addProperty("healed", healed == null ? 0 : healed);
        writeJson(ex, 200, o);
    }

    private void handleBackup(HttpExchange ex) throws IOException {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) { writeJson(ex, 503, AdminApiSerializer.errorResponse("server not running")); return; }
        // Save first
        runOnMain(server, () -> { server.saveAllChunks(true, true, true); return true; });
        // Then copy world dir to backups/<timestamp>
        try {
            java.nio.file.Path worldRoot = server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT);
            java.nio.file.Path backupsDir = worldRoot.getParent().resolve("liberthia_backups");
            java.nio.file.Files.createDirectories(backupsDir);
            String stamp = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            java.nio.file.Path target = backupsDir.resolve("backup_" + stamp + ".zip");
            zipDirectory(worldRoot, target);
            JsonObject o = new JsonObject();
            o.addProperty("ok", true);
            o.addProperty("file", target.toString());
            o.addProperty("sizeBytes", java.nio.file.Files.size(target));
            writeJson(ex, 200, o);
        } catch (Exception e) {
            writeJson(ex, 500, AdminApiSerializer.errorResponse("backup failed: " + e.getMessage()));
        }
    }

    private static void zipDirectory(java.nio.file.Path source, java.nio.file.Path zipFile) throws IOException {
        try (var fos = java.nio.file.Files.newOutputStream(zipFile);
             var zos = new java.util.zip.ZipOutputStream(fos)) {
            java.nio.file.Files.walkFileTree(source, new java.nio.file.SimpleFileVisitor<>() {
                @Override
                public java.nio.file.FileVisitResult visitFile(java.nio.file.Path file, java.nio.file.attribute.BasicFileAttributes attrs) throws IOException {
                    String name = source.relativize(file).toString().replace('\\', '/');
                    // skip lock + large session files
                    if (name.endsWith("session.lock")) return java.nio.file.FileVisitResult.CONTINUE;
                    try {
                        zos.putNextEntry(new java.util.zip.ZipEntry(name));
                        java.nio.file.Files.copy(file, zos);
                        zos.closeEntry();
                    } catch (IOException ignored) {}
                    return java.nio.file.FileVisitResult.CONTINUE;
                }
            });
        }
    }

    // ===================== History endpoints =====================

    private void handleHistoryChat(HttpExchange ex) throws IOException {
        var q = parseQuery(ex);
        String uuid = q.getOrDefault("uuid", "");
        long since = parseLong(q.get("since"), 0L);
        int limit = (int) Math.min(2000, parseLong(q.get("limit"), 200L));
        JsonObject o = new JsonObject();
        o.add("entries", AdminHistoryStore.queryChat(uuid, since, limit));
        writeJson(ex, 200, o);
    }

    private void handleHistoryCommands(HttpExchange ex) throws IOException {
        var q = parseQuery(ex);
        String uuid = q.getOrDefault("uuid", "");
        long since = parseLong(q.get("since"), 0L);
        int limit = (int) Math.min(2000, parseLong(q.get("limit"), 200L));
        JsonObject o = new JsonObject();
        o.add("entries", AdminHistoryStore.queryCommands(uuid, since, limit));
        writeJson(ex, 200, o);
    }

    private void handleHistorySiteCommands(HttpExchange ex) throws IOException {
        var q = parseQuery(ex);
        long since = parseLong(q.get("since"), 0L);
        int limit = (int) Math.min(2000, parseLong(q.get("limit"), 200L));
        JsonObject o = new JsonObject();
        o.add("entries", AdminHistoryStore.querySiteCommands(since, limit));
        writeJson(ex, 200, o);
    }

    private void handleSnapshotRunNow(HttpExchange ex) throws IOException {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) { writeJson(ex, 503, AdminApiSerializer.errorResponse("server not running")); return; }
        int n = AdminSnapshotScheduler.snapshotNow(server);
        JsonObject o = new JsonObject();
        o.addProperty("ok", true);
        o.addProperty("captured", n);
        writeJson(ex, 200, o);
    }

    private void handleSnapshotList(HttpExchange ex, String path) throws IOException {
        String uuid = path.substring("/api/snapshot/list/".length());
        JsonObject o = new JsonObject();
        o.add("snapshots", AdminSnapshotScheduler.listSnapshots(uuid));
        writeJson(ex, 200, o);
    }

    private void handleSnapshotGet(HttpExchange ex, String path) throws IOException {
        // /api/snapshot/get/{uuid}/{ts}
        String rest = path.substring("/api/snapshot/get/".length());
        String[] parts = rest.split("/");
        if (parts.length != 2) { writeJson(ex, 400, AdminApiSerializer.errorResponse("expected uuid/ts")); return; }
        long ts;
        try { ts = Long.parseLong(parts[1]); }
        catch (NumberFormatException e) { writeJson(ex, 400, AdminApiSerializer.errorResponse("invalid ts")); return; }
        JsonObject snap = AdminSnapshotScheduler.readSnapshot(parts[0], ts);
        if (snap == null) writeJson(ex, 404, AdminApiSerializer.errorResponse("snapshot not found"));
        else writeJson(ex, 200, snap);
    }

    // ===================== Map endpoints =====================

    private void handleMapChunks(HttpExchange ex) throws IOException {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) { writeJson(ex, 503, AdminApiSerializer.errorResponse("server not running")); return; }
        var q = parseQuery(ex);
        String dim = q.getOrDefault("dim", "");
        JsonObject o = runOnMain(server, () -> AdminMapRenderer.loadedChunks(server, dim));
        writeJson(ex, 200, o == null ? AdminApiSerializer.errorResponse("timeout") : o);
    }

    private void handleMapChunk(HttpExchange ex, String path) throws IOException {
        // /api/map/chunk/{cx}/{cz}?dim=
        String rest = path.substring("/api/map/chunk/".length());
        String[] parts = rest.split("/");
        if (parts.length != 2) { writeJson(ex, 400, AdminApiSerializer.errorResponse("expected cx/cz")); return; }
        int cx, cz;
        try { cx = Integer.parseInt(parts[0]); cz = Integer.parseInt(parts[1]); }
        catch (NumberFormatException e) { writeJson(ex, 400, AdminApiSerializer.errorResponse("invalid cx/cz")); return; }
        var q = parseQuery(ex);
        String dim = q.getOrDefault("dim", "");
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) { writeJson(ex, 503, AdminApiSerializer.errorResponse("server not running")); return; }
        byte[] png = runOnMain(server, () -> AdminMapRenderer.renderChunk(server, dim, cx, cz));
        if (png == null) {
            writeJson(ex, 404, AdminApiSerializer.errorResponse("chunk not loaded"));
            return;
        }
        ex.getResponseHeaders().set("Content-Type", "image/png");
        ex.getResponseHeaders().set("Cache-Control", "max-age=10");
        ex.sendResponseHeaders(200, png.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(png); }
    }

    // ===================== Helpers =====================

    private java.util.Map<String, String> parseQuery(HttpExchange ex) {
        java.util.Map<String, String> map = new java.util.HashMap<>();
        String q = ex.getRequestURI().getQuery();
        if (q == null) return map;
        for (String part : q.split("&")) {
            int eq = part.indexOf('=');
            if (eq > 0) map.put(part.substring(0, eq), java.net.URLDecoder.decode(part.substring(eq + 1), StandardCharsets.UTF_8));
            else map.put(part, "");
        }
        return map;
    }

    private long parseLong(String s, long def) {
        if (s == null || s.isEmpty()) return def;
        try { return Long.parseLong(s); } catch (Exception e) { return def; }
    }


    /**
     * v0.1.22 r15: agora detecta server-not-ready (boot em andamento) e
     * retorna null SEM logar como ERROR. User reportou logs poluídos com
     * TimeoutException de 5s quando o backend pinga /api/command durante
     * o boot de servidor pesado (200 mods + UnityTranslate tentando baixar
     * AI offline + Ketting). Causa: main thread ocupado, server.execute()
     * não consegue rodar em 5s.
     *
     * <p>Mudanças:
     * <ul>
     *   <li>Timeout aumentado de 5s pra 30s — tolera bursts de lag no boot</li>
     *   <li>Pre-check: se server.isRunning()=false ou tickCount=0, retorna
     *       null IMEDIATAMENTE (sem enfileirar) e loga INFO em vez de ERROR</li>
     *   <li>Timeout logado como WARN com info de diagnóstico (não ERROR)</li>
     * </ul>
     */
    private <T> T runOnMain(MinecraftServer server, java.util.function.Supplier<T> action) {
        // Pre-check: server ainda não terminou de bootar?
        if (!server.isRunning() || server.getTickCount() < 20) {
            LiberthiaMod.LOGGER.info(
                    "[AdminAPI] server ainda em boot (tickCount={}, isRunning={}) — request ignorado",
                    server.getTickCount(), server.isRunning());
            return null;
        }
        CompletableFuture<T> future = new CompletableFuture<>();
        server.execute(() -> {
            try { future.complete(action.get()); }
            catch (Exception e) { future.completeExceptionally(e); }
        });
        try {
            return future.get(EXEC_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            // Main thread sobrecarregado — provável servidor lagando com
            // muitos mods. Log WARN com info, sem stack trace.
            LiberthiaMod.LOGGER.warn(
                    "[AdminAPI] main thread timeout {} ms — server provavelmente lagando (tickCount={})",
                    EXEC_TIMEOUT_MS, server.getTickCount());
            return null;
        } catch (InterruptedException | ExecutionException e) {
            LiberthiaMod.LOGGER.error("[AdminAPI] runOnMain failed", e);
            return null;
        }
    }

    private JsonObject readJson(HttpExchange ex) throws IOException {
        try (var is = ex.getRequestBody()) {
            byte[] bytes = is.readAllBytes();
            if (bytes.length == 0) return null;
            String s = new String(bytes, StandardCharsets.UTF_8);
            return JsonParser.parseString(s).getAsJsonObject();
        } catch (Exception e) {
            return null;
        }
    }

    private void writeJson(HttpExchange ex, int status, com.google.gson.JsonElement json) throws IOException {
        byte[] body = AdminApiSerializer.GSON.toJson(json).getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json");
        ex.sendResponseHeaders(status, body.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(body);
        }
    }

    // =========================================================================
    // TELEMETRY — sistema nervoso do servidor
    // =========================================================================
    //
    // Rotas (GET, somente leitura):
    //   /api/telemetry/status           → resumo geral (sessões, storage queue)
    //   /api/telemetry/players          → lista de todos players com features/inference
    //   /api/telemetry/player/{uuid}    → dump completo de 1 player (timeline + features + inference)
    //
    // Por que separado dos outros handlers: telemetria é read-only, não toca
    // no mundo MC, então não precisa do server.execute() dance.
    private void handleTelemetry(HttpExchange ex, String path, String method) throws IOException {
        if (!"GET".equals(method)) {
            writeJson(ex, 405, AdminApiSerializer.errorResponse("method not allowed"));
            return;
        }
        var mgr = br.com.murilo.liberthia.telemetry.TelemetryManager.get();
        if (mgr == null) {
            writeJson(ex, 503, AdminApiSerializer.errorResponse("telemetry not active (server starting?)"));
            return;
        }

        if (path.equals("/api/telemetry/status")) {
            JsonObject out = new JsonObject();
            out.addProperty("active", true);
            out.addProperty("sessions", mgr.allSessions().size());
            out.addProperty("storageQueueSize", mgr.getStorage().queueSize());
            out.addProperty("storageDropped", mgr.getStorage().droppedEvents());
            out.addProperty("ts", System.currentTimeMillis());
            writeJson(ex, 200, out);
            return;
        }

        if (path.equals("/api/telemetry/players")) {
            JsonArray arr = new JsonArray();
            for (var s : mgr.allSessions()) {
                arr.add(sessionSummary(s));
            }
            JsonObject out = new JsonObject();
            out.add("players", arr);
            out.addProperty("count", arr.size());
            writeJson(ex, 200, out);
            return;
        }

        if (path.startsWith("/api/telemetry/player/")) {
            String uuidStr = path.substring("/api/telemetry/player/".length());
            UUID uuid;
            try { uuid = UUID.fromString(uuidStr); }
            catch (Exception e) {
                writeJson(ex, 400, AdminApiSerializer.errorResponse("invalid uuid"));
                return;
            }
            var session = mgr.getSession(uuid);
            if (session == null) {
                writeJson(ex, 404, AdminApiSerializer.errorResponse("no active session for that player"));
                return;
            }
            writeJson(ex, 200, sessionFullDetail(session));
            return;
        }

        writeJson(ex, 404, AdminApiSerializer.errorResponse("telemetry endpoint not found"));
    }

    private JsonObject sessionSummary(br.com.murilo.liberthia.telemetry.session.PlayerSession s) {
        JsonObject o = new JsonObject();
        o.addProperty("uuid", s.getUuid().toString());
        o.addProperty("startedAt", s.getStartedAt());
        o.addProperty("durationMs", s.sessionDurationMs());
        o.addProperty("idleMs", s.idleTimeMs());
        o.addProperty("dimension", s.getLastDim());
        o.addProperty("x", s.getLastX());
        o.addProperty("y", s.getLastY());
        o.addProperty("z", s.getLastZ());
        o.addProperty("deaths", s.getDeathCount());
        o.addProperty("kills", s.getKillCount());
        o.addProperty("blocksBroken", s.getBlocksBroken());
        o.addProperty("blocksPlaced", s.getBlocksPlaced());
        o.addProperty("totalDistance", s.getTotalDistanceXZ());
        o.addProperty("timelineSize", s.getTimeline().size());
        // Features + inference (snapshots cached pelo worker)
        var f = s.getFeatures();
        if (f != null) {
            o.add("features", AdminApiSerializer.GSON.toJsonTree(f.toMap()));
        }
        var i = s.getInference();
        if (i != null) {
            o.add("inference", AdminApiSerializer.GSON.toJsonTree(i.toMap()));
        }
        return o;
    }

    private JsonObject sessionFullDetail(br.com.murilo.liberthia.telemetry.session.PlayerSession s) {
        JsonObject o = sessionSummary(s);
        // Adiciona últimos 100 eventos da timeline pra inspeção
        JsonArray events = new JsonArray();
        var recent = s.getTimeline().last(100);
        for (var e : recent) {
            events.add(AdminApiSerializer.GSON.toJsonTree(e.toMap()));
        }
        o.add("recentEvents", events);
        return o;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // r161: Spell / Magic / Observer-Clone API
    // ═══════════════════════════════════════════════════════════════════════

    /** GET /api/spells/list — retorna todos feitiços do SpellLibrary. */
    private void handleSpellsList(HttpExchange ex) throws IOException {
        JsonObject root = new JsonObject();
        JsonArray arr = new JsonArray();
        for (br.com.murilo.liberthia.magic.spell.SpellDef def :
                br.com.murilo.liberthia.magic.spell.SpellLibrary.all()) {
            JsonObject s = new JsonObject();
            s.addProperty("id", def.id);
            s.addProperty("name", def.name);
            s.addProperty("school", def.school.name());
            s.addProperty("rarity", def.rarity.name());
            s.addProperty("manaCost", def.manaCost);
            s.addProperty("cooldownTicks", def.cooldownTicks);
            s.addProperty("damage", def.damage);
            s.addProperty("range", def.range);
            s.addProperty("lore", def.lore);
            s.addProperty("schoolColor", "#" + Integer.toHexString(def.school.colorHex() & 0xFFFFFF));
            arr.add(s);
        }
        root.add("spells", arr);
        root.addProperty("total", arr.size());
        writeJson(ex, 200, root);
    }

    /** POST /api/spells/give — dá scroll do feitiço. Body: {playerUuid, spellId}. */
    private void handleSpellGive(HttpExchange ex) throws IOException {
        JsonObject body = readJson(ex);
        if (body == null || !body.has("playerUuid") || !body.has("spellId")) {
            writeJson(ex, 400, AdminApiSerializer.errorResponse("need playerUuid + spellId"));
            return;
        }
        String uuidStr = body.get("playerUuid").getAsString();
        String spellId = body.get("spellId").getAsString();
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) { writeJson(ex, 503, AdminApiSerializer.errorResponse("server not running")); return; }
        String result = runOnMain(server, () -> {
            java.util.UUID uuid;
            try { uuid = java.util.UUID.fromString(uuidStr); }
            catch (Exception e) { return "bad_uuid"; }
            net.minecraft.server.level.ServerPlayer p = server.getPlayerList().getPlayer(uuid);
            if (p == null) return "player_offline";
            net.minecraft.world.item.ItemStack stack =
                    br.com.murilo.liberthia.magic.factory.DynamicSpellItem.stackFor(
                            br.com.murilo.liberthia.registry.ModItems.FACTORY_SPELL_SCROLL.get(),
                            spellId);
            if (!p.getInventory().add(stack)) p.drop(stack, false);
            return "ok";
        });
        JsonObject r = new JsonObject();
        r.addProperty("ok", "ok".equals(result));
        r.addProperty("status", result == null ? "null" : result);
        writeJson(ex, "ok".equals(result) ? 200 : 400, r);
    }

    /**
     * POST /api/spells/cast — força casting de spell em um target.
     * Body: {playerUuid, spellId}. O scroll é dado e forçado a executar.
     */
    private void handleSpellCast(HttpExchange ex) throws IOException {
        JsonObject body = readJson(ex);
        if (body == null || !body.has("playerUuid") || !body.has("spellId")) {
            writeJson(ex, 400, AdminApiSerializer.errorResponse("need playerUuid + spellId"));
            return;
        }
        String uuidStr = body.get("playerUuid").getAsString();
        String spellId = body.get("spellId").getAsString();
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) { writeJson(ex, 503, AdminApiSerializer.errorResponse("server not running")); return; }
        String result = runOnMain(server, () -> {
            java.util.UUID uuid;
            try { uuid = java.util.UUID.fromString(uuidStr); }
            catch (Exception e) { return "bad_uuid"; }
            net.minecraft.server.level.ServerPlayer p = server.getPlayerList().getPlayer(uuid);
            if (p == null) return "player_offline";
            br.com.murilo.liberthia.magic.spell.SpellDef def =
                    br.com.murilo.liberthia.magic.spell.SpellLibrary.get(spellId);
            if (def == null) return "spell_not_found";
            try {
                // Cast spell sem custo de mana (admin force)
                br.com.murilo.liberthia.magic.spell.CastContext ctx =
                        new br.com.murilo.liberthia.magic.spell.CastContext(p, p.serverLevel(),
                                p.getUsedItemHand(),
                                new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.AIR),
                                def);
                def.cast.execute(ctx);
                return "ok";
            } catch (Throwable t) {
                return "cast_error: " + t.getMessage();
            }
        });
        JsonObject r = new JsonObject();
        r.addProperty("ok", "ok".equals(result));
        r.addProperty("status", result == null ? "null" : result);
        writeJson(ex, "ok".equals(result) ? 200 : 400, r);
    }

    /**
     * POST /api/spells/create — recebe um spell JSON completo e salva.
     * Body: o conteúdo do JSON do spell (id, name, school, vfx, effects, ...).
     */
    private void handleSpellCreate(HttpExchange ex) throws IOException {
        JsonObject body = readJson(ex);
        if (body == null || !body.has("id")) {
            writeJson(ex, 400, AdminApiSerializer.errorResponse("need id"));
            return;
        }
        String spellId = body.get("id").getAsString();
        if (!spellId.startsWith("factory_")) spellId = "factory_" + spellId;
        try {
            // r161: reusa pipeline do mod — SpellRecipe.fromJson
            br.com.murilo.liberthia.magic.factory.SpellRecipe recipe =
                    br.com.murilo.liberthia.magic.factory.SpellRecipe.fromJson(body);
            br.com.murilo.liberthia.magic.factory.SpellRecipeRegistry.register(recipe);
            JsonObject r = new JsonObject();
            r.addProperty("ok", true);
            r.addProperty("id", spellId);
            r.addProperty("note", "Spell registered at runtime. To persist, save JSON to data/liberthia/spells/.");
            writeJson(ex, 200, r);
        } catch (Throwable t) {
            writeJson(ex, 500, AdminApiSerializer.errorResponse("parse error: " + t.getMessage()));
        }
    }

    /**
     * GET /api/magic/stats/{uuid} — retorna level + sanity + max source.
     * POST /api/magic/stats/{uuid} — body {level?, source?, sanity?}
     */
    private void handleMagicStats(HttpExchange ex, String path, String method) throws IOException {
        String[] parts = path.split("/");
        if (parts.length < 5) { writeJson(ex, 400, AdminApiSerializer.errorResponse("bad path")); return; }
        String uuidStr = parts[4];
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) { writeJson(ex, 503, AdminApiSerializer.errorResponse("server not running")); return; }

        if ("GET".equals(method)) {
            JsonObject result = runOnMain(server, () -> {
                java.util.UUID uuid;
                try { uuid = java.util.UUID.fromString(uuidStr); } catch (Exception e) { return null; }
                net.minecraft.server.level.ServerPlayer p = server.getPlayerList().getPlayer(uuid);
                if (p == null) return null;
                JsonObject o = new JsonObject();
                o.addProperty("level", br.com.murilo.liberthia.observation.source.MagicLevelData.getLevel(p));
                o.addProperty("kills", br.com.murilo.liberthia.observation.source.MagicLevelData.getKillsThisLevel(p));
                o.addProperty("progress", br.com.murilo.liberthia.observation.source.MagicLevelData.getProgress(p));
                o.addProperty("source", br.com.murilo.liberthia.observation.source.SourceData.get(p));
                o.addProperty("sourceMax", br.com.murilo.liberthia.observation.source.SourceData.getMax(p));
                return o;
            });
            if (result == null) {
                writeJson(ex, 404, AdminApiSerializer.errorResponse("player not online"));
            } else {
                writeJson(ex, 200, result);
            }
        } else if ("POST".equals(method)) {
            JsonObject body = readJson(ex);
            if (body == null) { writeJson(ex, 400, AdminApiSerializer.errorResponse("missing body")); return; }
            String result = runOnMain(server, () -> {
                java.util.UUID uuid;
                try { uuid = java.util.UUID.fromString(uuidStr); } catch (Exception e) { return "bad_uuid"; }
                net.minecraft.server.level.ServerPlayer p = server.getPlayerList().getPlayer(uuid);
                if (p == null) return "player_offline";
                if (body.has("level")) {
                    br.com.murilo.liberthia.observation.source.MagicLevelData.setLevel(p, body.get("level").getAsInt());
                }
                if (body.has("source")) {
                    br.com.murilo.liberthia.observation.source.SourceData.set(p, body.get("source").getAsInt());
                }
                return "ok";
            });
            JsonObject r = new JsonObject();
            r.addProperty("ok", "ok".equals(result));
            r.addProperty("status", result == null ? "null" : result);
            writeJson(ex, "ok".equals(result) ? 200 : 400, r);
        } else {
            writeJson(ex, 405, AdminApiSerializer.errorResponse("method not allowed"));
        }
    }

    /**
     * POST /api/world/spawn-observer-clone — spawna um ClonePlayerEntity com
     * observerMode = true. Body: {x, y, z, dimension?, playerName?, playerUuid?}.
     */
    private void handleSpawnObserverClone(HttpExchange ex) throws IOException {
        JsonObject body = readJson(ex);
        if (body == null) { writeJson(ex, 400, AdminApiSerializer.errorResponse("missing body")); return; }
        double x = body.get("x").getAsDouble();
        double y = body.get("y").getAsDouble();
        double z = body.get("z").getAsDouble();
        String dim = body.has("dimension") ? body.get("dimension").getAsString() : null;
        String pName = body.has("playerName") ? body.get("playerName").getAsString() : null;
        String pUuidStr = body.has("playerUuid") ? body.get("playerUuid").getAsString() : null;

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) { writeJson(ex, 503, AdminApiSerializer.errorResponse("server not running")); return; }

        String result = runOnMain(server, () -> {
            net.minecraft.server.level.ServerLevel level = server.overworld();
            if (dim != null && !dim.isBlank()) {
                String full = dim.contains(":") ? dim : ("minecraft:" + dim);
                net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dk =
                        net.minecraft.resources.ResourceKey.create(
                                net.minecraft.core.registries.Registries.DIMENSION,
                                net.minecraft.resources.ResourceLocation.tryParse(full));
                net.minecraft.server.level.ServerLevel tl = server.getLevel(dk);
                if (tl != null) level = tl;
            }
            java.util.UUID ownerUuid = null;
            String ownerName = pName;
            if (pUuidStr != null && !pUuidStr.isBlank()) {
                try { ownerUuid = java.util.UUID.fromString(pUuidStr); } catch (Exception ignored) {}
            }
            if (ownerUuid == null) {
                String fallback = ownerName != null ? ownerName : "Steve";
                ownerUuid = java.util.UUID.nameUUIDFromBytes(("OfflinePlayer:" + fallback).getBytes(java.nio.charset.StandardCharsets.UTF_8));
                ownerName = fallback;
            }
            br.com.murilo.liberthia.entity.ClonePlayerEntity clone =
                    br.com.murilo.liberthia.registry.ModEntities.CLONE_PLAYER.get().create(level);
            if (clone == null) return null;
            clone.moveTo(x, y, z, 0f, 0f);
            clone.setOwnerUuid(ownerUuid);
            clone.setOwnerName(ownerName);
            clone.setObserverMode(true);  // <-- OBSERVER MODE!
            clone.setCustomName(net.minecraft.network.chat.Component.literal("§4§lObserver"));
            clone.setCustomNameVisible(false);
            clone.addTag("liberthia_observer");
            level.addFreshEntity(clone);
            return clone.getUUID().toString();
        });
        if (result == null) {
            writeJson(ex, 500, AdminApiSerializer.errorResponse("failed to create observer"));
            return;
        }
        JsonObject ok = new JsonObject();
        ok.addProperty("ok", true);
        ok.addProperty("uuid", result);
        ok.addProperty("mode", "observer");
        writeJson(ex, 200, ok);
    }
}
