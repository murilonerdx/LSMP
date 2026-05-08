package br.com.murilo.liberthia.persistence;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.items.ItemStackHandler;

import java.util.HashMap;
import java.util.Map;

/**
 * Storage global de inventários dimensionais. Cada "canal" (string ID) tem um
 * {@link ItemStackHandler} de 54 slots compartilhado por todos os Baús
 * Dimensionais que apontam para ele.
 *
 * <p>Persistido como SavedData no overworld — sobrevive a reinícios, é único
 * por mundo, e funciona cross-dimension.
 */
public class DimensionalStorage extends SavedData {
    private static final String NAME = "liberthia_dimensional";
    public static final int CHANNEL_SIZE = 54;

    private final Map<String, ItemStackHandler> channels = new HashMap<>();

    /** Pega ou cria o storage no overworld (canônico, cross-dim). */
    public static DimensionalStorage get(MinecraftServer server) {
        ServerLevel overworld = server.overworld();
        return overworld.getDataStorage().computeIfAbsent(
                DimensionalStorage::load, DimensionalStorage::new, NAME);
    }

    public static DimensionalStorage get(Level level) {
        if (level.getServer() == null) return new DimensionalStorage();
        return get(level.getServer());
    }

    public ItemStackHandler getOrCreateChannel(String channelId) {
        if (channelId == null || channelId.isEmpty()) channelId = "default";
        return channels.computeIfAbsent(channelId, k -> {
            setDirty();
            return new ItemStackHandler(CHANNEL_SIZE) {
                @Override protected void onContentsChanged(int slot) {
                    setDirty();
                }
            };
        });
    }

    /** Lista todos os canais existentes (pra debug / GUI). */
    public java.util.Set<String> getChannelNames() {
        return channels.keySet();
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        CompoundTag map = new CompoundTag();
        for (var e : channels.entrySet()) {
            map.put(e.getKey(), e.getValue().serializeNBT());
        }
        tag.put("channels", map);
        return tag;
    }

    public static DimensionalStorage load(CompoundTag tag) {
        DimensionalStorage data = new DimensionalStorage();
        if (tag.contains("channels")) {
            CompoundTag map = tag.getCompound("channels");
            for (String id : map.getAllKeys()) {
                ItemStackHandler h = new ItemStackHandler(CHANNEL_SIZE) {
                    @Override protected void onContentsChanged(int slot) { data.setDirty(); }
                };
                h.deserializeNBT(map.getCompound(id));
                data.channels.put(id, h);
            }
        }
        return data;
    }
}
