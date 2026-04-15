package br.com.murilo.liberthia.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Persiste o nível de contaminação por chunk (0-100).
 * Atualizado periodicamente pelo servidor, usado para escalar efeitos.
 */
public class ChunkInfectionData extends SavedData {

    private static final String DATA_NAME = "liberthia_chunk_infection";
    private final Map<Long, Integer> contamination = new ConcurrentHashMap<>();

    public ChunkInfectionData() {}

    public static ChunkInfectionData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                ChunkInfectionData::load,
                ChunkInfectionData::new,
                DATA_NAME
        );
    }

    public int getContamination(ChunkPos pos) {
        return contamination.getOrDefault(pos.toLong(), 0);
    }

    public float getDensity(ChunkPos pos) {
        return getContamination(pos) / 100.0f;
    }

    public void setContamination(ChunkPos pos, int value) {
        int clamped = Math.max(0, Math.min(100, value));
        if (clamped == 0) {
            contamination.remove(pos.toLong());
        } else {
            contamination.put(pos.toLong(), clamped);
        }
        setDirty();
    }

    public Map<Long, Integer> getAllData() {
        return contamination;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        CompoundTag chunks = new CompoundTag();
        for (Map.Entry<Long, Integer> entry : contamination.entrySet()) {
            chunks.putInt(String.valueOf(entry.getKey()), entry.getValue());
        }
        tag.put("chunks", chunks);
        return tag;
    }

    public static ChunkInfectionData load(CompoundTag tag) {
        ChunkInfectionData data = new ChunkInfectionData();
        CompoundTag chunks = tag.getCompound("chunks");
        for (String key : chunks.getAllKeys()) {
            try {
                data.contamination.put(Long.parseLong(key), chunks.getInt(key));
            } catch (NumberFormatException ignored) {}
        }
        return data;
    }
}
