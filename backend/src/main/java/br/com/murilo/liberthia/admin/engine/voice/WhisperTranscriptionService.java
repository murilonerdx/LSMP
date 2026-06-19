package br.com.murilo.liberthia.admin.engine.voice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.*;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Transcrição automática de voice clips usando whisper.cpp (instalado no
 * Dockerfile). Roda um scheduler que pega 1 clipe PENDING por vez e transcreve.
 *
 * Whisper.cpp foi escolhido em vez de OpenAI Whisper Python porque:
 *  - C++ nativo, ~50MB binary (vs 2GB do PyTorch)
 *  - Roda eficientemente em CPU
 *  - Mesmo modelo ggml-base é aceitável pra português
 *
 * Output do whisper-cli: imprime no stdout + arquivos .txt/.srt no output dir.
 * A gente parseia o stdout pra ter o texto direto.
 */
@Service
public class WhisperTranscriptionService {

    private static final Logger LOG = LoggerFactory.getLogger(WhisperTranscriptionService.class);

    @Value("${voice.storage-dir:/app/data/voice-clips}")
    private String storageDir;

    /**
     * Modelo padrão é large-v3-turbo (~1.5GB RAM, qualidade ~igual ao large-v3
     * mas 8× mais rápido). Pra pt-BR é o sweet spot — caça precisão sem
     * estourar CPU. Pode trocar via env WHISPER_MODEL_PATH se quiser usar
     * small/medium (menos RAM, menos preciso).
     */
    @Value("${whisper.model-path:/usr/local/share/ggml-large-v3-turbo.bin}")
    private String modelPath;

    @Value("${whisper.language:pt}")
    private String language;

    @Value("${whisper.enabled:true}")
    private boolean enabled;

    /**
     * Threads pra UMA inferência. DEFAULT REDUZIDO de 16→2 — em VPS com 4 cores
     * compartilhados (DB, mod proxy, ffmpeg), 4 threads do whisper subiam o
     * processo pra 205% CPU e a JVM ficava sem tempo, gerando 503 no Traefik.
     *
     * Recomendação:
     *  - VPS 2 cores:  threads=1, workers=1
     *  - VPS 4 cores:  threads=2, workers=1   ← default
     *  - VPS 8+ cores: threads=4, workers=1-2
     *
     * NÃO subir além de cores FÍSICOS — hyperthreading não ajuda whisper.cpp.
     */
    @Value("${whisper.threads:2}")
    private int threads;

    /**
     * Beam size — hipóteses paralelas que o decoder considera.
     * REDUZIDO de 5→3 — qualidade quase idêntica em pt-BR informal mas ~40%
     * menos compute por clipe. Pra qualidade máxima sobe pra 5 ou 8.
     */
    @Value("${whisper.beam-size:3}")
    private int beamSize;

    /**
     * Best-of — quantas variações tenta antes de escolher a melhor.
     * REDUZIDO de 5→3 pelo mesmo motivo.
     */
    @Value("${whisper.best-of:3}")
    private int bestOf;

    /**
     * Nice level do processo whisper-cli (0=normal, 19=mais baixo).
     * Default 15 — bem baixo. Isso GARANTE que se a JVM ou ffmpeg precisar de
     * CPU, eles têm prioridade. Whisper continua rodando, só cede quando
     * outras coisas precisam. SEM nice, whisper acelerado a 200% deixava o
     * Traefik sem CPU pra responder healthcheck.
     */
    @Value("${whisper.cpu-priority:15}")
    private int cpuPriority;

    /**
     * CPU hard-cap em % (0 = desabilitado, usa nice). Se >0, usa `cpulimit -l N`
     * pra capar HARD o whisper-cli em N% (somando todos cores).
     * Ex: cpuPercent=150 num servidor de 4 cores = no máximo 1.5 cores = 37.5%
     * do total. Precisa do pacote `cpulimit` no container (não instalado por
     * default — adicionar no Dockerfile se quiser usar).
     */
    @Value("${whisper.cpu-percent:0}")
    private int cpuPercent;

    /**
     * Quantos workers de transcrição rodam EM PARALELO. Cada worker carrega
     * o modelo (~1.6GB RAM) e processa 1 clipe por vez. Com workers=3, três
     * clipes transcrevem simultaneamente — 3× throughput.
     *
     * Custo de RAM: workers × 1.6GB. Com 3 workers ≈ 5GB só pra whisper-cli.
     * Default 3 — pra usuários com 32GB RAM. Pra VPS apertada, baixar pra 1.
     */
    @Value("${whisper.workers:1}")
    private int workers;

    /**
     * Initial prompt — guia o modelo dando contexto. Crucial pra capturar gírias
     * de servidor de MC, nomes próprios, e estilo informal de chat de voz.
     * Mantém em PT-BR pra reforçar o idioma e estilo do servidor.
     */
    @Value("${whisper.prompt:Conversa informal em português brasileiro entre jogadores no servidor Liberthia. Usam gírias, nomes próprios e palavrões.}")
    private String initialPrompt;

    private final VoiceClipService svc;
    private final WhisperRuntimeConfig runtimeConfig;

    /** Detector de hate speech opcional (Optional<> permite null se o bean
     *  não estiver no contexto, ex: testes). Spring injeta automaticamente
     *  se HateSpeechDetectorService existir. */
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private br.com.murilo.liberthia.admin.engine.hatespeech.HateSpeechDetectorService hateSpeechDetector;

    /**
     * Semaphore com N permits = N workers paralelos. Inicializado no
     * @PostConstruct depois que `workers` foi injetado (não dá pra usar
     * @Value direto em field initializer).
     */
    private java.util.concurrent.Semaphore workerSlots;

    /** Executor dedicado pros workers — daemon threads, sem matar JVM. */
    private java.util.concurrent.ExecutorService workerPool;

    /** Lock antigo — mantido só pra compatibilidade do método status(). */
    private final AtomicBoolean running = new AtomicBoolean(false);

    /** Quantos workers estão atualmente ocupados (pra stats). */
    private final java.util.concurrent.atomic.AtomicInteger activeWorkers =
            new java.util.concurrent.atomic.AtomicInteger(0);

    /** Stats de visibilidade — quantos ticks o scheduler rodou. */
    private volatile long ticksRun = 0L;
    private volatile long clipsProcessed = 0L;
    private volatile long lastTickTs = 0L;
    private volatile String lastTickInfo = "ainda não rodou";

    /**
     * v0.1.13: rastreio de processos whisper-cli em andamento por clipId.
     * Quando user clica "Pausar" na UI, killAllActive() chama destroyForcibly()
     * em cada Process imediato — sem esperar o tick (~5s) nem o worker terminar
     * o clipe atual (~50s).
     *
     * <p>Antes: pausar só impedia NOVOS pickups. Workers já rodando finalizavam
     * o clipe atual antes de parar. Usuário via transcrições continuarem por
     * ~50s depois do click e ficava confuso.
     */
    private final java.util.Map<Long, Process> activeProcesses =
            new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * v0.1.13: chamado por WhisperRuntimeConfig.update() quando enabled vai
     * pra false. Mata TODOS os whisper-cli em andamento imediatamente.
     * Os workers vão capturar a interrupção, marcar o clipe como FAILED e
     * liberar o slot pro próximo tick (que já vai ver paused e não pegar).
     */
    public int killAllActive() {
        int killed = 0;
        for (var entry : activeProcesses.entrySet()) {
            try {
                Process p = entry.getValue();
                if (p != null && p.isAlive()) {
                    p.destroyForcibly();
                    killed++;
                    LOG.warn("[Whisper] killed in-flight clip #{} (pause requested)", entry.getKey());
                }
            } catch (Exception ignored) {}
        }
        return killed;
    }

    public WhisperTranscriptionService(VoiceClipService svc, WhisperRuntimeConfig runtimeConfig) {
        this.svc = svc;
        this.runtimeConfig = runtimeConfig;
    }

    /**
     * Log explícito DEPOIS do bean ser criado — antes era silencioso e usuário
     * não sabia se o scheduler estava ativo. Mostra config + caminho do modelo.
     */
    @jakarta.annotation.PostConstruct
    public void onStartup() {
        // Init dos workers paralelos
        int w = Math.max(1, workers);
        this.workerSlots = new java.util.concurrent.Semaphore(w);
        this.workerPool = java.util.concurrent.Executors.newFixedThreadPool(w, r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            t.setName("whisper-worker-" + System.nanoTime() % 10000);
            return t;
        });

        LOG.info("[Whisper] ===== Service inicializado =====");
        LOG.info("[Whisper] enabled = {}", enabled);
        LOG.info("[Whisper] model   = {}", modelPath);
        LOG.info("[Whisper] lang    = {}", language);
        LOG.info("[Whisper] threads = {} (por worker)", threads);
        LOG.info("[Whisper] workers = {} (clipes em paralelo)", w);
        LOG.info("[Whisper] beam    = {} | best-of = {}", beamSize, bestOf);
        LOG.info("[Whisper] cpu     = nice {}{}", cpuPriority,
                cpuPercent > 0 ? " + cpulimit " + cpuPercent + "%" : "");
        LOG.info("[Whisper] prompt  = \"{}\"", initialPrompt.length() > 60 ? initialPrompt.substring(0, 60) + "..." : initialPrompt);
        LOG.info("[Whisper] tick a cada 5s, initialDelay 10s. Aguardando clipes PENDING...");

        // Verificação de sanidade no boot — se whisper-cli ou modelo não existir,
        // já avisa LOGO em vez de só descobrir no primeiro tick.
        boolean cliOk = Files.exists(Paths.get("/usr/local/bin/whisper-cli"));
        boolean modelOk = Files.exists(Paths.get(modelPath));
        if (!cliOk) LOG.error("[Whisper] *** whisper-cli NÃO encontrado em /usr/local/bin/whisper-cli ***");
        if (!modelOk) LOG.error("[Whisper] *** modelo NÃO encontrado em {} ***", modelPath);
        if (cliOk && modelOk) {
            try {
                long modelSize = Files.size(Paths.get(modelPath));
                LOG.info("[Whisper] modelo OK ({} MB) ✓", modelSize / (1024 * 1024));
            } catch (Exception ignored) {}
        }
        if (!enabled) LOG.warn("[Whisper] DESATIVADO via env WHISPER_ENABLED=false");

        // BUG FIX (v90): clipes que ficaram em PROCESSING quando o backend
        // morreu/restartou nunca são reprocessados porque findPendingTranscription
        // só procura PENDING ou NULL. Resultado: a fila aparenta "andar" mas
        // alguns clipes ficam órfãos pra sempre.
        //
        // Solução: no boot, reseta TODOS os PROCESSING pra PENDING. Como o
        // serviço acabou de subir, não tem worker rodando ainda — é seguro.
        try {
            int reset = svc.resetStuckProcessing();
            if (reset > 0) {
                LOG.warn("[Whisper] {} clipe(s) em PROCESSING órfãos resetados pra PENDING (provável reinício durante transcrição)", reset);
            }
        } catch (Exception e) {
            LOG.warn("[Whisper] falha ao resetar PROCESSING órfãos: {}", e.getMessage());
        }
    }

    /**
     * Tick paralelo (a cada 5min) que reseta clipes em PROCESSING há mais de
     * 15min. Cobre casos onde whisper-cli hangou + waitFor não pegou (deveria
     * ser raro, mas vai que). Não substitui o reset on-startup — é safety net.
     */
    @Scheduled(fixedDelay = 5 * 60_000, initialDelay = 60_000)
    public void cleanupStaleProcessing() {
        if (!enabled) return;
        try {
            int reset = svc.resetStuckProcessingOlderThan(15 * 60_000); // 15 min
            if (reset > 0) {
                LOG.warn("[Whisper] cleanup periódico: {} clipe(s) presos em PROCESSING há >15min foram resetados pra PENDING", reset);
            }
        } catch (Exception ignored) {}
    }

    /**
     * Tick a cada 30 min: limpa orphan DB rows (sem WAV no disco) que causam
     * 404 quando user dá play. Isso acontece quando:
     *  - Mod faz reserve mas falha em uploadar o WAV (rede caiu)
     *  - Volume Docker remontado e perdeu arquivos
     *  - Cleanup deletou arquivo mas row sobreviveu
     */
    @Scheduled(fixedDelay = 30 * 60_000, initialDelay = 120_000)
    public void cleanupOrphanRows() {
        try {
            int removed = svc.cleanupOrphanClips();
            if (removed > 0) {
                LOG.info("[Whisper] cleanup orphan: {} rows sem arquivo deletados", removed);
            }
        } catch (Exception ignored) {}
    }

    /**
     * Tick a cada 5s — tenta agarrar UM permit do workerSlots; se conseguir,
     * pega o próximo clipe PENDING e dispara worker async. A tick retorna
     * imediatamente, permitindo múltiplos workers em paralelo.
     */
    @Scheduled(fixedDelay = 5_000, initialDelay = 10_000)
    public void tick() {
        ticksRun++;
        lastTickTs = System.currentTimeMillis();

        // CONFIG RUNTIME — lê do banco a cada tick (cache 5s internamente).
        // Permite habilitar/desabilitar e mudar parâmetros sem restart.
        // Se config do DB diz "enabled=false" OU está fora do schedule, pula tick.
        WhisperRuntimeConfig.Snapshot cfg = runtimeConfig.get();
        if (!cfg.shouldProcessNow()) {
            lastTickInfo = "paused (" + cfg.pausedReason() + ")";
            if (ticksRun % 60 == 1) {
                LOG.info("[Whisper] tick #{} — pausado: {}", ticksRun, cfg.pausedReason());
            }
            return;
        }

        // @Value enabled é o "kill switch" no env — config do DB pode dar override
        // pra true só se @Value também for true. Se @Value=false significa que
        // o operador FORÇOU disabled via env (ex: deploy de emergência),
        // não deixar DB sobrescrever.
        if (!enabled) {
            lastTickInfo = "disabled (env override)";
            if (ticksRun % 60 == 1) LOG.info("[Whisper] tick #{} — disabled via WHISPER_ENABLED=false env", ticksRun);
            return;
        }

        // Pausa enquanto vídeo está renderizando — ffmpeg precisa de TODA RAM
        // disponível e workers do whisper carregando modelo de 1.6GB cada
        // estouravam o container (exit=137 OOM).
        if (br.com.murilo.liberthia.admin.engine.videoeditor.VideoRenderMutex.isRunning()) {
            lastTickInfo = "paused (video render running)";
            if (ticksRun % 12 == 1) LOG.info("[Whisper] tick #{} pausado — video render em andamento", ticksRun);
            return;
        }

        // Tenta agarrar um worker. Se todos N estão ocupados, pula esse tick.
        if (!workerSlots.tryAcquire()) {
            lastTickInfo = "all " + workers + " workers busy";
            return;
        }

        VoiceClip clip = null;
        try {
            // Sanity checks
            if (!Files.exists(Paths.get("/usr/local/bin/whisper-cli"))) {
                LOG.error("[Whisper] whisper-cli não encontrado em runtime — desativando");
                enabled = false;
                lastTickInfo = "whisper-cli missing";
                workerSlots.release();
                return;
            }
            if (!Files.exists(Paths.get(modelPath))) {
                LOG.error("[Whisper] modelo não encontrado em {} — desativando", modelPath);
                enabled = false;
                lastTickInfo = "model missing";
                workerSlots.release();
                return;
            }

            List<VoiceClip> pending = svc.findPendingTranscription(1);
            if (pending.isEmpty()) {
                lastTickInfo = "idle (0 PENDING)";
                if (ticksRun % 60 == 1) {
                    LOG.info("[Whisper] tick #{} idle — 0 clipes PENDING. Scheduler vivo ✓", ticksRun);
                }
                workerSlots.release();
                return;
            }
            clip = pending.get(0);

            // CRÍTICO: marca PROCESSING JÁ AQUI (síncrono), antes do worker
            // disparar. Senão, próximo tick (em 5s) pode pegar o MESMO clipe.
            svc.setTranscriptionStatus(clip.getId(), "PROCESSING");

            final VoiceClip finalClip = clip;
            workerPool.submit(() -> {
                activeWorkers.incrementAndGet();
                try {
                    lastTickInfo = "transcribing clip " + finalClip.getId();
                    LOG.info("[Whisper] worker pegou clipe #{} ({}ms, {} bytes) — {}/{} workers ativos",
                            finalClip.getId(), finalClip.getDurationMs(), finalClip.getSizeBytes(),
                            activeWorkers.get(), workers);
                    transcribeOne(finalClip);
                    clipsProcessed++;
                } catch (Exception e) {
                    LOG.warn("[Whisper] worker clip #{} exception: {}", finalClip.getId(), e.getMessage(), e);
                } finally {
                    activeWorkers.decrementAndGet();
                    workerSlots.release();
                }
            });
        } catch (Exception e) {
            lastTickInfo = "tick exception: " + e.getMessage();
            LOG.warn("[Whisper] tick exception: {}", e.getMessage(), e);
            workerSlots.release(); // não vaza permit
        }
    }

    /** Status do scheduler — pra exposição via endpoint /api/voice/whisper/status. */
    public java.util.Map<String, Object> status() {
        WhisperRuntimeConfig.Snapshot cfg = runtimeConfig.get();
        // Linkedhashmap pra ordem estável no JSON do response
        java.util.LinkedHashMap<String, Object> out = new java.util.LinkedHashMap<>();
        out.put("enabled", enabled);                          // env @Value (kill switch)
        out.put("modelPath", modelPath);
        out.put("language", language);
        // Valores EFETIVOS — o que está sendo usado nas chamadas whisper-cli
        out.put("threads", cfg.threads > 0 ? cfg.threads : threads);
        out.put("beamSize", cfg.beamSize > 0 ? cfg.beamSize : beamSize);
        out.put("bestOf", cfg.bestOf > 0 ? cfg.bestOf : bestOf);
        out.put("cpuPriority", cfg.cpuPriority >= 0 ? cfg.cpuPriority : cpuPriority);
        out.put("workers", workers);
        out.put("activeWorkers", activeWorkers.get());
        out.put("ticksRun", ticksRun);
        out.put("clipsProcessed", clipsProcessed);
        out.put("lastTickTs", lastTickTs);
        out.put("lastTickInfo", lastTickInfo);
        out.put("currentlyRunning", activeWorkers.get() > 0);
        // Runtime config inteiro pra UI mostrar editor
        out.put("runtimeConfig", cfg.toMap());
        return out;
    }

    /** Transcreve 1 clipe específico (pode ser chamado manualmente via /retry). */
    @Transactional
    public void transcribeOne(VoiceClip clip) {
        if (clip.getFilePath() == null) return;
        Path file = Paths.get(storageDir, clip.getFilePath());
        if (!Files.exists(file)) {
            svc.setTranscription(clip.getId(), null, "FAILED", "arquivo não existe");
            return;
        }

        svc.setTranscriptionStatus(clip.getId(), "PROCESSING");
        long start = System.currentTimeMillis();

        try {
            // whisper-cli aceita WAV 16-bit mono direto (qualquer sample rate
            // — internamente downsampla pra 16kHz). Nossos clipes são 48kHz/
            // 16-bit/mono — formato perfeito.
            //
            // Flags otimizadas pra MÁXIMA qualidade:
            //  -bo / --best-of:     N variações por decode (default 5, usamos 8)
            //  -bs / --beam-size:   N hipóteses paralelas (default 5, usamos 8)
            //  --temperature 0:     determinístico, sem fallback aleatório
            //  --no-speech-thold:   0.4 (default 0.6) — captura mais speech
            //  --word-thold:        0.01 — não corta tokens incertos
            //  --prompt:            contexto pra modelo entender gírias/nomes
            //  --entropy-thold:     2.4 — fallback se confusão semântica alta
            //  --logprob-thold:     -1.0 — fallback se prob baixa
            // NOTA SOBRE FLAGS DE OUTPUT:
            // Antes usávamos "--output-txt --output-file <wav>.whisper" mas isso
            // tentava escrever o .txt dentro do volume Docker `lsmp_voice_data`,
            // que é montado com owner root. O whisper-cli (rodando como
            // `liberthia`) falhava com "open: failed to open '...' for writing"
            // e essa mensagem vazava no stdout sendo CONCATENADA na transcrição
            // (porque pb.redirectErrorStream(true) junta stderr+stdout).
            //
            // Sintoma exato visto na UI:
            //   "Espero que você goste, pipoca-deus. open: failed to open
            //    '/app/data/voice-clips/2026-05/27300.wav.whisper.txt' for writing"
            //
            // Fix: não usar --output-txt. A transcrição já vem no stdout normal
            // do whisper-cli. Sem arquivo intermediário, sem race condition.
            // Monta o comando: opcionalmente prefixado com `nice` (prioridade
             // baixa, cede CPU pra JVM) e/ou `cpulimit` (hard-cap em %).
            //
            // Sem isso, whisper-cli com -t=4 fica em 205% CPU (4 cores × ~50%),
            // a JVM/Traefik ficam sem ciclos, e o healthcheck retorna 503 →
            // Swarm marca o container unhealthy → restart loop.
            //
            // `nice` é nativo do Linux (debian-base do temurin tem). `cpulimit`
            // não vem por padrão — precisa instalar no Dockerfile se cpuPercent>0.
            // Lê config RUNTIME do banco — permite usuário mudar threads/beam/bestOf
            // em tempo real via UI. Cache interno de 5s evita martelar o DB.
            // Fallback nos @Value se DB não tiver nada.
            WhisperRuntimeConfig.Snapshot rcfg = runtimeConfig.get();
            int effThreads = rcfg.threads > 0 ? rcfg.threads : threads;
            int effBeam    = rcfg.beamSize > 0 ? rcfg.beamSize : beamSize;
            int effBestOf  = rcfg.bestOf > 0 ? rcfg.bestOf : bestOf;
            int effNice    = rcfg.cpuPriority >= 0 ? rcfg.cpuPriority : cpuPriority;

            java.util.List<String> cmd = new java.util.ArrayList<>();
            if (cpuPercent > 0) {
                // cpulimit -l <percent> -- <cmd>
                cmd.add("cpulimit");
                cmd.add("-l");
                cmd.add(String.valueOf(cpuPercent));
                cmd.add("--");
            }
            if (effNice > 0) {
                cmd.add("nice");
                cmd.add("-n");
                cmd.add(String.valueOf(Math.min(19, effNice)));
            }
            cmd.add("whisper-cli");
            cmd.add("-m"); cmd.add(modelPath);
            cmd.add("-f"); cmd.add(file.toAbsolutePath().toString());
            cmd.add("-l"); cmd.add(language);
            cmd.add("-t"); cmd.add(String.valueOf(Math.max(1, effThreads)));
            cmd.add("-bo"); cmd.add(String.valueOf(Math.max(1, effBestOf)));
            cmd.add("-bs"); cmd.add(String.valueOf(Math.max(1, effBeam)));
            cmd.add("--temperature"); cmd.add("0");
            cmd.add("--no-speech-thold"); cmd.add("0.4");
            cmd.add("--word-thold"); cmd.add("0.01");
            cmd.add("--entropy-thold"); cmd.add("2.4");
            cmd.add("--logprob-thold"); cmd.add("-1.0");
            cmd.add("--prompt"); cmd.add(initialPrompt);
            cmd.add("--no-prints");
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            // v0.1.13: registra o Process pra permitir kill via /api/voice/whisper/pause
            activeProcesses.put(clip.getId(), p);

            StringBuilder stdout = new StringBuilder();
            // Lê stdout em thread separada — caso whisper hang, conseguimos kill.
            // Antes lia em loop síncrono e podia bloquear o tick scheduler eterno.
            Thread reader = new Thread(() -> {
                try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                    String line;
                    while ((line = r.readLine()) != null) {
                        synchronized (stdout) {
                            if (stdout.length() < 16000) stdout.append(line).append("\n");
                        }
                    }
                } catch (Exception ignored) {}
            }, "whisper-reader-" + clip.getId());
            reader.setDaemon(true);
            reader.start();

            // Timeout duro de 5 min por clipe. Whisper large-v3-turbo em CPU
            // raramente passa de 2min mesmo pra clipe de 60s; 5min é margem.
            // Sem isso, hang silencioso quebra TODA a fila (running.compareAndSet
            // trava em true).
            boolean finished = p.waitFor(5, java.util.concurrent.TimeUnit.MINUTES);
            if (!finished) {
                p.destroyForcibly();
                reader.interrupt();
                svc.setTranscription(clip.getId(), null, "FAILED",
                        "whisper-cli timeout (>5min) — clipe pode estar corrompido ou modelo travou");
                LOG.error("[Whisper] clip {} TIMEOUT após 5min — kill forçado", clip.getId());
                return;
            }
            int exit = p.exitValue();
            long elapsed = System.currentTimeMillis() - start;

            if (exit != 0) {
                String err = stdout.toString();
                svc.setTranscription(clip.getId(), null, "FAILED",
                        "whisper-cli exit=" + exit + " | " + (err.length() > 400 ? err.substring(err.length() - 400) : err));
                LOG.warn("[Whisper] clip {} falhou (exit={}, {}ms): {}", clip.getId(), exit, elapsed, err);
                return;
            }

            // Texto vem do stdout (sem mais fallback de .txt — vide nota acima).
            String text = extractTextFromStdout(stdout.toString());

            // Limpeza defensiva: se ficou algum lixo de erro de IO sobrando do
            // .whisper.txt antigo de versões anteriores (clipes processados em
            // v85/v86 onde a flag --output-txt ainda existia), remove os
            // arquivos órfãos.
            try {
                Path orphan = Paths.get(file.toAbsolutePath() + ".whisper.txt");
                if (Files.exists(orphan)) Files.deleteIfExists(orphan);
            } catch (Exception ignored) {}
            if (text.isBlank()) {
                svc.setTranscription(clip.getId(), "", "DONE", null);
                LOG.info("[Whisper] clip {} transcrito vazio (silêncio?) em {}ms", clip.getId(), elapsed);
            } else {
                svc.setTranscription(clip.getId(), text, "DONE", null);
                LOG.info("[Whisper] clip {} transcrito em {}ms: \"{}\"",
                        clip.getId(), elapsed,
                        text.length() > 80 ? text.substring(0, 80) + "..." : text);

                // v0.1.23 — hook de detecção de hate speech. Async (quickScan
                // é ~µs; deepAnalyze via Ollama é ~2s mas roda em thread separada
                // dentro do detector). Não bloqueia o próximo clip da fila.
                if (hateSpeechDetector != null) {
                    try {
                        hateSpeechDetector.analyze(clip, text);
                    } catch (Exception e) {
                        LOG.warn("[Whisper] hate-speech detector falhou pro clip {}: {}",
                                clip.getId(), e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            svc.setTranscription(clip.getId(), null, "FAILED", e.getMessage());
            LOG.warn("[Whisper] clip {} exception: {}", clip.getId(), e.getMessage());
        } finally {
            // v0.1.13: sempre limpa o registro do Process, mesmo se deu erro/kill
            activeProcesses.remove(clip.getId());
        }
    }

    /**
     * O whisper-cli imprime no formato "[timestamp]  texto" por linha.
     * Junta tudo numa string limpa.
     *
     * Filtros aplicados:
     *  - Remove linhas de log do whisper-cli (whisper_, main:, output_txt:,
     *    output_file:, system_info, ggml_, etc) que vazam mesmo com --no-prints
     *  - Remove timestamps [00:00.000 --> 00:01.500]
     *  - Filtra HALLUCINATIONS conhecidas — Whisper inventa textos em silêncio
     *    ou áudio quase-mudo. As mais comuns são citações de legendadores
     *    do YouTube/Netflix que estavam no dataset de treino.
     */
    private String extractTextFromStdout(String stdout) {
        StringBuilder sb = new StringBuilder();
        for (String line : stdout.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;
            // Remove timestamps no formato [00:00.000 --> 00:01.500]
            int closeBracket = trimmed.indexOf(']');
            if (trimmed.startsWith("[") && closeBracket > 0 && closeBracket < trimmed.length() - 1) {
                trimmed = trimmed.substring(closeBracket + 1).trim();
            }
            // Skip linhas de log do whisper. Expandido: output_txt:, output_file:,
            // output_srt:, etc — mesmo com --no-prints whisper-cli imprime essas.
            if (isWhisperLogLine(trimmed)) continue;
            // Filtro defensivo: se whisper-cli mandar mensagem de erro pro
            // stdout (porque o stderr foi mergeado via redirectErrorStream),
            // remove a parte do erro mas preserva o texto antes/depois.
            // Ex: "Espero que você goste. open: failed to open '...' for writing"
            //   → "Espero que você goste."
            trimmed = stripIoErrorFragments(trimmed);
            if (trimmed.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(trimmed);
        }
        return removeHallucinations(sb.toString().trim());
    }

    /**
     * Tira fragmentos de erro de IO do whisper-cli que vazaram pro meio do
     * texto. Padrões observados em produção (v85-v87):
     *   "open: failed to open '/app/data/voice-clips/.../X.wav.whisper.txt' for writing"
     *   "main: error: ..."
     *   "ggml_aligned_malloc: insufficient memory ..."
     */
    private String stripIoErrorFragments(String s) {
        if (s == null || s.isEmpty()) return s;
        // Pattern 1: "open: failed to open ... for writing" (com aspas opcionais)
        int openIdx = s.indexOf("open: failed to open");
        if (openIdx >= 0) {
            // Pega texto antes do erro
            String before = s.substring(0, openIdx).trim();
            // Procura "for writing" e descarta tudo até lá
            int afterIdx = s.indexOf("for writing", openIdx);
            if (afterIdx >= 0) {
                String after = s.substring(afterIdx + "for writing".length()).trim();
                s = (before + " " + after).trim();
            } else {
                s = before;
            }
        }
        return s;
    }

    private boolean isWhisperLogLine(String s) {
        // Prefixos comuns que vazam mesmo com --no-prints
        String[] PREFIXES = {
                "whisper_", "main:", "system_info", "output_txt:", "output_file:",
                "output_srt:", "output_vtt:", "output_lrc:", "output_json:",
                "output_csv:", "output_words:", "Saving output", "saving output",
                "load time", "fallbacks", "mel time", "sample time",
                "encode time", "decode time", "batchd time", "prompt time",
                "total time", "model loaded"
        };
        for (String p : PREFIXES) if (s.startsWith(p)) return true;
        if (s.contains("ggml_")) return true;
        return false;
    }

    /**
     * Whisper alucina textos conhecidos em silêncio/ruído. Estes vêm do
     * dataset de treino (legendas de YouTube/Netflix). Filtramos pra não
     * poluir a UI com texto falso.
     *
     * Lista coletada de:
     *  - https://github.com/openai/whisper/discussions/928
     *  - relatos da comunidade
     */
    private String removeHallucinations(String text) {
        if (text == null || text.isBlank()) return text;
        String[] HALLUCINATIONS = {
                "Legendas pela comunidade Amara.org",
                "Legendado pela comunidade Amara.org",
                "Legenda por Sônia Ruberti",
                "Legendas por Sônia Ruberti",
                "Sub by",
                "subtitles by",
                "Obrigado por assistir",
                "Obrigado por ver o vídeo",
                "Inscreva-se no canal",
                "Curta e se inscreva",
                "Subscribe to the channel",
                "Thank you for watching",
                "Thanks for watching",
                "Tradução e Revisão",
                "Tradução:",
                "Revisão:",
                "Transcrição:",
        };
        for (String h : HALLUCINATIONS) {
            // Substitui case-insensitive
            text = text.replaceAll("(?i)" + java.util.regex.Pattern.quote(h) + "[^.!?]*[.!?]?", "").trim();
        }
        // Se sobrou apenas pontuação ou ficou muito curto = era só hallucination
        text = text.replaceAll("\\s+", " ").trim();
        if (text.length() < 3) return "";
        return text;
    }
}
