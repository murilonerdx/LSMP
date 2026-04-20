package br.com.murilo.liberthia.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class TeleportToolData {
    private static final String ROOT = "liberthiaTeleportTools";
    private static final String ANCHOR = "anchor";
    private static final String ANCHOR_DIM = "dimension";
    private static final String ANCHOR_X = "x";
    private static final String ANCHOR_Y = "y";
    private static final String ANCHOR_Z = "z";
    private static final String MARKED_PLAYERS = "markedPlayers";

    private TeleportToolData() {
    }

    public static void copyPersistedData(Player original, Player clone) {
        CompoundTag originalPersisted = original.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        if (originalPersisted.contains(ROOT, Tag.TAG_COMPOUND)) {
            clone.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG)
                    .put(ROOT, originalPersisted.getCompound(ROOT).copy());
        }
    }

    public static void setAnchor(ServerPlayer owner, ResourceKey<Level> dimension, double x, double y, double z) {
        CompoundTag root = getOrCreateRoot(owner);
        CompoundTag anchor = new CompoundTag();
        anchor.putString(ANCHOR_DIM, dimension.location().toString());
        anchor.putDouble(ANCHOR_X, x);
        anchor.putDouble(ANCHOR_Y, y);
        anchor.putDouble(ANCHOR_Z, z);
        root.put(ANCHOR, anchor);
    }

    public static Optional<TeleportAnchor> getAnchor(Player owner) {
        CompoundTag root = getRoot(owner);
        if (!root.contains(ANCHOR, Tag.TAG_COMPOUND)) {
            return Optional.empty();
        }

        CompoundTag anchor = root.getCompound(ANCHOR);
        if (!anchor.contains(ANCHOR_DIM, Tag.TAG_STRING)) {
            return Optional.empty();
        }

        ResourceLocation location = ResourceLocation.tryParse(anchor.getString(ANCHOR_DIM));
        if (location == null) {
            return Optional.empty();
        }

        ResourceKey<Level> dimension = ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, location);
        return Optional.of(new TeleportAnchor(
                dimension,
                anchor.getDouble(ANCHOR_X),
                anchor.getDouble(ANCHOR_Y),
                anchor.getDouble(ANCHOR_Z)
        ));
    }

    public static boolean addMarkedPlayer(ServerPlayer owner, UUID targetUuid) {
        CompoundTag root = getOrCreateRoot(owner);
        Set<UUID> current = new LinkedHashSet<>(getMarkedPlayers(owner));
        boolean added = current.add(targetUuid);
        writeMarkedPlayers(root, current);
        return added;
    }

    public static void removeMarkedPlayer(ServerPlayer owner, UUID targetUuid) {
        CompoundTag root = getOrCreateRoot(owner);
        Set<UUID> current = new LinkedHashSet<>(getMarkedPlayers(owner));
        current.remove(targetUuid);
        writeMarkedPlayers(root, current);
    }

    public static void clearMarkedPlayers(ServerPlayer owner) {
        CompoundTag root = getOrCreateRoot(owner);
        root.put(MARKED_PLAYERS, new ListTag());
    }

    public static List<UUID> getMarkedPlayers(Player owner) {
        CompoundTag root = getRoot(owner);
        List<UUID> result = new ArrayList<>();
        ListTag list = root.getList(MARKED_PLAYERS, Tag.TAG_STRING);
        for (Tag tag : list) {
            if (tag instanceof StringTag stringTag) {
                try {
                    result.add(UUID.fromString(stringTag.getAsString()));
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
        return result;
    }

    public static List<MarkedPlayerEntry> getMarkedEntries(ServerPlayer owner) {
        MinecraftServer server = owner.getServer();
        List<MarkedPlayerEntry> entries = new ArrayList<>();
        for (UUID uuid : getMarkedPlayers(owner)) {
            ServerPlayer target = server != null ? server.getPlayerList().getPlayer(uuid) : null;
            String name = target != null ? target.getGameProfile().getName() : uuid.toString();
            entries.add(new MarkedPlayerEntry(uuid, name));
        }
        return entries;
    }

    private static void writeMarkedPlayers(CompoundTag root, Set<UUID> markedPlayers) {
        ListTag list = new ListTag();
        for (UUID uuid : markedPlayers) {
            list.add(StringTag.valueOf(uuid.toString()));
        }
        root.put(MARKED_PLAYERS, list);
    }

    private static CompoundTag getRoot(Player player) {
        CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        if (!persisted.contains(ROOT, Tag.TAG_COMPOUND)) {
            persisted.put(ROOT, new CompoundTag());
        }
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
        return persisted.getCompound(ROOT);
    }

    private static CompoundTag getOrCreateRoot(Player player) {
        return getRoot(player);
    }
}
