package br.com.murilo.liberthia.loom;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

/**
 * v0.1.22 r33: chaves e constantes da dimensão LOOM — pure dark matter,
 * floating islands, sky roxo, sun roxo escuro, moon cinza.
 */
public final class LoomDimension {

    public static final ResourceKey<Level> LOOM_WORLD = ResourceKey.create(
            Registries.DIMENSION,
            new ResourceLocation("liberthia", "loom"));

    /** Ticks que player pode ficar sem dano de matéria pura (com armor). */
    public static final int RAW_DAMAGE_INTERVAL = 40; // 2s
    public static final float RAW_DAMAGE_PER_TICK = 1.0F;

    /** NBT key — turistas marcados ao voltar pra overworld (efeitos de paranoia). */
    public static final String NBT_VISITED = "liberthia.visited_loom";
    public static final String NBT_LAST_RETURN_TICK = "liberthia.loom_return_tick";

    private LoomDimension() {}
}
