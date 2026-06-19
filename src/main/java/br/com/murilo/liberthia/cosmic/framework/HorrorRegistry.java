package br.com.murilo.liberthia.cosmic.framework;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * v0.1.24 r81: Registry central de sistemas de horror.
 *
 * <p>Inicialização em {@link HorrorFramework#init()}. Sistemas registrados
 * são iterados no main tick loop.
 *
 * <p>Pra adicionar um novo sistema: crie classe que implementa
 * {@link HorrorSystem}, registre via {@code HorrorRegistry.register(new MeuSistema())}
 * em {@link HorrorFramework#init()}.
 */
public final class HorrorRegistry {

    private static final Map<HorrorType, HorrorSystem> SYSTEMS = new EnumMap<>(HorrorType.class);

    private HorrorRegistry() {}

    public static void register(HorrorSystem system) {
        SYSTEMS.put(system.type(), system);
    }

    public static HorrorSystem get(HorrorType type) {
        return SYSTEMS.get(type);
    }

    public static Collection<HorrorSystem> all() {
        return Collections.unmodifiableCollection(SYSTEMS.values());
    }

    public static int size() {
        return SYSTEMS.size();
    }
}
