package br.com.murilo.liberthia.cosmic.living;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * v0.1.22 r47: SavedData PER-WORLD que armazena memórias de cada chunk.
 *
 * <p>Persistido em {@code <world>/data/liberthia_chunk_memory.dat}.
 *
 * <p>Lookup por {@link ChunkPos}.long() pra ser thread-safe.
 */
public class ChunkMemoryStorage extends SavedData {

    public static final String STORAGE_KEY = "liberthia_chunk_memory";
    /** Limite — não guarda memória de chunks vazios (otimização). */
    public static final int MIN_EMOTIONAL_TO_KEEP = 5;

    private final Map<Long, ChunkMemory> chunks = new ConcurrentHashMap<>();

    public ChunkMemoryStorage() {}

    public static ChunkMemoryStorage get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                ChunkMemoryStorage::load,
                ChunkMemoryStorage::new,
                STORAGE_KEY);
    }

    public static ChunkMemoryStorage load(CompoundTag tag) {
        ChunkMemoryStorage s = new ChunkMemoryStorage();
        if (tag.contains("Memories")) {
            ListTag list = tag.getList("Memories", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                try {
                    ChunkMemory cm = ChunkMemory.fromNbt(list.getCompound(i));
                    s.chunks.put(cm.pos.toLong(), cm);
                } catch (Throwable ignored) {}
            }
        }
        return s;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (ChunkMemory cm : chunks.values()) {
            // Não salva chunks vazios (otimização)
            if (cm.emotionalIndex() < MIN_EMOTIONAL_TO_KEEP && cm.echoes.isEmpty()) continue;
            list.add(cm.toNbt());
        }
        tag.put("Memories", list);
        return tag;
    }

    public ChunkMemory get(ChunkPos pos) {
        return chunks.computeIfAbsent(pos.toLong(), k -> new ChunkMemory(pos));
    }

    public ChunkMemory peek(ChunkPos pos) {
        return chunks.get(pos.toLong());
    }

    public boolean has(ChunkPos pos) {
        return chunks.containsKey(pos.toLong());
    }

    public int totalChunks() {
        return chunks.size();
    }

    /** Limpa memória de um chunk (Curator command). */
    public void clearChunk(ChunkPos pos) {
        if (chunks.remove(pos.toLong()) != null) setDirty();
    }

    /** Snapshot completo de todos chunks (read-only). */
    public java.util.Collection<ChunkMemory> all() {
        return chunks.values();
    }

    @Override
    public boolean isDirty() {
        return true; // sempre salva (memória cresce dinamicamente)
    }
}
