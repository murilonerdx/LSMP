package br.com.murilo.liberthia.event;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.logic.entropy.EntropyTracker;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * r183 — drena a fila de revert da entropia em lotes por tick (evita freeze ao reverter
 * milhares de blocos de uma vez, seja ao quebrar o motor ou no comando revert all).
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class EntropyTickHandler {
    private EntropyTickHandler() {}

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent e) {
        if (e.phase == TickEvent.Phase.END) EntropyTracker.drainReverts();
    }
}
