package br.com.murilo.liberthia.observation.source;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.network.ModNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * v0.1.161 r136: <b>SourceSyncTicker</b> — sincroniza Source server -> client
 * a cada N ticks E quando o valor muda. Sem isso o HUD client mostra valor
 * antigo (persistent data nao sincroniza auto).
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class SourceSyncTicker {

    private static final int SYNC_INTERVAL = 10; // 0.5s — sync periodica
    private static final Map<UUID, Integer> lastSource = new HashMap<>();
    private static final Map<UUID, Integer> lastMaxSource = new HashMap<>();

    private SourceSyncTicker() {}

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        if (!(e.player instanceof ServerPlayer sp)) return;

        int cur = SourceData.get(sp);
        int max = SourceData.getMax(sp);
        UUID id = sp.getUUID();

        Integer prevCur = lastSource.get(id);
        Integer prevMax = lastMaxSource.get(id);

        // Sincroniza quando muda OU a cada SYNC_INTERVAL ticks (failsafe)
        boolean changed = prevCur == null || prevCur != cur || prevMax == null || prevMax != max;
        boolean periodic = sp.tickCount % SYNC_INTERVAL == 0;

        if (changed || periodic) {
            ModNetwork.sendToPlayer(sp, new SourceSyncS2CPacket(cur, max));
            lastSource.put(id, cur);
            lastMaxSource.put(id, max);
        }
    }

    /** Quando player loga, envia source inicial. */
    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer sp)) return;
        int cur = SourceData.get(sp);
        int max = SourceData.getMax(sp);
        ModNetwork.sendToPlayer(sp, new SourceSyncS2CPacket(cur, max));
        lastSource.put(sp.getUUID(), cur);
        lastMaxSource.put(sp.getUUID(), max);
    }

    /** Quando player desloga, limpa cache. */
    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        if (e.getEntity() != null) {
            lastSource.remove(e.getEntity().getUUID());
            lastMaxSource.remove(e.getEntity().getUUID());
        }
    }

    /**
     * r137 fix #7: clear cache no shutdown do server.
     * Em single-player o JVM persiste, e voltar pro main menu + abrir novo world
     * mantinha entries velhas que faziam "changed=false" pro UUID reusado,
     * resultando em primeiro tick sem sync (HUD 0/0 ate proximo periodic).
     */
    @SubscribeEvent
    public static void onServerStopping(net.minecraftforge.event.server.ServerStoppingEvent e) {
        lastSource.clear();
        lastMaxSource.clear();
    }
}
