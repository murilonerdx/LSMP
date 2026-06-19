package br.com.murilo.liberthia.voice;

import br.com.murilo.liberthia.LiberthiaMod;
import de.maxhenkel.voicechat.api.ForgeVoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import de.maxhenkel.voicechat.api.events.VoicechatServerStartedEvent;
import de.maxhenkel.voicechat.api.opus.OpusDecoder;
import de.maxhenkel.voicechat.api.packets.MicrophonePacket;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Plugin do Simple Voice Chat — captura todo áudio falado pelos players,
 * agrupa em "utterances" (clipes separados por silêncio) e faz upload pro
 * backend Spring Boot.
 *
 * <p><b>Pontos críticos pra debug</b>:
 * <ul>
 *   <li>O Simple Voice Chat tem que estar instalado no servidor MC pra
 *       a annotation {@link ForgeVoicechatPlugin} ser detectada.</li>
 *   <li>Cada etapa loga em INFO — se você não vê NENHUM log começando com
 *       "[LiberthiaVoice]", o plugin não foi carregado (provável: SVC não
 *       instalado, jar não no mods/, ou versão incompatível).</li>
 *   <li>O contador {@link #packetsReceived} prova se eventos chegam ou não.
 *       Cheque via {@code /liberthia voice status}.</li>
 * </ul>
 */
@ForgeVoicechatPlugin
public class LiberthiaVoicePlugin implements VoicechatPlugin {

    public static final int SAMPLE_RATE = 48000;
    public static final int CHANNELS = 1;
    public static final int SAMPLES_PER_FRAME = 960; // 20ms a 48kHz
    /**
     * Silêncio que fecha um utterance. AUMENTADO de 800ms → 1500ms — antes
     * frases longas eram cortadas no meio quando a pessoa respirava ou pausava
     * por meio segundo. 1.5s dá margem pra fala natural sem agrupar 2 falas
     * separadas no mesmo clipe.
     */
    public static final long UTTERANCE_END_SILENCE_MS = 1500;
    /**
     * Utterances mais curtos que isso = ruído (click, tosse, suspiro). REDUZIDO
     * de 250ms → 400ms — abaixo disso o Whisper não consegue extrair texto útil
     * mesmo com modelo large, então não vale o custo de CPU.
     */
    public static final long MIN_UTTERANCE_MS = 400;
    /**
     * Filtro adicional por TAMANHO do WAV. REDUZIDO de 300KB → 80KB pra captar
     * frases curtas tipo "olá", "valeu", "okay", "vai pra cima dele" — antes
     * cortávamos qualquer clipe < 3s e a maioria das interações do servidor
     * caía fora. 80KB ≈ 0.85s de áudio (48kHz/16-bit/mono).
     *
     * Whisper transcreve clipes de 1-2s decentemente; abaixo disso quase sempre
     * é ruído. Vamos depender do MIN_UTTERANCE_MS=400ms pra cortar microruídos.
     */
    public static final long MIN_UTTERANCE_BYTES = 80 * 1024;
    /** Duração máxima de um clipe. Aumentada de 30s → 60s pra capturar histórias longas. */
    public static final long MAX_UTTERANCE_MS = 60_000;

    private static volatile VoicechatApi api;
    private static volatile VoicechatServerApi serverApi;
    private static final Map<UUID, VoiceCaptureSession> SESSIONS = new ConcurrentHashMap<>();
    private static final Map<UUID, OpusDecoder> DECODERS = new ConcurrentHashMap<>();
    // Contador global de eventos recebidos — usado no /liberthia voice status
    // pra diagnosticar se o plugin está realmente recebendo packets.
    private static final AtomicLong packetsReceived = new AtomicLong(0);
    private static volatile boolean initCalled = false;
    private static volatile boolean serverStartedCalled = false;
    private static volatile long firstPacketTs = 0L;

    @Override
    public String getPluginId() {
        return "liberthia_voice";
    }

    @Override
    public void initialize(VoicechatApi api) {
        LiberthiaVoicePlugin.api = api;
        initCalled = true;
        String url = null;
        try { url = br.com.murilo.liberthia.config.LiberthiaConfig.voiceBackendUrl(); }
        catch (Exception ignored) {}
        if (url == null || url.isBlank()) {
            LiberthiaMod.LOGGER.warn("[LiberthiaVoice] voice.backend_url vazio — clipes serão descartados. Configure em liberthia-server.toml [voice] backend_url");
        } else {
            LiberthiaMod.LOGGER.info("[LiberthiaVoice] plugin ativo, backend: {}", url);
        }
    }

    @Override
    public void registerEvents(EventRegistration registration) {
        registration.registerEvent(VoicechatServerStartedEvent.class, this::onServerStarted);
        registration.registerEvent(MicrophonePacketEvent.class, this::onMicPacket);
    }

    private void onServerStarted(VoicechatServerStartedEvent event) {
        serverApi = event.getVoicechat();
        serverStartedCalled = true;
    }

    /**
     * Hookado em cada packet de áudio que o player envia. Decodifica Opus → PCM
     * e acumula. Quando o player para de falar por {@link #UTTERANCE_END_SILENCE_MS},
     * o {@link VoiceTickWatcher} flusha o buffer pro backend.
     */
    private void onMicPacket(MicrophonePacketEvent event) {
        try {
            long count = packetsReceived.incrementAndGet();
            if (count == 1) {
                firstPacketTs = System.currentTimeMillis();
                LiberthiaMod.LOGGER.info("[LiberthiaVoice] primeira captura recebida — pipeline funcionando");
            }

            VoicechatConnection conn = event.getSenderConnection();
            if (conn == null || conn.getPlayer() == null) return;
            UUID uuid = conn.getPlayer().getUuid();
            MicrophonePacket packet = event.getPacket();
            byte[] opusData = packet.getOpusEncodedData();
            if (opusData == null || opusData.length == 0) return;

            OpusDecoder decoder = DECODERS.get(uuid);
            if (decoder == null) {
                VoicechatApi a = api != null ? api : event.getVoicechat();
                decoder = a.createDecoder();
                DECODERS.put(uuid, decoder);
            }

            short[] pcm = decoder.decode(opusData);
            if (pcm == null || pcm.length == 0) return;

            VoiceCaptureSession session = SESSIONS.computeIfAbsent(uuid,
                    u -> new VoiceCaptureSession(uuid));
            session.appendPcm(pcm);

            // v0.1.22 r28: relay via antena se speaker estiver perto de uma antena ativa
            try { AntennaVoiceRelay.relayMicPacket(event); }
            catch (Throwable t) {
                LiberthiaMod.LOGGER.debug("[AntennaVoice] relay erro: {}", t.toString());
            }
            // v0.1.22 r30: relay via Crown of Mass Possession (whisper direto na cabeça)
            try { CrownVoiceRelay.relayMicPacket(event); }
            catch (Throwable t) {
                LiberthiaMod.LOGGER.debug("[CrownVoice] relay erro: {}", t.toString());
            }
            // Walkie Talkie: rádio privado por código secreto
            try { WalkieTalkieRelay.relayMicPacket(event); }
            catch (Throwable t) {
                LiberthiaMod.LOGGER.debug("[WalkieTalkie] relay erro: {}", t.toString());
            }
        } catch (Throwable t) {
            // Pega QUALQUER erro pra não derrubar a thread de I/O do SVC
            LiberthiaMod.LOGGER.warn("[LiberthiaVoice] erro em onMicPacket: {}", t.toString(), t);
        }
    }

    /**
     * Chamado pelo {@link VoiceTickWatcher} cada 10 ticks. Verifica sessions
     * que ficaram inativas o suficiente pra fechar utterance.
     * Sem heartbeat — silencioso quando não tem nada pra fazer.
     */
    public static void tickInactivity() {
        if (SESSIONS.isEmpty()) return;
        long now = System.currentTimeMillis();
        for (var entry : SESSIONS.entrySet()) {
            VoiceCaptureSession s = entry.getValue();
            if (s.shouldFlushBySilence(now) || s.shouldFlushByMaxDuration(now)) {
                s.flushAndUpload();
            }
        }
    }

    public static VoicechatServerApi getServerApi() {
        return serverApi;
    }

    public static VoicechatApi getApi() {
        return api != null ? api : serverApi;
    }

    public static boolean isActive() {
        return initCalled && (api != null || serverApi != null);
    }

    /** Reseta a sessão de um player (logout). Flusha o que tinha + descarta decoder. */
    public static void resetSession(UUID uuid) {
        VoiceCaptureSession s = SESSIONS.remove(uuid);
        if (s != null) s.flushAndUpload();
        OpusDecoder d = DECODERS.remove(uuid);
        if (d != null) {
            try { d.close(); } catch (Exception ignored) {}
        }
        // v0.1.22 r28: limpa canais de relay de antena desse player
        try { AntennaVoiceRelay.cleanupSender(uuid); }
        catch (Throwable ignored) {}
        // v0.1.22 r30: limpa canais Crown desse player
        try { CrownVoiceRelay.cleanupSender(uuid); }
        catch (Throwable ignored) {}
        // Walkie Talkie: limpa canais de rádio (emissor ou receptor)
        try { WalkieTalkieRelay.cleanup(uuid); }
        catch (Throwable ignored) {}
    }

    /** Dump de estado pro comando de debug. */
    public static String statusDump() {
        StringBuilder sb = new StringBuilder();
        sb.append("§7§l═ LiberthiaVoice Status ═§r\n");
        sb.append(String.format("§7initialize() called: §%s%s§r\n",
                initCalled ? "a" : "c", initCalled));
        sb.append(String.format("§7VoicechatApi: §%s%s§r\n",
                api != null ? "a" : "c", api != null ? "OK" : "NULL"));
        sb.append(String.format("§7VoicechatServerStarted: §%s%s§r\n",
                serverStartedCalled ? "a" : "c", serverStartedCalled));
        sb.append(String.format("§7VoicechatServerApi: §%s%s§r\n",
                serverApi != null ? "a" : "c", serverApi != null ? "OK" : "NULL"));
        long pkts = packetsReceived.get();
        sb.append(String.format("§7Packets received: §%s%d§r\n",
                pkts > 0 ? "a" : "e", pkts));
        if (pkts > 0) {
            long secs = (System.currentTimeMillis() - firstPacketTs) / 1000;
            sb.append(String.format("§7First packet: §a%ds ago§r\n", secs));
        }
        sb.append(String.format("§7Active sessions: §b%d§r\n", SESSIONS.size()));
        sb.append(String.format("§7Active decoders: §b%d§r\n", DECODERS.size()));
        // Backend URL (chave pra entender se uploads estão acontecendo)
        String url = null;
        try { url = br.com.murilo.liberthia.config.LiberthiaConfig.voiceBackendUrl(); }
        catch (Exception ignored) {}
        if (url == null || url.isBlank()) {
            sb.append("§7Backend URL: §c§lNÃO CONFIGURADO§r §c(clipes descartados!)§r\n");
        } else {
            sb.append(String.format("§7Backend URL: §a%s§r\n", url));
        }
        return sb.toString();
    }
}
