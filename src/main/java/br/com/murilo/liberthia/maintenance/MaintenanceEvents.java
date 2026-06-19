package br.com.murilo.liberthia.maintenance;

import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Aplica as restaurações de chunk agendadas ANTES do mundo carregar.
 *
 * <p>Registrado manualmente em {@code LiberthiaMod} (padrão confiável do mod,
 * em vez de depender do scan de {@code @Mod.EventBusSubscriber}).
 * {@code ServerAboutToStartEvent} dispara antes do servidor abrir os levels, então
 * os {@code .mca} não estão abertos/locked — seguro pra sobrescrever.
 *
 * <p>{@link ChunkBackupManager#applyPendingRestores} é idempotente (arquiva o
 * pending file depois de aplicar), então mesmo um registro duplo não re-aplica.
 */
public final class MaintenanceEvents {

    private MaintenanceEvents() {}

    @SubscribeEvent
    public static void onServerAboutToStart(ServerAboutToStartEvent event) {
        ChunkBackupManager.applyPendingRestores(event.getServer());
    }
}
