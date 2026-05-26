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
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Persistência por dimensão de "rifts dimensionais" — pontos onde matéria
 * escura dimensional pode ser extraída pela {@code dimensional_extractor}
 * e que a {@code dimensional_compass} aponta.
 *
 * <p>Cada rift agora tem uma <b>capacidade finita</b>: quantos baldes podem
 * ser extraídos antes de se esgotar. Quando chega a 0, o rift é removido
 * automaticamente e o compass passa a apontar para o próximo.
 *
 * <p>Persistido em {@code data/liberthia_rifts.dat}. Aceita carregar tanto
 * o formato antigo (só posição) quanto o novo (posição + capacidade).
 */
public class RiftSavedData extends SavedData {

    private static final String NAME = "liberthia_rifts";
    /**
     * v0.1.13: aumentado de 8 → 16 rifts iniciais.
     * Razão: usuário reportou que a bússola "sempre apontava pro mesmo".
     * Com 8 rifts espalhados 400-1500 blocos em volta do spawn, qualquer
     * player numa região via apenas 1-2 acessíveis. Agora 16 garante que
     * mesmo após esgotar 2-3, ainda haja outros próximos pra explorar.
     */
    private static final int INITIAL_COUNT = 16;
    private static final int MIN_DIST = 250;
    private static final int MAX_DIST = 2500;

    /**
     * v0.1.13: reduzido de 100 → 30 baldes.
     * Razão: 100 baldes por rift = praticamente eterno. Reduz pra forçar o
     * jogador a se mover, e a bússola encontrar o "próximo mais próximo"
     * realmente fica útil.
     */
    public static final int INITIAL_CAPACITY = 30;

    /** rift pos → capacidade restante (em baldes). Quando chega a 0, é removido. */
    private final Map<BlockPos, Integer> rifts = new HashMap<>();

    public static RiftSavedData get(ServerLevel level) {
        RiftSavedData data = level.getDataStorage().computeIfAbsent(
                RiftSavedData::load,
                RiftSavedData::new,
                NAME
        );
        data.initIfEmpty(level);
        return data;
    }

    public Set<BlockPos> getRifts() { return rifts.keySet(); }
    public Map<BlockPos, Integer> getRiftsWithCapacity() { return rifts; }

    public void addRift(BlockPos pos) {
        if (rifts.putIfAbsent(pos, INITIAL_CAPACITY) == null) setDirty();
    }

    public void addRift(BlockPos pos, int capacity) {
        if (rifts.putIfAbsent(pos, capacity) == null) setDirty();
    }

    public boolean removeRift(BlockPos pos) {
        boolean ok = rifts.remove(pos) != null;
        if (ok) setDirty();
        return ok;
    }

    /** Capacidade restante do rift (0 se não existir). */
    public int getCapacity(BlockPos pos) {
        return rifts.getOrDefault(pos, 0);
    }

    /**
     * Consome 1 ponto de capacidade do rift. Se chegar a 0, o rift é REMOVIDO
     * automaticamente. Retorna a capacidade restante após o consumo
     * (-1 se o rift não existia).
     */
    public int consumeCapacity(BlockPos pos) {
        Integer cur = rifts.get(pos);
        if (cur == null) return -1;
        int next = cur - 1;
        if (next <= 0) {
            rifts.remove(pos);
            setDirty();
            return 0;
        }
        rifts.put(pos, next);
        setDirty();
        return next;
    }

    /** Rift mais próximo (ignora os esgotados, já que esses foram removidos). */
    @Nullable
    public BlockPos findNearest(BlockPos origin) {
        return rifts.keySet().stream()
                .min(Comparator.comparingDouble(p -> p.distSqr(origin)))
                .orElse(null);
    }

    /**
     * v0.1.13: encontra o N-ésimo rift mais próximo (0 = mais próximo, 1 = segundo,
     * etc). Usado pelo compass com SHIFT+click pra ciclar entre os rifts em vez
     * de sempre apontar pro mesmo. Retorna null se {@code rank >= rifts.size()}.
     */
    @Nullable
    public BlockPos findNthNearest(BlockPos origin, int rank) {
        return rifts.keySet().stream()
                .sorted(Comparator.comparingDouble(p -> p.distSqr(origin)))
                .skip(rank)
                .findFirst()
                .orElse(null);
    }

    /**
     * v0.1.13: encontra o rift mais próximo, EXCETO {@code exclude}. Útil quando
     * o jogador clica de novo na compass pra ver alternativas — pula o mais
     * próximo (que ele já conhece) e mostra o próximo.
     */
    @Nullable
    public BlockPos findNearestExcluding(BlockPos origin, @Nullable BlockPos exclude) {
        return rifts.keySet().stream()
                .filter(p -> exclude == null || !p.equals(exclude))
                .min(Comparator.comparingDouble(p -> p.distSqr(origin)))
                .orElse(null);
    }

    /** Quantos rifts ativos restam na dimensão. */
    public int activeCount() { return rifts.size(); }

    /** Popula rifts iniciais se ainda não houver nenhum. */
    private void initIfEmpty(ServerLevel level) {
        if (!rifts.isEmpty()) return;
        Random r = new Random(level.getSeed() ^ 0xDEADBEEFL);
        BlockPos spawn = level.getSharedSpawnPos();
        int generated = 0;
        for (int attempt = 0; attempt < 30 && generated < INITIAL_COUNT; attempt++) {
            double angle = r.nextDouble() * Math.PI * 2;
            double dist = MIN_DIST + r.nextDouble() * (MAX_DIST - MIN_DIST);
            int dx = (int) (Math.cos(angle) * dist);
            int dz = (int) (Math.sin(angle) * dist);
            int x = spawn.getX() + dx;
            int z = spawn.getZ() + dz;
            int y = -50 + r.nextInt(130);
            BlockPos pos = new BlockPos(x, y, z);
            if (rifts.putIfAbsent(pos, INITIAL_CAPACITY) == null) generated++;
        }
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        // Formato novo: lista de { pos: BlockPos, cap: int }
        ListTag list = new ListTag();
        for (Map.Entry<BlockPos, Integer> e : rifts.entrySet()) {
            CompoundTag entry = new CompoundTag();
            entry.put("pos", NbtUtils.writeBlockPos(e.getKey()));
            entry.putInt("cap", e.getValue());
            list.add(entry);
        }
        tag.put("rifts_v2", list);
        return tag;
    }

    public static RiftSavedData load(CompoundTag tag) {
        RiftSavedData data = new RiftSavedData();

        // Formato novo (rifts_v2)
        if (tag.contains("rifts_v2", Tag.TAG_LIST)) {
            ListTag list = tag.getList("rifts_v2", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag entry = list.getCompound(i);
                BlockPos pos = NbtUtils.readBlockPos(entry.getCompound("pos"));
                int cap = entry.contains("cap") ? entry.getInt("cap") : INITIAL_CAPACITY;
                data.rifts.put(pos, cap);
            }
            return data;
        }

        // Formato antigo (rifts) — migra atribuindo capacidade inicial
        if (tag.contains("rifts", Tag.TAG_LIST)) {
            ListTag list = tag.getList("rifts", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                BlockPos pos = NbtUtils.readBlockPos(list.getCompound(i));
                data.rifts.put(pos, INITIAL_CAPACITY);
            }
            data.setDirty();
        }
        return data;
    }
}
