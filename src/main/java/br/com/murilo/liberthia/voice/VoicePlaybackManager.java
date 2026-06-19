package br.com.murilo.liberthia.voice;

import br.com.murilo.liberthia.LiberthiaMod;
import de.maxhenkel.voicechat.api.Position;
import de.maxhenkel.voicechat.api.ServerLevel;
import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.audiochannel.LocationalAudioChannel;
import de.maxhenkel.voicechat.api.audiochannel.StaticAudioChannel;
import de.maxhenkel.voicechat.api.opus.OpusEncoder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Toca clipes de voz salvos no banco DENTRO do mundo via Simple Voice Chat.
 *
 * <p>O playback é assíncrono — chama de uma thread separada por clipe, lê WAV
 * do backend, decodifica PCM e envia frames Opus de 20ms cada via
 * {@link LocationalAudioChannel}. Sleep de 20ms entre frames pra respeitar
 * a taxa real de reprodução.
 */
public final class VoicePlaybackManager {

    private static final ConcurrentHashMap<UUID, Future<?>> ACTIVE_PLAYS = new ConcurrentHashMap<>();
    private static final ExecutorService POOL = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "LiberthiaVoice-Playback");
        t.setDaemon(true);
        return t;
    });

    private VoicePlaybackManager() {}

    public static UUID playAtLocation(long clipId, String dimensionId,
                                       double x, double y, double z,
                                       float volume, String category) {
        VoicechatServerApi server = LiberthiaVoicePlugin.getServerApi();
        VoicechatApi api = LiberthiaVoicePlugin.getApi();
        if (server == null || api == null) {
            LiberthiaMod.LOGGER.warn("[LiberthiaVoice] SVC API offline; não toca clipe #{}", clipId);
            return null;
        }
        MinecraftServer mc = ServerLifecycleHooks.getCurrentServer();
        if (mc == null) return null;

        // Resolve a dimensão do MC
        String full = dimensionId == null || dimensionId.isBlank() ? "minecraft:overworld"
                : (dimensionId.contains(":") ? dimensionId : "minecraft:" + dimensionId);
        ResourceKey<Level> dimKey = ResourceKey.create(Registries.DIMENSION, ResourceLocation.tryParse(full));
        net.minecraft.server.level.ServerLevel mcLevel = mc.getLevel(dimKey);
        if (mcLevel == null) mcLevel = mc.overworld();

        // Wrap pro ServerLevel da API SVC
        ServerLevel svcLevel = server.fromServerLevel(mcLevel);
        if (svcLevel == null) {
            LiberthiaMod.LOGGER.warn("[LiberthiaVoice] não conseguiu wrappar ServerLevel");
            return null;
        }

        UUID playId = UUID.randomUUID();
        final ServerLevel finalLevel = svcLevel;
        Future<?> f = POOL.submit(() -> {
            try {
                streamWavToLocation(server, api, finalLevel, x, y, z, clipId, volume, category);
            } catch (Exception e) {
                LiberthiaMod.LOGGER.warn("[LiberthiaVoice] playback fail clip #{}: {}", clipId, e.getMessage(), e);
            } finally {
                ACTIVE_PLAYS.remove(playId);
            }
        });
        ACTIVE_PLAYS.put(playId, f);
        return playId;
    }

    public static void stop(UUID playId) {
        Future<?> f = ACTIVE_PLAYS.remove(playId);
        if (f != null) f.cancel(true);
    }

    public static int activePlaysCount() {
        return ACTIVE_PLAYS.size();
    }

    /** Cancela TODAS as reproduções ativas (usado por {@code /liberthia play stop}). */
    public static void stopAll() {
        for (UUID id : new ArrayList<>(ACTIVE_PLAYS.keySet())) {
            stop(id);
        }
    }

    /**
     * {@code /liberthia play <link>} — baixa+decodifica o áudio da URL e toca
     * pra TODOS os players online (canal estático = rádio no ouvido). Assíncrono.
     *
     * @param feedbackUuid player que recebe mensagens de status (pode ser null)
     */
    public static void playUrlForAll(String url, float volume, UUID feedbackUuid) {
        VoicechatServerApi server = LiberthiaVoicePlugin.getServerApi();
        VoicechatApi api = LiberthiaVoicePlugin.getApi();
        MinecraftServer mc = ServerLifecycleHooks.getCurrentServer();
        if (server == null || api == null || mc == null) {
            feedback(mc, feedbackUuid, "§cSimple Voice Chat não está ativo no servidor.");
            return;
        }
        final UUID playId = UUID.randomUUID();
        final MinecraftServer fmc = mc;
        Future<?> f = POOL.submit(() -> {
            try {
                feedback(fmc, feedbackUuid, "§7Baixando e decodificando áudio...");
                short[] pcm = AudioUrlDecoder.decodeToPcm48kMono(url);
                if (pcm == null || pcm.length == 0) {
                    feedback(fmc, feedbackUuid, "§cFalha ao decodificar. Use um link de áudio DIRETO (.mp3/.ogg/.wav) e tenha §lffmpeg§r§c instalado no servidor.");
                    return;
                }
                int secs = pcm.length / AudioUrlDecoder.SAMPLE_RATE;
                feedback(fmc, feedbackUuid, "§aPreparando §f~" + secs + "s§a de áudio...");
                streamPcmToAll(server, api, fmc, pcm, volume);
                feedback(fmc, feedbackUuid, "§aReprodução finalizada.");
            } catch (Exception e) {
                feedback(fmc, feedbackUuid, "§cErro no playback: " + e.getMessage());
                LiberthiaMod.LOGGER.warn("[PlayUrl] erro: {}", e.toString(), e);
            } finally {
                ACTIVE_PLAYS.remove(playId);
            }
        });
        ACTIVE_PLAYS.put(playId, f);
    }

    private static void feedback(MinecraftServer mc, UUID uuid, String msg) {
        if (mc == null || uuid == null) return;
        mc.execute(() -> {
            ServerPlayer p = mc.getPlayerList().getPlayer(uuid);
            if (p != null) p.sendSystemMessage(Component.literal("§5[Liberthia] §r" + msg));
        });
    }

    /**
     * Pré-encoda o PCM em frames Opus e envia, com timing cravado de 20ms, pra
     * todos os players online via canais estáticos. Reaproveita exatamente o
     * pipeline provado de {@link #streamWavToLocation}.
     */
    private static void streamPcmToAll(VoicechatServerApi server, VoicechatApi api,
                                       MinecraftServer mc, short[] pcm, float volume) {
        int sampleCount = pcm.length;
        OpusEncoder encoder = api.createEncoder();
        int samplesPerFrame = LiberthiaVoicePlugin.SAMPLES_PER_FRAME;
        int totalFrames = (sampleCount + samplesPerFrame - 1) / samplesPerFrame;
        byte[][] opusFrames = new byte[totalFrames][];
        try {
            short[] chunk = new short[samplesPerFrame];
            for (int frame = 0; frame < totalFrames; frame++) {
                int off = frame * samplesPerFrame;
                int len = Math.min(samplesPerFrame, sampleCount - off);
                System.arraycopy(pcm, off, chunk, 0, len);
                for (int i = len; i < samplesPerFrame; i++) chunk[i] = 0;
                if (volume != 1.0f) {
                    for (int i = 0; i < chunk.length; i++) {
                        int v = (int) (chunk[i] * volume);
                        if (v > Short.MAX_VALUE) v = Short.MAX_VALUE;
                        if (v < Short.MIN_VALUE) v = Short.MIN_VALUE;
                        chunk[i] = (short) v;
                    }
                }
                opusFrames[frame] = encoder.encode(chunk);
            }
        } finally {
            try { encoder.close(); } catch (Exception ignored) {}
        }

        // Cria um canal estático por player online (snapshot no início).
        List<StaticAudioChannel> channels = new ArrayList<>();
        for (ServerPlayer p : mc.getPlayerList().getPlayers()) {
            try {
                VoicechatConnection conn = server.getConnectionOf(p.getUUID());
                if (conn == null) continue;
                ServerLevel lvl = server.fromServerLevel(p.serverLevel());
                if (lvl == null) continue;
                StaticAudioChannel ch = server.createStaticAudioChannel(UUID.randomUUID(), lvl, conn);
                if (ch != null) channels.add(ch);
            } catch (Throwable ignored) {}
        }
        if (channels.isEmpty()) return;

        final long FRAME_NS = 20_000_000L;
        long startNanos = System.nanoTime();
        for (int frame = 0; frame < totalFrames; frame++) {
            if (Thread.currentThread().isInterrupted()) break;
            byte[] data = opusFrames[frame];
            for (StaticAudioChannel ch : channels) {
                try { ch.send(data); } catch (Throwable ignored) {}
            }
            long target = startNanos + (long) (frame + 1) * FRAME_NS;
            long sleepNanos = target - System.nanoTime();
            if (sleepNanos > 0) {
                try { Thread.sleep(sleepNanos / 1_000_000L, (int) (sleepNanos % 1_000_000L)); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); break; }
            }
        }
    }

    private static void streamWavToLocation(VoicechatServerApi server, VoicechatApi api,
                                            ServerLevel svcLevel,
                                            double x, double y, double z,
                                            long clipId, float volume, String category) throws Exception {
        byte[] wav = VoiceUploader.downloadClip(clipId);
        if (wav == null || wav.length < 44) {
            LiberthiaMod.LOGGER.warn("[LiberthiaVoice] clipe #{} vazio ou inacessível", clipId);
            return;
        }
        int dataOffset = findDataChunk(wav);
        if (dataOffset < 0) {
            LiberthiaMod.LOGGER.warn("[LiberthiaVoice] WAV mal formado clip #{}", clipId);
            return;
        }
        int dataLen = wav.length - dataOffset;
        int sampleCount = dataLen / 2; // 16-bit
        ByteBuffer bb = ByteBuffer.wrap(wav, dataOffset, dataLen).order(ByteOrder.LITTLE_ENDIAN);
        short[] pcm = new short[sampleCount];
        for (int i = 0; i < sampleCount; i++) pcm[i] = bb.getShort();

        Position pos = api.createPosition(x, y, z);
        UUID channelUuid = UUID.randomUUID();
        LocationalAudioChannel channel = server.createLocationalAudioChannel(channelUuid, svcLevel, pos);
        if (channel == null) {
            LiberthiaMod.LOGGER.warn("[LiberthiaVoice] createLocationalAudioChannel retornou null");
            return;
        }
        channel.setDistance(48f);

        // ============================================================
        // FASE 1: PRÉ-ENCODA TODOS os frames Opus ANTES de começar a enviar.
        // Sem isso, encode acontecia DURANTE o loop de envio, somando com
        // network jitter + Thread.sleep(20) impreciso = stutter "travadão"
        // brutal no jogo. Encode demora ~1-3ms/frame, então pré-encodar
        // um clipe de 5s (250 frames) leva ~500ms — aceitável, e depois
        // todos saem no timing perfeito.
        // ============================================================
        OpusEncoder encoder = api.createEncoder();
        int samplesPerFrame = LiberthiaVoicePlugin.SAMPLES_PER_FRAME;
        int totalFrames = (sampleCount + samplesPerFrame - 1) / samplesPerFrame;
        byte[][] opusFrames = new byte[totalFrames][];
        try {
            short[] chunk = new short[samplesPerFrame];
            for (int frame = 0; frame < totalFrames; frame++) {
                int off = frame * samplesPerFrame;
                int len = Math.min(samplesPerFrame, sampleCount - off);
                System.arraycopy(pcm, off, chunk, 0, len);
                for (int i = len; i < samplesPerFrame; i++) chunk[i] = 0;
                if (volume != 1.0f) {
                    for (int i = 0; i < chunk.length; i++) {
                        int v = (int) (chunk[i] * volume);
                        if (v > Short.MAX_VALUE) v = Short.MAX_VALUE;
                        if (v < Short.MIN_VALUE) v = Short.MIN_VALUE;
                        chunk[i] = (short) v;
                    }
                }
                opusFrames[frame] = encoder.encode(chunk);
            }
        } finally {
            try { encoder.close(); } catch (Exception ignored) {}
        }

        // ============================================================
        // FASE 2: ENVIA frames com timing CRAVADO usando System.nanoTime().
        // Cada frame deve sair exatamente a cada 20ms.
        // Antes: Thread.sleep(20) podia dormir 25-30ms em servers ocupados
        // (especialmente com whisper/ffmpeg consumindo CPU) = jitter.
        // Agora: calcula deadline absoluto e dorme pelo delta exato.
        // ============================================================
        final long FRAME_NS = 20_000_000L; // 20ms em nanos
        long startNanos = System.nanoTime();
        for (int frame = 0; frame < totalFrames; frame++) {
            if (Thread.currentThread().isInterrupted()) break;
            channel.send(opusFrames[frame]);
            long targetNanos = startNanos + (long) (frame + 1) * FRAME_NS;
            long now = System.nanoTime();
            long sleepNanos = targetNanos - now;
            if (sleepNanos > 0) {
                long sleepMs = sleepNanos / 1_000_000L;
                int sleepRem = (int) (sleepNanos % 1_000_000L);
                try { Thread.sleep(sleepMs, sleepRem); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); break; }
            }
            // Se sleepNanos for negativo, estamos ATRASADOS — não dorme, manda
            // o próximo na hora pra tentar recuperar. Se o atraso for grande
            // (>100ms), o cliente provavelmente vai pular esse trecho mas
            // pelo menos não acumula mais lag.
        }
        LiberthiaMod.LOGGER.debug("[LiberthiaVoice] playback finalizado clip #{} ({} frames pre-encoded)", clipId, totalFrames);
    }

    private static int findDataChunk(byte[] wav) {
        int i = 12;
        while (i + 8 <= wav.length) {
            String id = new String(wav, i, 4);
            ByteBuffer szBb = ByteBuffer.wrap(wav, i + 4, 4).order(ByteOrder.LITTLE_ENDIAN);
            int size = szBb.getInt();
            if ("data".equals(id)) return i + 8;
            i += 8 + size;
        }
        return -1;
    }
}
