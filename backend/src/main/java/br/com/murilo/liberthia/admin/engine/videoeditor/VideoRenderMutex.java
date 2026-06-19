package br.com.murilo.liberthia.admin.engine.videoeditor;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Flag global compartilhada — true enquanto um render de vídeo (ffmpeg pesado)
 * está rodando. WhisperTranscriptionService consulta antes de iniciar workers
 * pra não competir por memória.
 *
 * Sem isso: 3 whisper workers (1.6GB cada) + ffmpeg + JVM facilmente estouram
 * o limit de 6GB do container e o OOM killer derruba processos aleatórios.
 *
 * Implementação trivial — AtomicBoolean estático. Não precisa de Spring bean
 * porque é um latch global e cross-package (whisper vê videoeditor).
 */
public class VideoRenderMutex {

    private static final AtomicBoolean running = new AtomicBoolean(false);

    public static void start() { running.set(true); }
    public static void stop()  { running.set(false); }
    public static boolean isRunning() { return running.get(); }

    private VideoRenderMutex() {}
}
