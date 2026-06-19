package br.com.murilo.liberthia.voice;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.item.WalkieTalkieItem;
import de.maxhenkel.voicechat.api.ServerLevel;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.audiochannel.StaticAudioChannel;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <b>Relay de voz do Walkie Talkie.</b> Roda dentro do plugin do Simple Voice
 * Chat ({@link LiberthiaVoicePlugin#onMicPacket}). Quando alguém fala segurando
 * um walkie LIGADO num código, a fala é entregue — via canal estático (rádio,
 * não-locacional, "no ouvido") — pra cada player que tem um walkie ligado no
 * MESMO código. Privado: só quem tem o código escuta.
 *
 * <p>Usa {@link StaticAudioChannel} pra que SÓ o destinatário ouça (diferente
 * do relay de antenas, que é locacional e qualquer um por perto escuta).
 */
public final class WalkieTalkieRelay {

    /** Cache: senderUuid + "->" + receiverUuid → canal estático. */
    private static final Map<String, Entry> CHANNELS = new ConcurrentHashMap<>();

    private record Entry(StaticAudioChannel channel, ResourceKey<Level> dim) {}

    private WalkieTalkieRelay() {}

    public static boolean relayMicPacket(MicrophonePacketEvent event) {
        try {
            VoicechatConnection conn = event.getSenderConnection();
            if (conn == null || conn.getPlayer() == null) return false;

            VoicechatServerApi server = LiberthiaVoicePlugin.getServerApi();
            if (server == null) return false;

            MinecraftServer mc = ServerLifecycleHooks.getCurrentServer();
            if (mc == null) return false;

            UUID senderUuid = conn.getPlayer().getUuid();
            ServerPlayer sender = mc.getPlayerList().getPlayer(senderUuid);
            if (sender == null) return false;

            // Só transmite se está SEGURANDO um walkie ligado com código.
            String freq = WalkieTalkieItem.getActiveTransmitFrequency(sender);
            if (freq == null || freq.isEmpty()) return false;

            byte[] opus = event.getPacket().getOpusEncodedData();
            if (opus == null || opus.length == 0) return false;

            int relayed = 0;
            for (ServerPlayer receiver : mc.getPlayerList().getPlayers()) {
                if (receiver.getUUID().equals(senderUuid)) continue;
                if (!WalkieTalkieItem.isListeningOn(receiver, freq)) continue;
                if (relayToPlayer(server, senderUuid, receiver, opus)) relayed++;
            }
            if (relayed > 0) {
                LiberthiaMod.LOGGER.debug("[WalkieTalkie] {} → {} ouvinte(s) no código {}",
                        sender.getName().getString(), relayed, freq);
            }
            return relayed > 0;
        } catch (Throwable t) {
            LiberthiaMod.LOGGER.warn("[WalkieTalkie] erro em relayMicPacket: {}", t.toString());
            return false;
        }
    }

    private static boolean relayToPlayer(VoicechatServerApi server, UUID senderUuid,
                                         ServerPlayer receiver, byte[] opus) {
        try {
            String key = senderUuid + "->" + receiver.getUUID();
            Entry entry = CHANNELS.get(key);
            // Recria se o ouvinte trocou de dimensão.
            if (entry != null && !entry.dim().equals(receiver.level().dimension())) {
                CHANNELS.remove(key);
                entry = null;
            }
            if (entry == null) {
                VoicechatConnection rconn = server.getConnectionOf(receiver.getUUID());
                if (rconn == null) return false;
                ServerLevel svcLevel = server.fromServerLevel(receiver.serverLevel());
                if (svcLevel == null) return false;
                UUID channelUuid = UUID.nameUUIDFromBytes(key.getBytes());
                StaticAudioChannel channel =
                        server.createStaticAudioChannel(channelUuid, svcLevel, rconn);
                if (channel == null) return false;
                entry = new Entry(channel, receiver.level().dimension());
                CHANNELS.put(key, entry);
            }
            entry.channel().send(opus);
            return true;
        } catch (Throwable t) {
            LiberthiaMod.LOGGER.debug("[WalkieTalkie] relay fail: {}", t.toString());
            return false;
        }
    }

    /** Logout: limpa canais onde o player é emissor OU receptor. */
    public static void cleanup(UUID uuid) {
        String pre = uuid + "->";
        String suf = "->" + uuid;
        CHANNELS.keySet().removeIf(k -> k.startsWith(pre) || k.endsWith(suf));
    }

    public static void clearAll() {
        CHANNELS.clear();
    }
}
