package br.com.murilo.liberthia.logic;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import br.com.murilo.liberthia.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class MatterHistoryManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type MAP_TYPE = new TypeToken<Map<String, String>>() {}.getType();

    private static final Map<String, Map<String, String>> REPLACEMENT_CACHE = new HashMap<>();
    private static final Map<String, Set<BlockPos>> PROTECTION_CACHE = new HashMap<>();

    private MatterHistoryManager() {
    }

    public static void recordOriginalBlock(ServerLevel level, BlockPos pos, BlockState original) {
        if (original.isAir() || original.is(ModBlocks.DARK_MATTER_BLOCK.get())) {
            return;
        }

        String key = levelKey(level);
        Map<String, String> replacements = loadReplacementMap(level);
        String posKey = posKey(pos);
        replacements.putIfAbsent(posKey, BuiltInRegistries.BLOCK.getKey(original.getBlock()).toString());
        REPLACEMENT_CACHE.put(key, replacements);
        saveReplacementMap(level, replacements);
    }

    public static int restoreMappedBlocks(ServerLevel level, BlockPos center, int radius) {
        Map<String, String> replacements = loadReplacementMap(level);
        if (replacements.isEmpty()) {
            return 0;
        }

        boolean changed = false;
        int restored = 0;
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -2, -radius), center.offset(radius, 2, radius))) {
            String id = replacements.get(posKey(pos));
            if (id == null) {
                continue;
            }

            Block block = BuiltInRegistries.BLOCK.get(new ResourceLocation(id));
            if (block == null || block == net.minecraft.world.level.block.Blocks.AIR) {
                replacements.remove(posKey(pos));
                changed = true;
                continue;
            }

            level.setBlockAndUpdate(pos, block.defaultBlockState());
            replacements.remove(posKey(pos));
            changed = true;
            restored++;
        }

        if (changed) {
            saveReplacementMap(level, replacements);
        }
        return restored;
    }

    public static void registerProtectionBlock(ServerLevel level, BlockPos pos, BlockState state) {
        if (!isProtection(state)) {
            return;
        }
        String key = levelKey(level);
        PROTECTION_CACHE.computeIfAbsent(key, ignored -> new HashSet<>()).add(pos.immutable());
    }

    public static void unregisterProtectionBlock(ServerLevel level, BlockPos pos) {
        Set<BlockPos> set = PROTECTION_CACHE.get(levelKey(level));
        if (set != null) {
            set.remove(pos);
        }
    }

    public static boolean hasProtectionInChunkRange(ServerLevel level, BlockPos target, int chunkRange) {
        Set<BlockPos> set = PROTECTION_CACHE.get(levelKey(level));
        if (set == null || set.isEmpty()) {
            return false;
        }

        double rangeBlocks = chunkRange * 16.0D;
        double rangeSq = rangeBlocks * rangeBlocks;
        for (BlockPos pos : set) {
            if (pos.distSqr(target) <= rangeSq) {
                return true;
            }
        }
        return false;
    }

    private static boolean isProtection(BlockState state) {
        return state.is(ModBlocks.CLEAR_MATTER_BLOCK.get()) || state.is(ModBlocks.YELLOW_MATTER_BLOCK.get());
    }

    private static Map<String, String> loadReplacementMap(ServerLevel level) {
        String key = levelKey(level);
        if (REPLACEMENT_CACHE.containsKey(key)) {
            return REPLACEMENT_CACHE.get(key);
        }

        Path file = historyFile(level);
        Map<String, String> data = new HashMap<>();
        if (Files.exists(file)) {
            try {
                String json = Files.readString(file);
                Map<String, String> read = GSON.fromJson(json, MAP_TYPE);
                if (read != null) {
                    data.putAll(read);
                }
            } catch (IOException ignored) {
            }
        }

        REPLACEMENT_CACHE.put(key, data);
        return data;
    }

    private static void saveReplacementMap(ServerLevel level, Map<String, String> data) {
        Path file = historyFile(level);
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, GSON.toJson(data));
        } catch (IOException ignored) {
        }
    }

    private static Path historyFile(ServerLevel level) {
        return level.getServer().getWorldPath(LevelResource.ROOT)
                .resolve("data")
                .resolve("liberthia_block_history_" + level.dimension().location().getPath() + ".json");
    }

    private static String levelKey(ServerLevel level) {
        return level.dimension().location().toString();
    }

    private static String posKey(BlockPos pos) {
        return pos.getX() + "," + pos.getY() + "," + pos.getZ();
    }
}
