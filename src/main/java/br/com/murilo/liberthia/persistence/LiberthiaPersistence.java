package br.com.murilo.liberthia.persistence;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Backup file persistente — armazena snapshot do estado dos BEs Liberthia
 * (energia, inventário) num arquivo separado em {@code world/data/liberthia_persistence.dat}.
 *
 * <p><b>Por que:</b> mesmo com {@code setChanged()} corretamente chamado em
 * todas mutações, há cenários edge-case onde o chunk save vanilla pode falhar
 * (crash, kill -9, etc.). Este SavedData é a "rede de segurança": tickamos
 * snapshots periódicos. Se um BE carrega com estado vazio mas a SavedData tem
 * snapshot recente, restauramos.
 *
 * <p>Formato em {@code save()}: chave = "x,y,z" (BlockPos como string),
 * valor = CompoundTag completo do BE + timestamp (gameTime).
 */
public class LiberthiaPersistence extends SavedData {

    private static final String NAME = "liberthia_persistence";
    private static final String KEY_TIME = "Time";
    /** Snapshot é válido por essa janela de tempo. Antigo demais = ignorar. */
    public static final long MAX_AGE_TICKS = 24_000L * 7; // 7 dias de jogo

    private final Map<String, CompoundTag> snapshots = new HashMap<>();

    public static LiberthiaPersistence get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                LiberthiaPersistence::load,
                LiberthiaPersistence::new,
                NAME
        );
    }

    private static String key(BlockPos pos) {
        return pos.getX() + "," + pos.getY() + "," + pos.getZ();
    }

    /** Salva um snapshot do BE pra esta posição. */
    public void snapshot(ServerLevel level, BlockPos pos, CompoundTag beNbt) {
        CompoundTag entry = beNbt.copy();
        entry.putLong(KEY_TIME, level.getGameTime());
        snapshots.put(key(pos), entry);
        setDirty();
    }

    /** Lê o snapshot mais recente. Retorna null se não houver ou for antigo demais. */
    @Nullable
    public CompoundTag getSnapshot(ServerLevel level, BlockPos pos) {
        CompoundTag entry = snapshots.get(key(pos));
        if (entry == null) return null;
        long age = level.getGameTime() - entry.getLong(KEY_TIME);
        if (age > MAX_AGE_TICKS) {
            snapshots.remove(key(pos));
            setDirty();
            return null;
        }
        return entry;
    }

    /** Remove snapshot — chamado quando o bloco é DESTRUÍDO legitimamente
     *  (player quebrou, drops caíram). Evita restaurar bloco quebrado. */
    public void clear(BlockPos pos) {
        if (snapshots.remove(key(pos)) != null) setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        CompoundTag map = new CompoundTag();
        for (var e : snapshots.entrySet()) {
            map.put(e.getKey(), e.getValue());
        }
        tag.put("snapshots", map);
        return tag;
    }

    public static LiberthiaPersistence load(CompoundTag tag) {
        LiberthiaPersistence data = new LiberthiaPersistence();
        if (tag.contains("snapshots")) {
            CompoundTag map = tag.getCompound("snapshots");
            for (String k : map.getAllKeys()) {
                data.snapshots.put(k, map.getCompound(k));
            }
        }
        return data;
    }
}
