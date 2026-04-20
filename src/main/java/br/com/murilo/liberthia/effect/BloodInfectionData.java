package br.com.murilo.liberthia.effect;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * World-level saved data mapping player UUID -> accumulated max-HP drain.
 * Survives server restarts, crashes, and dimension changes. Written to
 * <world>/data/liberthia_blood_infection.dat.
 */
public class BloodInfectionData extends SavedData {
    private static final String FILE_ID = "liberthia_blood_infection";
    private final Map<UUID, Double> drainByPlayer = new HashMap<>();

    public static BloodInfectionData load(CompoundTag tag) {
        BloodInfectionData d = new BloodInfectionData();
        CompoundTag entries = tag.getCompound("entries");
        for (String key : entries.getAllKeys()) {
            try {
                UUID id = UUID.fromString(key);
                d.drainByPlayer.put(id, entries.getDouble(key));
            } catch (IllegalArgumentException ignored) {}
        }
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        CompoundTag entries = new CompoundTag();
        for (Map.Entry<UUID, Double> e : drainByPlayer.entrySet()) {
            entries.putDouble(e.getKey().toString(), e.getValue());
        }
        tag.put("entries", entries);
        return tag;
    }

    public double getDrain(UUID id) {
        return drainByPlayer.getOrDefault(id, 0.0D);
    }

    public void setDrain(UUID id, double drain) {
        if (drain <= 0.0D) {
            drainByPlayer.remove(id);
        } else {
            drainByPlayer.put(id, drain);
        }
        setDirty();
    }

    public void clear(UUID id) {
        drainByPlayer.remove(id);
        setDirty();
    }

    /** Access singleton on overworld (server-wide storage). */
    public static BloodInfectionData get(MinecraftServer server) {
        ServerLevel overworld = server.overworld();
        return overworld.getDataStorage().computeIfAbsent(
                BloodInfectionData::load,
                BloodInfectionData::new,
                FILE_ID);
    }
}
