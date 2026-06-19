package br.com.murilo.liberthia.voice;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.network.packet.CrownVoiceTargetC2SPacket;
import br.com.murilo.liberthia.registry.ModItems;
import de.maxhenkel.voicechat.api.Position;
import de.maxhenkel.voicechat.api.ServerLevel;
import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.audiochannel.LocationalAudioChannel;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * v0.1.22 r30: Roteador de voz SVC pra "whisper" via Crown of Mass Possession.
 *
 * <p>Quando speaker tem a Crown segurando + voice target setado no NBT da crown,
 * cada pacote Opus é enviado como {@link LocationalAudioChannel} na CABEÇA do
 * target com distance = 3 blocos — então só o target ouve, e ninguém em volta
 * dele.
 *
 * <p>Cache: 1 canal por (senderUuid → targetUuid). Reset no logout/target switch.
 */
public final class CrownVoiceRelay {

    private static final Map<String, LocationalAudioChannel> CACHE = new ConcurrentHashMap<>();

    private CrownVoiceRelay() {}

    /** Chamado de {@link LiberthiaVoicePlugin#onMicPacket}. Return true se relay. */
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
            ServerPlayer mcSender = mc.getPlayerList().getPlayer(senderUuid);
            if (mcSender == null) return false;

            // Speaker precisa segurar Crown na mão (não funciona se só no inv)
            ItemStack crown = ItemStack.EMPTY;
            var c = ModItems.MASS_POSSESSION_CROWN.get();
            if (mcSender.getMainHandItem().is(c)) crown = mcSender.getMainHandItem();
            else if (mcSender.getOffhandItem().is(c)) crown = mcSender.getOffhandItem();
            if (crown.isEmpty()) return false;
            if (!crown.hasTag()) return false;
            if (!crown.getTag().hasUUID(CrownVoiceTargetC2SPacket.NBT_VOICE_TARGET)) return false;

            UUID targetUuid = crown.getTag().getUUID(CrownVoiceTargetC2SPacket.NBT_VOICE_TARGET);
            ServerPlayer mcTarget = mc.getPlayerList().getPlayer(targetUuid);
            if (mcTarget == null) return false;

            byte[] opus = event.getPacket().getOpusEncodedData();
            if (opus == null || opus.length == 0) return false;

            String key = senderUuid + "->" + targetUuid;
            LocationalAudioChannel channel = CACHE.get(key);
            if (channel == null) {
                ServerLevel svcLevel = server.fromServerLevel(
                        (net.minecraft.server.level.ServerLevel) mcTarget.level());
                if (svcLevel == null) return false;
                // Posiciona no rosto do target (eye height)
                Position pos = api.createPosition(
                        mcTarget.getX(), mcTarget.getEyeY(), mcTarget.getZ());
                UUID channelUuid = UUID.nameUUIDFromBytes(key.getBytes());
                channel = server.createLocationalAudioChannel(channelUuid, svcLevel, pos);
                if (channel == null) return false;
                // Distância 3 blocos — só o target ouve, ninguém em volta
                channel.setDistance(3.0F);
                CACHE.put(key, channel);
            }
            // Atualiza posição (target pode estar andando)
            channel.updateLocation(api.createPosition(
                    mcTarget.getX(), mcTarget.getEyeY(), mcTarget.getZ()));
            channel.send(opus);
            return true;
        } catch (Throwable t) {
            LiberthiaMod.LOGGER.debug("[CrownVoice] relay fail: {}", t.toString());
            return false;
        }
    }

    public static void cleanupSender(UUID senderUuid) {
        String prefix = senderUuid + "->";
        CACHE.keySet().removeIf(k -> k.startsWith(prefix));
    }

    public static void clearAll() { CACHE.clear(); }
    public static int cacheSize() { return CACHE.size(); }
}
