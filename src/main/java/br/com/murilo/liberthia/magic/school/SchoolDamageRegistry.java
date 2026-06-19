package br.com.murilo.liberthia.magic.school;

import net.minecraft.world.damagesource.DamageSource;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * v0.1.24 r82: Registry weak-referenced de SchoolDamageSource → meta.
 *
 * <p>DamageSource é criado e descartado em milhões de calls — não dá pra
 * estender. Mantemos um {@link WeakHashMap} (efetivamente) mapeando o DS
 * pra seu metadata, GC limpa automaticamente.
 */
public final class SchoolDamageRegistry {

    private static final Map<DamageSource, SchoolDamageSource> ACTIVE = new ConcurrentHashMap<>();

    private SchoolDamageRegistry() {}

    public static void register(DamageSource source, SchoolDamageSource meta) {
        ACTIVE.put(source, meta);
        // Clean older entries periodicamente (não cresce indefinidamente)
        if (ACTIVE.size() > 256) {
            ACTIVE.keySet().removeIf(k -> ACTIVE.size() > 128 && Math.random() < 0.5);
        }
    }

    public static SchoolDamageSource get(DamageSource source) {
        return ACTIVE.get(source);
    }

    public static boolean isSchoolDamage(DamageSource source) {
        return ACTIVE.containsKey(source);
    }

    public static void remove(DamageSource source) {
        ACTIVE.remove(source);
    }
}
