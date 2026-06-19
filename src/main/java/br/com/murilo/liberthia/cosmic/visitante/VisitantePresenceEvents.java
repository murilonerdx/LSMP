package br.com.murilo.liberthia.cosmic.visitante;

import br.com.murilo.liberthia.LiberthiaMod;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * r180b — hooks Forge do {@link VisitantePresenceManager}. Auto-registra no
 * FORGE bus via {@code @Mod.EventBusSubscriber} (igual {@code HallucinationEvents}).
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class VisitantePresenceEvents {

    private VisitantePresenceEvents() {}

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        try {
            VisitantePresenceManager.onServerTick(event.getServer());
        } catch (Throwable t) {
            LiberthiaMod.LOGGER.warn("[Visitante] manager tick error: {}", t.toString());
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        VisitantePresenceManager.cleanup(event.getEntity().getUUID());
    }
}
