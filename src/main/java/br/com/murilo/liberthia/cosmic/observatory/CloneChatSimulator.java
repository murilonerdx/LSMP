package br.com.murilo.liberthia.cosmic.observatory;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.cosmic.hallucination.HallucinationManager;
import br.com.murilo.liberthia.cosmic.hallucination.HallucinationType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * v0.1.22 r48: <b>Clone Chat Simulator</b> — gera mensagens de chat
 * falsas como se o player TARGET estivesse digitando — visíveis APENAS
 * pra outros players próximos do clone.
 *
 * <h2>Manifestações</h2>
 * <ul>
 *   <li>Cada 30-120s, simula uma mensagem do target ("oi", "alguém aí?", "to indo")</li>
 *   <li>Inclui ocasional "typing pause" — pequena delay realista</li>
 *   <li>O victim NÃO vê a mensagem — outros sim</li>
 *   <li>Mensagens referenciam contexto local (céu, blocks, mobs)</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class CloneChatSimulator {

    public static class Session {
        public final UUID targetId;
        public final UUID cloneEntityId;
        public final long startTick;
        public long lastMsgTick;
        public long nextMsgTick;

        public Session(UUID targetId, UUID cloneId, long now) {
            this.targetId = targetId;
            this.cloneEntityId = cloneId;
            this.startTick = now;
            this.lastMsgTick = now;
            this.nextMsgTick = now + 600 + (long) (Math.random() * 1800); // 30s-90s
        }
    }

    private static final Map<UUID, Session> SESSIONS = new HashMap<>();

    /** Mensagens contextuais que o "clone" pode "enviar". */
    private static final String[] CLONE_MESSAGES = {
            "oi",
            "alguém aí?",
            "to indo até você",
            "olha o que achei",
            "espera",
            "viu aquilo?",
            "?",
            "to com problema",
            "voltei",
            "ainda tá ai?",
            "achei sua base",
            "não consigo dormir",
            "to ouvindo passos",
            "olha o céu",
            "to atrás de você",
            "ressuscitei",
            "/spawn não funciona",
            "morri pra zombie",
            "ja vou",
            "/home"
    };

    private CloneChatSimulator() {}

    public static void startSession(ServerPlayer target, ReflectionEntity clone) {
        Session s = new Session(target.getUUID(), clone.getUUID(),
                target.level().getGameTime());
        SESSIONS.put(target.getUUID(), s);
        LiberthiaMod.LOGGER.info("[CloneChat] session start for {}", target.getName().getString());
    }

    public static void stopSession(UUID targetId) {
        SESSIONS.remove(targetId);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        // r112: kill switch
        if (!br.com.murilo.liberthia.config.LiberthiaConfig.SERVER.cosmicChatSpamEnabled.get()) return;
        long now = event.getServer().overworld().getGameTime();
        if (now % 20 != 0) return; // 1s

        Iterator<Map.Entry<UUID, Session>> it = SESSIONS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Session> e = it.next();
            Session s = e.getValue();

            // Stop after 5 min (matches clone lifetime)
            if (now - s.startTick > 6000) {
                it.remove();
                continue;
            }

            if (now < s.nextMsgTick) continue;

            ServerPlayer target = event.getServer().getPlayerList().getPlayer(s.targetId);
            if (target == null) continue;

            // Generate fake message
            String msg = CLONE_MESSAGES[(int)(Math.random() * CLONE_MESSAGES.length)];
            String formatted = "§7<§f" + target.getName().getString() + "§7> §o" + msg;

            // Send to OTHER players within 64b of the clone (not target!)
            ReflectionEntity clone = findClone(event.getServer(), s.cloneEntityId);
            net.minecraft.world.phys.Vec3 origin = clone != null
                    ? clone.position()
                    : target.position();

            for (ServerPlayer near : event.getServer().getPlayerList().getPlayers()) {
                if (near.getUUID().equals(s.targetId)) continue; // skip target
                double dist = near.position().distanceTo(origin);
                if (dist > 64) continue;
                // Hallucination = fake chat msg só pra ele
                HallucinationManager.force(near, HallucinationType.FAKE_CHAT_MESSAGE,
                        1.0F, 1, formatted);
            }

            s.lastMsgTick = now;
            // Schedule next 30-120s
            s.nextMsgTick = now + 600 + (long)(Math.random() * 1800);
        }
    }

    private static ReflectionEntity findClone(net.minecraft.server.MinecraftServer server, UUID cloneId) {
        for (var level : server.getAllLevels()) {
            net.minecraft.world.entity.Entity ent = level.getEntity(cloneId);
            if (ent instanceof ReflectionEntity re) return re;
        }
        return null;
    }
}
