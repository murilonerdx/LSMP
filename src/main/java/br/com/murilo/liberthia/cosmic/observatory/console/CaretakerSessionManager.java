package br.com.murilo.liberthia.cosmic.observatory.console;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.cosmic.hallucination.HallucinationManager;
import br.com.murilo.liberthia.cosmic.hallucination.HallucinationType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * v0.1.22 r50: Manager das sessões do Caretaker Console.
 *
 * <p>A cada tick, varre sessões ativas e injeta mensagens fake APENAS no
 * target (per-player hallucination). Não broadcasta pra mais ninguém.
 *
 * <p>Sessions persistem em memória — limpas em logout do target.
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class CaretakerSessionManager {

    private static final Map<UUID, CaretakerSession> SESSIONS = new ConcurrentHashMap<>();

    private CaretakerSessionManager() {}

    public static void start(CaretakerSession session) {
        SESSIONS.put(session.targetId, session);
        LiberthiaMod.LOGGER.info("[CaretakerConsole] session start for {} ({} msgs)",
                session.targetId, session.messages.size());
    }

    public static void stop(UUID targetId) {
        if (SESSIONS.remove(targetId) != null) {
            LiberthiaMod.LOGGER.info("[CaretakerConsole] session end for {}", targetId);
        }
    }

    public static boolean hasActive(UUID targetId) {
        return SESSIONS.containsKey(targetId);
    }

    public static CaretakerSession get(UUID targetId) {
        return SESSIONS.get(targetId);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (SESSIONS.isEmpty()) return;
        long now = event.getServer().overworld().getGameTime();
        if (now % 20 != 0) return; // checa 1×/sec

        MinecraftServer server = event.getServer();
        Iterator<Map.Entry<UUID, CaretakerSession>> it = SESSIONS.entrySet().iterator();
        while (it.hasNext()) {
            CaretakerSession s = it.next().getValue();

            // Expiration
            if (s.remainingTicks > 0) {
                s.remainingTicks -= 20;
                if (s.remainingTicks <= 0) {
                    it.remove();
                    continue;
                }
            }

            // Time pra próxima mensagem?
            if (now < s.nextFireTick) continue;

            ServerPlayer target = server.getPlayerList().getPlayer(s.targetId);
            if (target == null) continue; // offline, mantém session

            String msg = s.pickRandomMessage();
            if (msg == null || msg.isEmpty()) continue;

            // Injeta APENAS pro target via FAKE_CHAT_MESSAGE (per-player)
            HallucinationManager.force(target, HallucinationType.FAKE_CHAT_MESSAGE,
                    1.0F, 1, msg);

            // Próximo fire — variation ±20%
            int jitter = (int)((Math.random() - 0.5) * 2 * s.intervalTicks * 0.2);
            s.nextFireTick = now + s.intervalTicks + jitter;
        }
    }

    /** Cleanup quando player desloga. */
    public static void cleanup(UUID playerId) {
        SESSIONS.remove(playerId);
    }
}
