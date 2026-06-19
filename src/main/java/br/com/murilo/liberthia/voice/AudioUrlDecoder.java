package br.com.murilo.liberthia.voice;

import br.com.murilo.liberthia.LiberthiaMod;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.BufferedInputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Baixa e decodifica áudio de uma URL pra PCM 48kHz mono 16-bit (formato que o
 * {@link VoicePlaybackManager} encoda em Opus pro Simple Voice Chat).
 *
 * <h2>Estratégia</h2>
 * <ol>
 *   <li><b>ffmpeg</b> (preferido): abre QUALQUER formato/URL (mp3, ogg, m3u8,
 *       streams, etc.), corta em {@link #MAX_SECONDS}s e cospe s16le 48kHz mono
 *       no stdout. Precisa do ffmpeg no PATH do servidor (o backend de voz já
 *       usa, então normalmente está disponível).</li>
 *   <li><b>Fallback Java</b>: {@code javax.sound.sampled} pra links diretos de
 *       WAV/AIFF/AU (sem precisar de ffmpeg). NÃO decodifica mp3/ogg.</li>
 * </ol>
 *
 * <p>Links de páginas (ex: YouTube) NÃO funcionam direto — precisa de um link
 * de mídia direto, ou ter yt-dlp encadeado no ffmpeg.
 */
public final class AudioUrlDecoder {

    public static final int SAMPLE_RATE = 48000;
    private static final int MAX_SECONDS = 240; // teto de 4 min por reprodução

    private AudioUrlDecoder() {}

    public static short[] decodeToPcm48kMono(String url) {
        short[] viaFfmpeg = tryFfmpeg(url);
        if (viaFfmpeg != null && viaFfmpeg.length > 0) return viaFfmpeg;
        try {
            return tryJavaSound(url);
        } catch (Throwable t) {
            LiberthiaMod.LOGGER.warn("[PlayUrl] fallback Java falhou: {}", t.toString());
            return null;
        }
    }

    private static short[] tryFfmpeg(String url) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "ffmpeg", "-hide_banner", "-loglevel", "error",
                    "-t", String.valueOf(MAX_SECONDS),
                    "-i", url,
                    "-vn", "-ac", "1", "-ar", String.valueOf(SAMPLE_RATE),
                    "-f", "s16le", "-");
            pb.redirectError(ProcessBuilder.Redirect.DISCARD);
            Process proc = pb.start();
            byte[] raw = proc.getInputStream().readAllBytes();
            int code = proc.waitFor();
            if (code != 0 || raw.length < 2) {
                LiberthiaMod.LOGGER.debug("[PlayUrl] ffmpeg saiu {} ({} bytes)", code, raw.length);
                return null;
            }
            LiberthiaMod.LOGGER.debug("[PlayUrl] ffmpeg decodificou {} bytes", raw.length);
            return bytesToShorts(raw);
        } catch (java.io.IOException e) {
            // ffmpeg provavelmente não está instalado/no PATH
            LiberthiaMod.LOGGER.debug("[PlayUrl] ffmpeg indisponível: {}", e.getMessage());
            return null;
        } catch (Throwable t) {
            LiberthiaMod.LOGGER.warn("[PlayUrl] ffmpeg erro: {}", t.toString());
            return null;
        }
    }

    private static short[] tryJavaSound(String url) throws Exception {
        URLConnection conn = new URL(url).openConnection();
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(15000);
        conn.setRequestProperty("User-Agent", "Liberthia/1.0");
        try (AudioInputStream in = AudioSystem.getAudioInputStream(
                new BufferedInputStream(conn.getInputStream()))) {
            AudioFormat target = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
                    SAMPLE_RATE, 16, 1, 2, SAMPLE_RATE, false);
            try (AudioInputStream conv = AudioSystem.getAudioInputStream(target, in)) {
                byte[] raw = conv.readAllBytes();
                return bytesToShorts(raw);
            }
        }
    }

    private static short[] bytesToShorts(byte[] raw) {
        int n = raw.length / 2;
        short[] pcm = new short[n];
        ByteBuffer.wrap(raw, 0, n * 2).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(pcm);
        return pcm;
    }
}
