package br.com.murilo.liberthia.faction;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Per-player faction reputation, saved at the overworld level. Values clamp to [-100, +100].
 *
 *   +score in BLOOD  → more friendly with cultists (less aggro), discounted blood crafts
 *   +score in ORDER  → more friendly with paladins, bonus from shrines
 *
 *   Killing cultists: Blood -5, Order +3
 *   Praying at Order Shrine: Order +10
 *   Drinking Blood Cure Pill: Blood -2
 */
public class FactionReputation extends SavedData {

    private static final String SAVE_NAME = "liberthia_faction_reputation";

    private final Map<UUID, int[]> scores = new HashMap<>();

    public FactionReputation() {}

    private int[] getRow(UUID id) {
        return scores.computeIfAbsent(id, k -> new int[Faction.values().length]);
    }

    public int get(UUID id, Faction f) {
        return getRow(id)[f.ordinal()];
    }

    public void add(UUID id, Faction f, int delta) {
        int[] row = getRow(id);
        row[f.ordinal()] = Math.max(-100, Math.min(100, row[f.ordinal()] + delta));
        setDirty();
    }

    public void set(UUID id, Faction f, int value) {
        int[] row = getRow(id);
        row[f.ordinal()] = Math.max(-100, Math.min(100, value));
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        CompoundTag entries = new CompoundTag();
        for (Map.Entry<UUID, int[]> e : scores.entrySet()) {
            CompoundTag row = new CompoundTag();
            int[] arr = e.getValue();
            row.putInt("blood", arr[Faction.BLOOD.ordinal()]);
            row.putInt("order", arr[Faction.ORDER.ordinal()]);
            entries.put(e.getKey().toString(), row);
        }
        tag.put("Entries", entries);
        return tag;
    }

    public static FactionReputation load(CompoundTag tag) {
        FactionReputation data = new FactionReputation();
        CompoundTag entries = tag.getCompound("Entries");
        for (String key : entries.getAllKeys()) {
            try {
                UUID id = UUID.fromString(key);
                CompoundTag row = entries.getCompound(key);
                int[] arr = new int[Faction.values().length];
                arr[Faction.BLOOD.ordinal()] = row.getInt("blood");
                arr[Faction.ORDER.ordinal()] = row.getInt("order");
                data.scores.put(id, arr);
            } catch (IllegalArgumentException ignored) {}
        }
        return data;
    }

    public static FactionReputation forLevel(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(
                FactionReputation::load, FactionReputation::new, SAVE_NAME);
    }
}
