package br.com.murilo.liberthia.persistence;

import br.com.murilo.liberthia.LiberthiaMod;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Apenas limpa snapshots quando blocos Persistable são destruídos.
 * O snapshot em si é feito pelos BEs no tick deles (cada um chama
 * {@code LiberthiaPersistence.get(level).snapshot(...)} a cada 600t).
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class PersistenceHandler {

    public static final int SNAPSHOT_PERIOD = 600; // 30 segundos

    private PersistenceHandler() {}

    /** Quando jogador quebra um Persistable, remove o snapshot pra evitar
     *  ressurreição zumbi do bloco no próximo restart. */
    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel sl)) return;
        BlockPos pos = event.getPos();
        BlockEntity be = sl.getBlockEntity(pos);
        if (be instanceof Persistable) {
            LiberthiaPersistence.get(sl).clear(pos);
        }
    }

    /**
     * No shutdown do server, força snapshot final de TODOS os Persistable BEs
     * em chunks carregados. Garante que kill -9, /stop, ou crash perdendo o
     * último tick não destruam estado.
     */
    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        LiberthiaMod.LOGGER.info("[Liberthia] ServerStoppingEvent disparado — snapshotando Persistable BEs");
        int count = 0;
        synchronized (Persistable.LIVE) {
            for (BlockEntity be : Persistable.LIVE) {
                if (be == null || be.isRemoved()) continue;
                if (!(be.getLevel() instanceof ServerLevel sl)) continue;
                LiberthiaPersistence.get(sl).snapshot(sl, be.getBlockPos(), be.saveWithFullMetadata());
                count++;
            }
        }
        LiberthiaMod.LOGGER.info("[Liberthia] {} Persistable BEs snapshotados — forçando flush em disco", count);

        // FORÇA flush imediato em disco — não confia no autosave ou save de mundo.
        for (ServerLevel sl : event.getServer().getAllLevels()) {
            try {
                sl.getDataStorage().save();
                LiberthiaMod.LOGGER.info("[Liberthia] DataStorage flushed em {}", sl.dimension().location());
            } catch (Exception ex) {
                LiberthiaMod.LOGGER.error("[Liberthia] Erro flushing DataStorage em {}: {}",
                        sl.dimension().location(), ex.getMessage());
            }
        }
    }
}
