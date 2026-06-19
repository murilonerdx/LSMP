package br.com.murilo.liberthia.voice;

import br.com.murilo.liberthia.LiberthiaMod;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.UUID;

/**
 * Captura PCM de UM player ao longo do tempo, agrupando em "utterances"
 * separados por silêncio. Quando o utterance termina, gera um arquivo WAV
 * em memória e dispara upload pro backend via {@link VoiceUploader}.
 *
 * <p>Thread safety: cada session é acessada de duas threads —
 *  (1) thread de I/O do SVC (chama {@link #appendPcm}) e
 *  (2) thread de tick do mod (chama {@link #shouldFlushBySilence}).
 *  Usamos sincronização por instância.
 */
final class VoiceCaptureSession {
    private final UUID playerUuid;
    private final ByteArrayOutputStream buffer = new ByteArrayOutputStream(48000 * 2); // ~1s
    private long startedAtMs = 0L;
    private long lastPacketMs = 0L;

    VoiceCaptureSession(UUID playerUuid) {
        this.playerUuid = playerUuid;
    }

    synchronized void appendPcm(short[] pcm) {
        long now = System.currentTimeMillis();
        if (startedAtMs == 0L) startedAtMs = now;
        lastPacketMs = now;
        // PCM 16-bit little-endian
        ByteBuffer bb = ByteBuffer.allocate(pcm.length * 2).order(ByteOrder.LITTLE_ENDIAN);
        for (short s : pcm) bb.putShort(s);
        try { buffer.write(bb.array()); }
        catch (IOException ignored) {}
    }

    synchronized boolean shouldFlushBySilence(long now) {
        return buffer.size() > 0 && (now - lastPacketMs) > LiberthiaVoicePlugin.UTTERANCE_END_SILENCE_MS;
    }

    synchronized boolean shouldFlushByMaxDuration(long now) {
        return buffer.size() > 0 && (now - startedAtMs) > LiberthiaVoicePlugin.MAX_UTTERANCE_MS;
    }

    synchronized void flushAndUpload() {
        if (buffer.size() == 0) return;
        byte[] pcmData = buffer.toByteArray();
        long durationMs = (long) ((pcmData.length / 2.0 / LiberthiaVoicePlugin.SAMPLE_RATE) * 1000);
        long ts = startedAtMs;

        // Reseta estado pra próximo utterance
        buffer.reset();
        startedAtMs = 0L;
        lastPacketMs = 0L;

        if (durationMs < LiberthiaVoicePlugin.MIN_UTTERANCE_MS) {
            // Muito curto — descarta (click acidental, etc.)
            return;
        }

        // Monta WAV em memória
        byte[] wav = wrapPcmAsWav(pcmData);

        // Filtro por tamanho — descarta utterances com WAV menor que MIN_UTTERANCE_BYTES.
        // Mais agressivo que o filtro de duração: pega ruídos curtos OU silêncios
        // longos com pouco volume real.
        if (wav.length < LiberthiaVoicePlugin.MIN_UTTERANCE_BYTES) {
            br.com.murilo.liberthia.LiberthiaMod.LOGGER.debug(
                    "[LiberthiaVoice] descartando clip pequeno: {} bytes < {} bytes mínimo ({}ms)",
                    wav.length, LiberthiaVoicePlugin.MIN_UTTERANCE_BYTES, durationMs);
            return;
        }

        // Coleta metadata do player (pos + nome)
        String playerName = "unknown";
        double x = 0, y = 0, z = 0;
        String dim = "minecraft:overworld";
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            ServerPlayer p = server.getPlayerList().getPlayer(playerUuid);
            if (p != null) {
                playerName = p.getGameProfile().getName();
                x = p.getX(); y = p.getY(); z = p.getZ();
                dim = p.level().dimension().location().toString();
            }
        }

        VoiceClipMetadata meta = new VoiceClipMetadata(
                playerUuid.toString(), playerName,
                ts, durationMs,
                x, y, z, dim
        );

        LiberthiaMod.LOGGER.debug("[LiberthiaVoice] flush {}ms de {} ({} bytes)",
                durationMs, playerName, wav.length);

        // Upload assíncrono — não bloqueia o thread de I/O do SVC
        VoiceUploader.uploadAsync(wav, meta);
    }

    /**
     * Encapsula PCM 16-bit 48kHz mono em um arquivo WAV legível por qualquer
     * player de áudio. Header de 44 bytes seguido pelos samples.
     */
    private static byte[] wrapPcmAsWav(byte[] pcm) {
        int sampleRate = LiberthiaVoicePlugin.SAMPLE_RATE;
        int channels = LiberthiaVoicePlugin.CHANNELS;
        int bitsPerSample = 16;
        int byteRate = sampleRate * channels * bitsPerSample / 8;
        int blockAlign = channels * bitsPerSample / 8;
        int dataSize = pcm.length;
        int fileSize = 36 + dataSize;

        ByteBuffer bb = ByteBuffer.allocate(44 + dataSize).order(ByteOrder.LITTLE_ENDIAN);
        bb.put("RIFF".getBytes());
        bb.putInt(fileSize);
        bb.put("WAVE".getBytes());
        bb.put("fmt ".getBytes());
        bb.putInt(16);                  // fmt chunk size (PCM)
        bb.putShort((short) 1);         // PCM format
        bb.putShort((short) channels);
        bb.putInt(sampleRate);
        bb.putInt(byteRate);
        bb.putShort((short) blockAlign);
        bb.putShort((short) bitsPerSample);
        bb.put("data".getBytes());
        bb.putInt(dataSize);
        bb.put(pcm);
        return bb.array();
    }
}

/** Metadata enviada junto com o WAV no upload. */
final class VoiceClipMetadata {
    final String playerUuid;
    final String playerName;
    final long ts;
    final long durationMs;
    final double x, y, z;
    final String dimension;

    VoiceClipMetadata(String playerUuid, String playerName, long ts, long durationMs,
                      double x, double y, double z, String dimension) {
        this.playerUuid = playerUuid;
        this.playerName = playerName;
        this.ts = ts;
        this.durationMs = durationMs;
        this.x = x; this.y = y; this.z = z;
        this.dimension = dimension;
    }
}
