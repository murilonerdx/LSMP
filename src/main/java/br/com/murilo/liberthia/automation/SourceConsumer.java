package br.com.murilo.liberthia.automation;

import br.com.murilo.liberthia.observation.source.SourceData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;

import java.util.Comparator;
import java.util.List;

/**
 * v0.1.156 r133: <b>SourceConsumer</b> — helper pra blocks de automação
 * consumirem Source do player mais proximo.
 *
 * <p>Como blocks nao tem "owner" natural, blocks como AutoMiner e SpellTurret
 * drenam Source de qualquer player dentro de um raio (default 16 blocos).
 *
 * <p>Filosofia: o player precisa estar PERTO da automação pra ela funcionar,
 * o que cria uma trade-off interessante (não pode AFK longe).
 */
public final class SourceConsumer {

    public static final int DEFAULT_RADIUS = 16;

    private SourceConsumer() {}

    /**
     * Tenta consumir {@code amount} Source de algum player proximo.
     *
     * @return true se conseguiu drenar; false se nenhum player tinha Source suficiente
     */
    public static boolean tryConsume(ServerLevel level, BlockPos pos, int amount) {
        return tryConsume(level, pos, amount, DEFAULT_RADIUS);
    }

    public static boolean tryConsume(ServerLevel level, BlockPos pos, int amount, int radius) {
        AABB box = new AABB(
                pos.getX() - radius, pos.getY() - radius, pos.getZ() - radius,
                pos.getX() + radius + 1, pos.getY() + radius + 1, pos.getZ() + radius + 1);

        List<ServerPlayer> players = level.getEntitiesOfClass(ServerPlayer.class, box);
        if (players.isEmpty()) return false;

        // Sort by closest first
        players.sort(Comparator.comparingDouble(p ->
                p.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5)));

        for (ServerPlayer p : players) {
            int src = SourceData.get(p);
            if (src >= amount) {
                SourceData.consume(p, amount);
                return true;
            }
        }
        return false;
    }

    /** Apenas checa se algum player proximo TEM Source — sem consumir. */
    public static boolean hasNearby(ServerLevel level, BlockPos pos, int minAmount) {
        return hasNearby(level, pos, minAmount, DEFAULT_RADIUS);
    }

    public static boolean hasNearby(ServerLevel level, BlockPos pos, int minAmount, int radius) {
        AABB box = new AABB(
                pos.getX() - radius, pos.getY() - radius, pos.getZ() - radius,
                pos.getX() + radius + 1, pos.getY() + radius + 1, pos.getZ() + radius + 1);
        List<ServerPlayer> players = level.getEntitiesOfClass(ServerPlayer.class, box);
        for (ServerPlayer p : players) {
            if (SourceData.get(p) >= minAmount) return true;
        }
        return false;
    }
}
