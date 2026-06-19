package br.com.murilo.liberthia.voice;

import br.com.murilo.liberthia.LiberthiaMod;
import de.maxhenkel.voicechat.api.Position;
import de.maxhenkel.voicechat.api.ServerLevel;
import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.audiochannel.LocationalAudioChannel;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import de.maxhenkel.voicechat.api.packets.MicrophonePacket;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * v0.1.22 r28: Relay de voz Simple Voice Chat via antenas.
 *
 * <h2>Como funciona</h2>
 * Quando um player fala e está dentro de {@link AntennaNetwork#BROADCAST_RADIUS}
 * de uma antena ativa registrada, o pacote Opus é forwarded EM TEMPO REAL pra
 * todas as OUTRAS antenas tunadas na mesma frequência, tocando como áudio
 * locacional na posição de cada antena receptora.
 *
 * <h2>Otimização</h2>
 * Mantém cache de {@link LocationalAudioChannel} por (sender_uuid, receiver_globalpos)
 * pra não criar novos canais a cada frame Opus de 20ms.
 *
 * <h2>Cleanup</h2>
 * Canais são removidos do cache se o sender desconectar ou se a antena
 * receptora desregistrar. Cache é checked-on-write a cada relay.
 */
public final class AntennaVoiceRelay {

    /**
     * Cache de canais SVC: key = senderUuid + "@" + receiverGlobalPos.
     * Cada canal é único por (quem fala, qual antena recebe).
     */
    private static final Map<String, LocationalAudioChannel> CHANNEL_CACHE =
            new ConcurrentHashMap<>();

    private AntennaVoiceRelay() {}

    /**
     * Hook principal — chamado de {@link LiberthiaVoicePlugin#onMicPacket}
     * pra cada pacote de microfone.
     *
     * @return true se foi feito relay (gera tracking); false se ninguém ouviu
     */
    public static boolean relayMicPacket(MicrophonePacketEvent event) {
        try {
            VoicechatConnection conn = event.getSenderConnection();
            if (conn == null || conn.getPlayer() == null) return false;

            VoicechatServerApi server = LiberthiaVoicePlugin.getServerApi();
            VoicechatApi api = LiberthiaVoicePlugin.getApi();
            if (server == null || api == null) return false;

            MinecraftServer mc = ServerLifecycleHooks.getCurrentServer();
            if (mc == null) return false;

            UUID senderUuid = conn.getPlayer().getUuid();
            net.minecraft.server.level.ServerPlayer mcPlayer =
                    mc.getPlayerList().getPlayer(senderUuid);
            if (mcPlayer == null) return false;

            // Acha antena ativa próxima do speaker
            GlobalPos sourceAntenna = AntennaNetwork.findNearestActive(
                    mcPlayer, AntennaNetwork.BROADCAST_RADIUS);
            if (sourceAntenna == null) return false;

            String freq = AntennaNetwork.getFrequency(sourceAntenna);
            if (freq == null || freq.isEmpty()) return false;

            // Acha todas outras antenas tunadas na mesma freq
            var tuned = AntennaNetwork.getTunedAntennas(freq);
            if (tuned.size() < 2) return false;

            MicrophonePacket packet = event.getPacket();
            byte[] opusData = packet.getOpusEncodedData();
            if (opusData == null || opusData.length == 0) return false;

            int relayed = 0;
            for (GlobalPos receiver : tuned) {
                if (receiver.equals(sourceAntenna)) continue;
                if (relayToAntenna(server, api, mc, senderUuid, receiver, opusData)) {
                    relayed++;
                }
            }
            if (relayed > 0) {
                LiberthiaMod.LOGGER.debug(
                        "[AntennaVoice] {} → {} antena(s) na freq {}",
                        mcPlayer.getName().getString(), relayed, freq);
            }
            return relayed > 0;
        } catch (Throwable t) {
            LiberthiaMod.LOGGER.warn("[AntennaVoice] erro em relayMicPacket: {}",
                    t.toString(), t);
            return false;
        }
    }

    private static boolean relayToAntenna(VoicechatServerApi server, VoicechatApi api,
                                          MinecraftServer mc, UUID senderUuid,
                                          GlobalPos receiverPos, byte[] opusData) {
        try {
            String key = senderUuid + "@" + receiverPos.dimension().location() + ":" + receiverPos.pos();
            LocationalAudioChannel channel = CHANNEL_CACHE.get(key);
            if (channel == null) {
                ResourceKey<Level> dimKey = receiverPos.dimension();
                net.minecraft.server.level.ServerLevel mcLevel = mc.getLevel(dimKey);
                if (mcLevel == null) return false;

                ServerLevel svcLevel = server.fromServerLevel(mcLevel);
                if (svcLevel == null) return false;

                Position pos = api.createPosition(
                        receiverPos.pos().getX() + 0.5,
                        receiverPos.pos().getY() + 1.0,
                        receiverPos.pos().getZ() + 0.5);
                UUID channelUuid = UUID.nameUUIDFromBytes(key.getBytes());
                channel = server.createLocationalAudioChannel(channelUuid, svcLevel, pos);
                if (channel == null) return false;
                channel.setDistance((float) AntennaNetwork.BROADCAST_RADIUS);
                CHANNEL_CACHE.put(key, channel);
            }
            channel.send(opusData);
            return true;
        } catch (Throwable t) {
            LiberthiaMod.LOGGER.debug("[AntennaVoice] relay fail: {}", t.toString());
            return false;
        }
    }

    /**
     * Limpa cache de canais de um sender específico (logout) ou quando uma
     * antena receptora é destruída.
     */
    public static void cleanupSender(UUID senderUuid) {
        String prefix = senderUuid + "@";
        CHANNEL_CACHE.keySet().removeIf(k -> k.startsWith(prefix));
    }

    /** Limpa canais de uma antena específica que foi desregistrada. */
    public static void cleanupReceiver(GlobalPos receiverPos) {
        String suffix = "@" + receiverPos.dimension().location() + ":" + receiverPos.pos();
        CHANNEL_CACHE.keySet().removeIf(k -> k.endsWith(suffix));
    }

    /** Limpa TUDO (server stop). */
    public static void clearAll() {
        CHANNEL_CACHE.clear();
    }

    public static int cacheSize() {
        return CHANNEL_CACHE.size();
    }
}
