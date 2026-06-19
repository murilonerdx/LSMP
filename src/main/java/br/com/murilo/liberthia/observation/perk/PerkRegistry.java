package br.com.murilo.liberthia.observation.perk;

import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

/**
 * v0.1.22 r62: Registry simples pros Perks. Singleton pattern.
 */
public final class PerkRegistry {

    private static final Map<ResourceLocation, Perk> PERKS = new HashMap<>();

    private PerkRegistry() {}

    public static <T extends Perk> T register(T perk) {
        if (PERKS.containsKey(perk.id())) {
            throw new IllegalStateException("Perk already registered: " + perk.id());
        }
        PERKS.put(perk.id(), perk);
        return perk;
    }

    public static Perk get(ResourceLocation id) {
        return PERKS.get(id);
    }

    public static Map<ResourceLocation, Perk> all() {
        return PERKS;
    }
}
