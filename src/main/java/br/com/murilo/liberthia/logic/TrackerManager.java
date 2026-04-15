package br.com.murilo.liberthia.logic;

import br.com.murilo.liberthia.LiberthiaMod;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class TrackerManager {

    private static final Map<UUID, TrackerData> TRACKED = new ConcurrentHashMap<>();

    private TrackerManager() {}

    public static void track(UUID targetId, String name) {
        TRACKED.put(targetId, new TrackerData(name));
    }

    public static void untrack(UUID targetId) {
        TRACKED.remove(targetId);
    }

    public static boolean isTracked(UUID targetId) {
        return TRACKED.containsKey(targetId);
    }

    public static TrackerData getData(UUID targetId) {
        return TRACKED.get(targetId);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (TRACKED.isEmpty()) return;

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        for (Map.Entry<UUID, TrackerData> entry : TRACKED.entrySet()) {
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            TrackerData data = entry.getValue();
            if (player != null && player.isAlive()) {
                data.x = player.getX();
                data.y = player.getY();
                data.z = player.getZ();
                data.dimension = player.level().dimension().location().toString();
                data.signalLost = false;
            } else {
                data.signalLost = true;
            }
        }
    }

    public static class TrackerData {
        public final String name;
        public double x, y, z;
        public String dimension = "?";
        public boolean signalLost = true;

        public TrackerData(String name) {
            this.name = name;
        }
    }
}
