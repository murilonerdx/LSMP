package br.com.murilo.liberthia.persistence;

import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Marker interface — BEs que querem ser auto-snapshotados pra
 * {@link LiberthiaPersistence}. O handler tickado vai iterar todos os BEs
 * que implementam isso e snapshotar.
 *
 * <p>O BE também é responsável por consultar o snapshot em {@code load()}
 * e restaurar se seu estado interno parecer vazio.
 *
 * <p>Cada BE Persistable se registra no {@link #LIVE} em {@code onLoad} e
 * sai no {@code setRemoved}. Assim o {@code ServerStoppingEvent} consegue
 * achar todos sem mexer em APIs internas do Minecraft.
 */
public interface Persistable {
    /** Registry global de BEs Persistable carregados — keyed por identidade. */
    Set<BlockEntity> LIVE = Collections.synchronizedSet(
            Collections.newSetFromMap(new WeakHashMap<>()));

    /** True se o BE precisa restaurar do backup (load do NBT veio vazio). */
    boolean isStateEmpty();
    /** Carrega estado do snapshot (mesmo formato do {@code saveAdditional}). */
    void restoreFromSnapshot(net.minecraft.nbt.CompoundTag tag);
}
