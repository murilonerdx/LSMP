package br.com.murilo.liberthia.world;

import br.com.murilo.liberthia.LiberthiaMod;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * Gera rifts dimensionais novos ao longo do tempo, perto de jogadores online.
 *
 * <p>A cada {@link #PERIOD} ticks (10 minutos), pega um jogador aleatório e
 * tenta colocar um rift novo num raio de 500–2000 blocos dele, desde que:
 * <ul>
 *   <li>Não haja outro rift dentro de {@link #MIN_DISTANCE_TO_OTHER_RIFTS} blocos.</li>
 *   <li>O total de rifts na dimensão não exceda {@link #MAX_RIFTS}.</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class RiftGenerationHandler {

    private static final int PERIOD = 12_000;            // 10 min
    private static final int MAX_RIFTS = 25;
    private static final int MIN_DIST_FROM_PLAYER = 500;
    private static final int MAX_DIST_FROM_PLAYER = 2_000;
    private static final int MIN_DISTANCE_TO_OTHER_RIFTS = 250;

    private RiftGenerationHandler() {}

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.level instanceof ServerLevel sl)) return;
        if (sl.getGameTime() % PERIOD != 0) return;

        RiftSavedData data = RiftSavedData.get(sl);
        if (data.getRifts().size() >= MAX_RIFTS) return;

        List<ServerPlayer> players = sl.players();
        if (players.isEmpty()) return;
        ServerPlayer p = players.get(sl.random.nextInt(players.size()));

        for (int attempt = 0; attempt < 12; attempt++) {
            double angle = sl.random.nextDouble() * Math.PI * 2;
            double dist = MIN_DIST_FROM_PLAYER
                    + sl.random.nextDouble() * (MAX_DIST_FROM_PLAYER - MIN_DIST_FROM_PLAYER);
            int x = p.getBlockX() + (int) (Math.cos(angle) * dist);
            int z = p.getBlockZ() + (int) (Math.sin(angle) * dist);
            int y = -50 + sl.random.nextInt(130);
            BlockPos pos = new BlockPos(x, y, z);

            // Distância mínima de outros rifts
            int minSq = MIN_DISTANCE_TO_OTHER_RIFTS * MIN_DISTANCE_TO_OTHER_RIFTS;
            boolean tooClose = data.getRifts().stream()
                    .anyMatch(r -> r.distSqr(pos) < minSq);
            if (tooClose) continue;

            data.addRift(pos);
            LiberthiaMod.LOGGER.info("[Liberthia] Novo rift dimensional gerado: {}", pos);
            break;
        }
    }
}
