package br.com.murilo.liberthia.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jetbrains.annotations.Nullable;

/**
 * Helpers para evitar operações que forçam geração parcial de chunks.
 *
 * <p><b>Por que isso importa:</b> chamar {@code level.getHeightmapPos(...)},
 * {@code level.getHeight(...)} ou {@code level.getChunk(x, z, status, true)}
 * em uma posição cuja chunk NÃO está carregada faz o servidor carregar /
 * gerar essa chunk no estágio mínimo necessário (HEIGHTMAPS, FEATURES, etc.).
 *
 * <p>Quando isso acontece em meio a um random tick ou tick handler — que roda
 * em chunks já carregadas e iterates pra chunks vizinhas — o resultado é a
 * "chunk recortada": parcialmente gerada, terreno faltando, blocos sumindo
 * quando o jogador chega lá. É o sintoma clássico de chunk corruption por
 * world-gen prematuro.
 *
 * <p>Uso: sempre envolver {@code getHeightmapPos} com {@link #safeHeightmap}
 * e tratar null (chunk não carregada → desistir da operação).
 */
public final class ChunkSafe {

    private ChunkSafe() {}

    /**
     * Versão segura de {@code level.getHeightmapPos(type, pos)}.
     *
     * @return null se a chunk não está carregada, ou o resultado real caso esteja.
     */
    public static @Nullable BlockPos safeHeightmap(LevelReader level, Heightmap.Types type, BlockPos pos) {
        if (!level.hasChunkAt(pos)) return null;
        return level.getHeightmapPos(type, pos);
    }

    /** Atalho — true se a chunk em {@code pos} está carregada e pronta. */
    public static boolean isLoaded(Level level, BlockPos pos) {
        return level.hasChunkAt(pos);
    }

    /** Atalho — true se a área é segura pra leitura (todos os 4 cantos da AABB de {@code radius}). */
    public static boolean isAreaLoaded(Level level, BlockPos center, int radius) {
        return level.hasChunkAt(center.offset(-radius, 0, -radius))
                && level.hasChunkAt(center.offset(radius, 0, -radius))
                && level.hasChunkAt(center.offset(-radius, 0, radius))
                && level.hasChunkAt(center.offset(radius, 0, radius));
    }
}
