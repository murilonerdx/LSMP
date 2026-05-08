package br.com.murilo.liberthia.world;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/**
 * Persistência por dimensão de "rifts dimensionais" — pontos onde matéria
 * escura dimensional pode ser extraída pela {@code dimensional_extractor}
 * e que a {@code dimensional_compass} aponta.
 *
 * <p>Inicializado na primeira chamada com 6–10 rifts espalhados num raio de
 * 800–1500 blocos do spawn. Persistido em {@code data/liberthia_rifts.dat}.
 */
public class RiftSavedData extends SavedData {

    private static final String NAME = "liberthia_rifts";
    private static final int INITIAL_COUNT = 8;
    private static final int MIN_DIST = 400;
    private static final int MAX_DIST = 1500;

    private final Set<BlockPos> rifts = new HashSet<>();

    public static RiftSavedData get(ServerLevel level) {
        RiftSavedData data = level.getDataStorage().computeIfAbsent(
                RiftSavedData::load,
                RiftSavedData::new,
                NAME
        );
        data.initIfEmpty(level);
        return data;
    }

    public Set<BlockPos> getRifts() { return rifts; }

    public void addRift(BlockPos pos) {
        if (rifts.add(pos)) setDirty();
    }

    public boolean removeRift(BlockPos pos) {
        boolean ok = rifts.remove(pos);
        if (ok) setDirty();
        return ok;
    }

    @Nullable
    public BlockPos findNearest(BlockPos origin) {
        return rifts.stream()
                .min(Comparator.comparingDouble(p -> p.distSqr(origin)))
                .orElse(null);
    }

    /** Popula rifts iniciais se ainda não houver nenhum. */
    private void initIfEmpty(ServerLevel level) {
        if (!rifts.isEmpty()) return;
        Random r = new Random(level.getSeed() ^ 0xDEADBEEFL);
        BlockPos spawn = level.getSharedSpawnPos();
        int generated = 0;
        // Tenta até 30 vezes pra achar posições válidas
        for (int attempt = 0; attempt < 30 && generated < INITIAL_COUNT; attempt++) {
            double angle = r.nextDouble() * Math.PI * 2;
            double dist = MIN_DIST + r.nextDouble() * (MAX_DIST - MIN_DIST);
            int dx = (int) (Math.cos(angle) * dist);
            int dz = (int) (Math.sin(angle) * dist);
            int x = spawn.getX() + dx;
            int z = spawn.getZ() + dz;
            // Y aleatório entre -50 e 80
            int y = -50 + r.nextInt(130);
            BlockPos pos = new BlockPos(x, y, z);
            if (rifts.add(pos)) generated++;
        }
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (BlockPos p : rifts) list.add(NbtUtils.writeBlockPos(p));
        tag.put("rifts", list);
        return tag;
    }

    public static RiftSavedData load(CompoundTag tag) {
        RiftSavedData data = new RiftSavedData();
        if (tag.contains("rifts", Tag.TAG_LIST)) {
            ListTag list = tag.getList("rifts", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                data.rifts.add(NbtUtils.readBlockPos(list.getCompound(i)));
            }
        }
        return data;
    }
}
