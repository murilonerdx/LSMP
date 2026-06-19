package br.com.murilo.liberthia.logic.entropy;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * r183 — <b>EntropyTracker</b>: mapeia TUDO que os motores (Entropy/Black Matter)
 * trocam, por <b>id de motor</b>, num JSON na pasta do mundo
 * ({@code <world>/liberthia/entropy/<id>.json}), com o bloco ORIGINAL de cada posição.
 * Permite reverter 100% quando o motor é destruído, e — se o servidor cair e bugar —
 * reverter via comando ({@code /liberthia entropy revert <id|all>}) lendo os JSONs.
 *
 * <p>Formato: <code>{"dim":"minecraft:overworld","blocks":{"x_y_z":"minecraft:stone",...}}</code>
 * Guarda o id do bloco (tipo/nome) — restaura como defaultBlockState (mesmo critério do
 * MatterHistoryManager). Cache em memória + flush periódico = seguro contra crash.
 */
public final class EntropyTracker {
    private EntropyTracker() {}

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    // id -> (posLong -> blockId original)
    private static final Map<String, Map<Long, String>> CACHE = new ConcurrentHashMap<>();
    private static final Map<String, String> DIM = new ConcurrentHashMap<>();
    private static final Set<String> DIRTY = ConcurrentHashMap.newKeySet();
    private static final Map<String, java.util.concurrent.CompletableFuture<?>> WRITES = new ConcurrentHashMap<>();
    private static final java.util.Queue<Pending> PENDING_REVERT = new java.util.concurrent.ConcurrentLinkedQueue<>();
    private static final int REVERT_PER_TICK = 4096;
    private record Pending(ServerLevel level, BlockPos pos, BlockState target) {}

    private static Path dir(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT).resolve("liberthia").resolve("entropy");
    }
    private static Path file(MinecraftServer server, String id) {
        return dir(server).resolve(id + ".json");
    }
    private static String posKey(long l) { BlockPos p = BlockPos.of(l); return p.getX() + "_" + p.getY() + "_" + p.getZ(); }
    private static long parsePos(String k) {
        String[] s = k.split("_");
        return new BlockPos(Integer.parseInt(s[0]), Integer.parseInt(s[1]), Integer.parseInt(s[2])).asLong();
    }

    /** Grava o bloco ORIGINAL antes do motor trocá-lo (só a 1ª vez por posição). */
    public static void record(ServerLevel level, String id, BlockPos pos, BlockState original) {
        CACHE.computeIfAbsent(id, k -> loadIntoCache(level.getServer(), k))
                .putIfAbsent(pos.asLong(), BuiltInRegistries.BLOCK.getKey(original.getBlock()).toString());
        DIM.putIfAbsent(id, level.dimension().location().toString());
        DIRTY.add(id);
    }

    private static Map<Long, String> loadIntoCache(MinecraftServer server, String id) {
        Map<Long, String> map = new HashMap<>();
        Path f = file(server, id);
        if (Files.exists(f)) {
            try {
                JsonObject o = JsonParser.parseString(Files.readString(f)).getAsJsonObject();
                if (o.has("dim")) DIM.putIfAbsent(id, o.get("dim").getAsString());
                JsonObject blocks = o.getAsJsonObject("blocks");
                for (String k : blocks.keySet()) map.put(parsePos(k), blocks.get(k).getAsString());
            } catch (Exception ignored) {}
        }
        return map;
    }

    /** Persiste no disco os ids sujos (chamado periodicamente + ao parar/quebrar). */
    public static void flush(MinecraftServer server) {
        if (DIRTY.isEmpty()) return;
        for (String id : new ArrayList<>(DIRTY)) {
            Map<Long, String> map = CACHE.get(id);
            if (map == null || map.isEmpty()) { DIRTY.remove(id); continue; }
            // serializa no tick (rápido) a partir de um snapshot; ESCREVE no disco em background (não trava o servidor)
            JsonObject o = new JsonObject();
            o.addProperty("dim", DIM.getOrDefault(id, Level.OVERWORLD.location().toString()));
            JsonObject blocks = new JsonObject();
            new HashMap<>(map).forEach((k, v) -> blocks.addProperty(posKey(k), v));
            o.add("blocks", blocks);
            final String json = GSON.toJson(o);
            final Path target = file(server, id);
            final String fid = id;
            // escreve em background; só limpa DIRTY APÓS a escrita (crash-safe); rastreia a Future p/ ordenar o delete
            WRITES.put(id, java.util.concurrent.CompletableFuture.runAsync(() -> {
                try { Files.createDirectories(target.getParent()); Files.writeString(target, json); }
                catch (IOException e) { br.com.murilo.liberthia.LiberthiaMod.LOGGER.error("[Entropy] flush falhou: {}", e.toString()); }
                finally { DIRTY.remove(fid); }
            }, net.minecraft.Util.ioPool()));
        }
    }

    /** Reverte TUDO de um motor: restaura blocos originais, apaga cache + JSON. Retorna nº revertido. */
    public static int revert(MinecraftServer server, String id) {
        Map<Long, String> map = CACHE.computeIfAbsent(id, k -> loadIntoCache(server, k));
        if (map.isEmpty()) { cleanup(server, id); return 0; }
        String dimStr = DIM.getOrDefault(id, Level.OVERWORLD.location().toString());
        ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION, new ResourceLocation(dimStr)));
        int n = 0;
        if (level != null) {
            // enfileira p/ aplicar em lotes por tick (evita freeze ao reverter milhares de blocos / force-load de chunks)
            for (Map.Entry<Long, String> e : map.entrySet()) {
                Block b = BuiltInRegistries.BLOCK.get(new ResourceLocation(e.getValue()));
                BlockState target = (b != null ? b.defaultBlockState() : Blocks.AIR.defaultBlockState());
                PENDING_REVERT.add(new Pending(level, BlockPos.of(e.getKey()), target));
                n++;
            }
        }
        cleanup(server, id);
        return n;
    }

    /** Aplica o revert em lotes (chamado todo tick por {@code event/EntropyTickHandler}). */
    public static void drainReverts() {
        int n = 0; Pending p;
        while (n < REVERT_PER_TICK && (p = PENDING_REVERT.poll()) != null) {
            try { p.level().setBlock(p.pos(), p.target(), Block.UPDATE_ALL); } catch (Exception ignored) {}
            n++;
        }
    }

    private static void cleanup(MinecraftServer server, String id) {
        CACHE.remove(id); DIM.remove(id); DIRTY.remove(id);
        Path f = file(server, id);
        Runnable del = () -> { try { Files.deleteIfExists(f); } catch (IOException ignored) {} };
        // só apaga o JSON DEPOIS que a escrita pendente terminar (evita órfão/AccessDenied)
        java.util.concurrent.CompletableFuture<?> w = WRITES.remove(id);
        if (w != null) w.whenComplete((a, b) -> del.run()); else net.minecraft.Util.ioPool().execute(del);
    }

    /** Reverte TODOS os motores (recuperação de crash). Retorna nº de motores revertidos. */
    public static int revertAll(MinecraftServer server) {
        int engines = 0;
        for (String id : listIds(server)) { revert(server, id); engines++; }
        return engines;
    }

    public static Set<String> listIds(MinecraftServer server) {
        Set<String> ids = new HashSet<>(CACHE.keySet());
        Path d = dir(server);
        if (Files.isDirectory(d)) {
            try (var st = Files.list(d)) {
                st.filter(f -> f.toString().endsWith(".json")).forEach(f -> {
                    String name = f.getFileName().toString();
                    ids.add(name.substring(0, name.length() - 5));
                });
            } catch (IOException ignored) {}
        }
        return ids;
    }

    public static int count(MinecraftServer server, String id) {
        return CACHE.computeIfAbsent(id, k -> loadIntoCache(server, k)).size();
    }
}
