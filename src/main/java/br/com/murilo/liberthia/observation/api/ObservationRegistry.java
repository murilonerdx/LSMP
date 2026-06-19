package br.com.murilo.liberthia.observation.api;

import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

/**
 * v0.1.22 r60: <b>ObservationRegistry</b> — registry simples baseado em
 * ResourceLocation. Inspired by AN's GlyphRegistry.
 *
 * <p>Não é DeferredRegister (parts são static singletons), mas suporta lookup
 * por ID — útil pra serialização codec-based, networking, e save/load.
 */
public final class ObservationRegistry {

    private static final Map<ResourceLocation, ObservationPart> PARTS = new HashMap<>();

    private ObservationRegistry() {}

    public static <T extends ObservationPart> T register(T part) {
        ResourceLocation id = part.id();
        if (PARTS.containsKey(id)) {
            throw new IllegalStateException("ObservationPart already registered: " + id);
        }
        PARTS.put(id, part);
        return part;
    }

    public static ObservationPart get(ResourceLocation id) {
        return PARTS.get(id);
    }

    public static Map<ResourceLocation, ObservationPart> all() {
        return PARTS;
    }
}
