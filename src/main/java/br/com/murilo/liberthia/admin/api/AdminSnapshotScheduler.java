package br.com.murilo.liberthia.admin.api;

import br.com.murilo.liberthia.LiberthiaMod;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Snapshot horário de todos os players online. Salva em
 * world/serverconfig/liberthia_snapshots/{uuid}/{timestamp}.json
 *
 * Cada snapshot captura: inventory main+armor+offhand+ender, stats, posição,
 * matter profile e — recursivamente — items dentro de containers (shulker box,
 * bundles, bolsas com NBT "Items"/"Inventory"/"BlockEntityTag").
 */
public final class AdminSnapshotScheduler {

    private static volatile ScheduledExecutorService scheduler;
    private static volatile Path baseDir;

    private AdminSnapshotScheduler() {}

    public static synchronized void start(MinecraftServer server, Path worldRoot) {
        if (scheduler != null) return;
        try {
            baseDir = worldRoot.resolve("serverconfig").resolve("liberthia_snapshots");
            Files.createDirectories(baseDir);
        } catch (Exception e) {
            LiberthiaMod.LOGGER.warn("[Snapshot] mkdir failed: {}", e.getMessage());
            return;
        }
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "Liberthia-Snapshot");
            t.setDaemon(true);
            return t;
        });
        // Primeiro snapshot 30s após start; depois a cada hora.
        scheduler.scheduleAtFixedRate(() -> snapshotAll(server), 30, 3600, TimeUnit.SECONDS);
        LiberthiaMod.LOGGER.info("[Snapshot] scheduler iniciado (cada 1h)");
    }

    public static synchronized void stop() {
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
    }

    /** Dispara um snapshot manualmente (chamado pelo endpoint /api/snapshot/run-now). */
    public static int snapshotNow(MinecraftServer server) {
        return snapshotAll(server);
    }

    private static int snapshotAll(MinecraftServer server) {
        if (server == null || baseDir == null) return 0;
        // Tem que rodar leitura no main thread; usa execute+future
        java.util.concurrent.CompletableFuture<Integer> fut = new java.util.concurrent.CompletableFuture<>();
        server.execute(() -> {
            int n = 0;
            try {
                for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                    JsonObject snap = capture(p);
                    persist(p.getUUID(), snap);
                    n++;
                }
            } finally { fut.complete(n); }
        });
        try {
            int n = fut.get(10, TimeUnit.SECONDS);
            // Só loga quando capturou alguém — evita spam horário com servidor vazio
            if (n > 0) LiberthiaMod.LOGGER.info("[Snapshot] capturado {} players", n);
            return n;
        } catch (Exception e) {
            LiberthiaMod.LOGGER.warn("[Snapshot] timeout: {}", e.getMessage());
            return 0;
        }
    }

    private static JsonObject capture(ServerPlayer p) {
        JsonObject o = new JsonObject();
        long ts = System.currentTimeMillis();
        o.addProperty("ts", ts);
        o.addProperty("uuid", p.getUUID().toString());
        o.addProperty("name", p.getGameProfile().getName());
        o.addProperty("dimension", p.level().dimension().location().toString());
        JsonObject pos = new JsonObject();
        pos.addProperty("x", p.getX());
        pos.addProperty("y", p.getY());
        pos.addProperty("z", p.getZ());
        o.add("position", pos);
        o.addProperty("health", p.getHealth());
        o.addProperty("maxHealth", p.getMaxHealth());
        o.addProperty("food", p.getFoodData().getFoodLevel());
        o.addProperty("xpLevel", p.experienceLevel);
        o.addProperty("xpTotal", p.totalExperience);
        o.addProperty("gameMode", p.gameMode.getGameModeForPlayer().getName());

        // Inventory
        JsonArray main = new JsonArray();
        for (int i = 0; i < p.getInventory().items.size(); i++) {
            main.add(serializeWithBags(p.getInventory().items.get(i), i));
        }
        JsonArray armor = new JsonArray();
        for (int i = 0; i < p.getInventory().armor.size(); i++) {
            armor.add(serializeWithBags(p.getInventory().armor.get(i), i));
        }
        JsonObject offhand = serializeWithBags(p.getInventory().offhand.get(0), 0);
        o.add("main", main);
        o.add("armor", armor);
        o.add("offhand", offhand);

        // Ender chest
        JsonArray ender = new JsonArray();
        for (int i = 0; i < p.getEnderChestInventory().getContainerSize(); i++) {
            ender.add(serializeWithBags(p.getEnderChestInventory().getItem(i), i));
        }
        o.add("ender", ender);

        // Matter profile (se existir)
        try {
            p.getCapability(br.com.murilo.liberthia.matter.MatterProfileProvider.CAP).ifPresent(profile -> {
                JsonObject m = new JsonObject();
                m.addProperty("dm", profile.getDark());
                m.addProperty("wm", profile.getWhite());
                m.addProperty("ym", profile.getYellow());
                m.addProperty("type", profile.getActiveType().name());
                o.add("matter", m);
            });
        } catch (Exception ignored) {}

        return o;
    }

    /**
     * Serializa um ItemStack incluindo NBT customizado e RECURSIVAMENTE expande
     * containers conhecidos: shulker boxes (BlockEntityTag.Items), bundles (Items),
     * bolsas custom que usam "Inventory" ou "Items" no root tag.
     */
    private static JsonObject serializeWithBags(ItemStack stack, int slot) {
        JsonObject o = new JsonObject();
        o.addProperty("slot", slot);
        if (stack == null || stack.isEmpty()) {
            o.addProperty("empty", true);
            return o;
        }
        var id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        o.addProperty("id", id == null ? "?" : id.toString());
        o.addProperty("count", stack.getCount());
        o.addProperty("name", stack.getHoverName().getString());
        if (stack.hasTag()) {
            CompoundTag tag = stack.getTag();
            o.addProperty("nbt", tag.toString());
            // Tenta extrair containers aninhados
            JsonArray contents = extractNested(tag);
            if (contents.size() > 0) o.add("contents", contents);
        }
        if (stack.isEnchanted()) {
            JsonArray enchs = new JsonArray();
            net.minecraft.world.item.enchantment.EnchantmentHelper.getEnchantments(stack).forEach((e, lvl) -> {
                JsonObject ej = new JsonObject();
                var ek = ForgeRegistries.ENCHANTMENTS.getKey(e);
                ej.addProperty("id", ek == null ? "?" : ek.toString());
                ej.addProperty("level", lvl);
                enchs.add(ej);
            });
            o.add("enchantments", enchs);
        }
        return o;
    }

    /** Procura "Items" / "Inventory" / "BlockEntityTag.Items" e serializa cada item dentro. */
    private static JsonArray extractNested(CompoundTag tag) {
        JsonArray out = new JsonArray();
        addItemsList(out, tag, "Items");
        addItemsList(out, tag, "Inventory");
        if (tag.contains("BlockEntityTag", Tag.TAG_COMPOUND)) {
            CompoundTag bet = tag.getCompound("BlockEntityTag");
            addItemsList(out, bet, "Items");
        }
        return out;
    }

    private static void addItemsList(JsonArray out, CompoundTag tag, String key) {
        if (!tag.contains(key, Tag.TAG_LIST)) return;
        ListTag list = tag.getList(key, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag item = list.getCompound(i);
            try {
                ItemStack inner = ItemStack.of(item);
                int innerSlot = item.contains("Slot", Tag.TAG_BYTE) ? item.getByte("Slot") : i;
                out.add(serializeWithBags(inner, innerSlot));
            } catch (Exception ignored) {}
        }
    }

    private static void persist(UUID uuid, JsonObject snap) {
        if (baseDir == null) return;
        try {
            Path dir = baseDir.resolve(uuid.toString());
            Files.createDirectories(dir);
            long ts = snap.has("ts") ? snap.get("ts").getAsLong() : System.currentTimeMillis();
            Path file = dir.resolve(ts + ".json");
            Files.writeString(file, AdminApiSerializer.GSON.toJson(snap));
            // Cleanup: mantém só últimos 200 snapshots por player
            try (var stream = Files.list(dir)) {
                var files = stream.sorted().toList();
                if (files.size() > 200) {
                    for (int i = 0; i < files.size() - 200; i++) Files.deleteIfExists(files.get(i));
                }
            }
        } catch (Exception e) {
            LiberthiaMod.LOGGER.debug("[Snapshot] persist failed: {}", e.getMessage());
        }
    }

    /** Lista timestamps disponíveis pra um uuid. */
    public static JsonArray listSnapshots(String uuidStr) {
        JsonArray arr = new JsonArray();
        if (baseDir == null) return arr;
        try {
            Path dir = baseDir.resolve(uuidStr);
            if (!Files.isDirectory(dir)) return arr;
            try (var stream = Files.list(dir)) {
                stream.sorted().forEach(p -> {
                    String name = p.getFileName().toString();
                    if (name.endsWith(".json")) {
                        try { arr.add(Long.parseLong(name.substring(0, name.length() - 5))); }
                        catch (Exception ignored) {}
                    }
                });
            }
        } catch (Exception ignored) {}
        return arr;
    }

    public static JsonObject readSnapshot(String uuidStr, long ts) {
        if (baseDir == null) return null;
        try {
            Path file = baseDir.resolve(uuidStr).resolve(ts + ".json");
            if (!Files.exists(file)) return null;
            String s = Files.readString(file);
            return com.google.gson.JsonParser.parseString(s).getAsJsonObject();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Restaura player a partir de um snapshot. Roda em main thread.
     * Retorna JSON com {ok, restoredItems, message}.
     */
    public static JsonObject restoreFromSnapshot(ServerPlayer p, JsonObject snap,
                                                  boolean restoreInv, boolean restoreStats, boolean restorePos) {
        JsonObject result = new JsonObject();
        if (p == null) {
            result.addProperty("ok", false);
            result.addProperty("error", "player not online");
            return result;
        }
        int restored = 0;
        if (restoreInv) {
            // Limpa inventário antes
            p.getInventory().clearContent();
            // Main 36
            if (snap.has("main") && snap.get("main").isJsonArray()) {
                for (com.google.gson.JsonElement el : snap.getAsJsonArray("main")) {
                    JsonObject it = el.getAsJsonObject();
                    if (it.has("empty") && it.get("empty").getAsBoolean()) continue;
                    int slot = it.has("slot") ? it.get("slot").getAsInt() : -1;
                    ItemStack stack = deserializeItem(it);
                    if (stack == null || stack.isEmpty()) continue;
                    if (slot >= 0 && slot < p.getInventory().items.size()) {
                        p.getInventory().items.set(slot, stack);
                    } else {
                        p.getInventory().add(stack);
                    }
                    restored++;
                }
            }
            // Armor 4
            if (snap.has("armor") && snap.get("armor").isJsonArray()) {
                int idx = 0;
                for (com.google.gson.JsonElement el : snap.getAsJsonArray("armor")) {
                    JsonObject it = el.getAsJsonObject();
                    if (!(it.has("empty") && it.get("empty").getAsBoolean())) {
                        ItemStack stack = deserializeItem(it);
                        if (stack != null && !stack.isEmpty() && idx < p.getInventory().armor.size()) {
                            p.getInventory().armor.set(idx, stack);
                            restored++;
                        }
                    }
                    idx++;
                }
            }
            // Offhand
            if (snap.has("offhand") && snap.get("offhand").isJsonObject()) {
                JsonObject it = snap.getAsJsonObject("offhand");
                if (!(it.has("empty") && it.get("empty").getAsBoolean())) {
                    ItemStack stack = deserializeItem(it);
                    if (stack != null && !stack.isEmpty()) {
                        p.getInventory().offhand.set(0, stack);
                        restored++;
                    }
                }
            }
            // Ender chest
            if (snap.has("ender") && snap.get("ender").isJsonArray()) {
                p.getEnderChestInventory().clearContent();
                for (com.google.gson.JsonElement el : snap.getAsJsonArray("ender")) {
                    JsonObject it = el.getAsJsonObject();
                    if (it.has("empty") && it.get("empty").getAsBoolean()) continue;
                    int slot = it.has("slot") ? it.get("slot").getAsInt() : -1;
                    ItemStack stack = deserializeItem(it);
                    if (stack == null || stack.isEmpty()) continue;
                    if (slot >= 0 && slot < p.getEnderChestInventory().getContainerSize()) {
                        p.getEnderChestInventory().setItem(slot, stack);
                    }
                    restored++;
                }
            }
            p.getInventory().setChanged();
            p.containerMenu.broadcastChanges();
        }

        if (restoreStats) {
            if (snap.has("health")) p.setHealth(Math.min(p.getMaxHealth(), snap.get("health").getAsFloat()));
            if (snap.has("food")) p.getFoodData().setFoodLevel(snap.get("food").getAsInt());
            if (snap.has("xpLevel")) {
                p.experienceLevel = 0; p.totalExperience = 0; p.experienceProgress = 0;
                p.giveExperienceLevels(snap.get("xpLevel").getAsInt());
            }
        }

        if (restorePos && snap.has("position") && snap.get("position").isJsonObject()) {
            JsonObject pos = snap.getAsJsonObject("position");
            try {
                p.teleportTo(p.serverLevel(),
                        pos.get("x").getAsDouble(), pos.get("y").getAsDouble(), pos.get("z").getAsDouble(),
                        p.getYRot(), p.getXRot());
            } catch (Exception ignored) {}
        }

        result.addProperty("ok", true);
        result.addProperty("restoredItems", restored);
        result.addProperty("playerName", p.getGameProfile().getName());
        return result;
    }

    /**
     * Reconstrói um ItemStack a partir do JSON salvo no snapshot.
     * Usa {id, count, nbt(string)} pra restaurar com NBT completa
     * (preserva enchants, durability, contents de bags/shulkers).
     */
    private static ItemStack deserializeItem(JsonObject json) {
        if (!json.has("id")) return ItemStack.EMPTY;
        try {
            String id = json.get("id").getAsString();
            int count = json.has("count") ? json.get("count").getAsInt() : 1;
            var item = ForgeRegistries.ITEMS.getValue(net.minecraft.resources.ResourceLocation.tryParse(id));
            if (item == null) return ItemStack.EMPTY;
            ItemStack stack = new ItemStack(item, count);
            if (json.has("nbt")) {
                String nbtStr = json.get("nbt").getAsString();
                try {
                    var tag = net.minecraft.nbt.TagParser.parseTag(nbtStr);
                    stack.setTag(tag);
                } catch (Exception ignored) {}
            }
            return stack;
        } catch (Exception e) {
            return ItemStack.EMPTY;
        }
    }
}
