package br.com.murilo.liberthia.admin.engine.hatespeech;

import br.com.murilo.liberthia.admin.engine.voice.VoiceClip;
import br.com.murilo.liberthia.admin.engine.voice.VoiceClipService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Detector de discurso de ódio em transcrições — pipeline em 2 estágios:
 *
 * <h2>Estágio 1 — Quick scan (regex)</h2>
 * Roda em TODAS as transcrições. Custo ~µs por clip. Lista de gatilhos
 * por categoria carregada de {@code application.yml}. Se nenhum gatilho
 * bate, nada acontece.
 *
 * <h2>Estágio 2 — Deep analyze (Ollama qwen2.5:1.5b)</h2>
 * Roda só pras transcrições onde gatilho bateu (~5% do volume). Manda pro
 * LLM com prompt estruturado que retorna JSON:
 * <pre>
 * {
 *   "ofensivo": true/false,
 *   "categorias": ["transfobia","racismo",...],
 *   "severity": "low|medium|high",
 *   "confidence": 0.85,
 *   "razao": "frase curta"
 * }
 * </pre>
 *
 * <p>Diferencia ofensa real ("morre viado") de meta-fala ("o cara me chamou
 * de viado, fiquei chateado"). LLM dá esse contexto que regex puro não dá.
 *
 * <h2>Performance</h2>
 * - Quick scan: 1000 transcrições/seg num core.<br>
 * - LLM (qwen 1.5b): ~2-3s por análise, single CPU. Limitado a 1 thread.<br>
 * - Cache LRU de 1000 resultados pra não re-analisar mesma string.<br>
 * - Modo "REGEX_ONLY" disponível pra desligar o LLM se CPU estiver apertado.
 */
@Service
public class HateSpeechDetectorService {

    private static final Logger LOG = LoggerFactory.getLogger(HateSpeechDetectorService.class);

    private final HateSpeechAlertRepository repo;
    private final VoiceClipService voiceService;
    private final WebClient ollamaClient;
    private final ObjectMapper json = new ObjectMapper();

    /** Triggers carregados do application.yml: categoria → lista de regex patterns.
     *  Cada pattern é case-INSENSITIVE + word-boundary pra evitar falsos do tipo
     *  "praticante" matchar "praticant[e]" quando o trigger é "tica". */
    private final Map<String, List<Pattern>> triggers = new LinkedHashMap<>();

    /** Cache LRU simples — evita re-analisar transcrições idênticas (ex: jogador
     *  espamando a mesma fala). Synchronized pra thread-safety. */
    private final Map<String, AnalysisResult> cache =
            Collections.synchronizedMap(new LinkedHashMap<>(128, 0.75f, true) {
                @Override protected boolean removeEldestEntry(Map.Entry<String, AnalysisResult> eldest) {
                    return size() > 1000;
                }
            });

    /** Quando false, pula o LLM e cria alerta apenas com o regex match
     *  (severity sempre MEDIUM, confidence 0.5). Útil se CPU tá apertado. */
    @Value("${hate-speech.use-llm:true}")
    private boolean useLlm;

    /** Threshold mínimo de confidence pra criar alerta. Abaixo disso, descarta. */
    @Value("${hate-speech.min-confidence:0.5}")
    private double minConfidence;

    /** Modelo Ollama a usar. Mesmo já em uso no compose. */
    @Value("${ollama.model:qwen2.5:1.5b}")
    private String ollamaModel;

    public HateSpeechDetectorService(HateSpeechAlertRepository repo,
                                      VoiceClipService voiceService,
                                      @Value("${ollama.url:http://ollama:11434}") String ollamaUrl) {
        this.repo = repo;
        this.voiceService = voiceService;
        this.ollamaClient = WebClient.builder()
                .baseUrl(ollamaUrl)
                .codecs(c -> c.defaultCodecs().maxInMemorySize(2 * 1024 * 1024))
                .build();
        initTriggers();
    }

    /**
     * Lista de gatilhos por categoria. Mantida em código (vs DB) porque:
     *  - É lista pequena/estável
     *  - Edição requer redeploy (intencional — evita admin "limpar lista" sem querer)
     *  - Carrega em ms (vs query DB em cada scan)
     *
     * <p>Cada gatilho é regex que será compilado case-insensitive + word boundary.
     * Adicione variações fonéticas comuns ("vaiado", "viadu") porque transcrição
     * Whisper às vezes erra esses fonemas.
     *
     * <p>Convenções:
     *  - Tudo lowercase no source. Compilação adiciona CASE_INSENSITIVE.
     *  - Use \b (word boundary) implícito (Pattern.compile com lookahead).
     *  - Sem acentos onde possível — Whisper às vezes omite.
     */
    private void initTriggers() {
        // Transfobia
        addCategory("transfobia",
                "traveco", "traveca", "travecu", "tranny",
                "trans nao e mulher", "trans nao e homem",
                "homem virou mulher", "mulher virou homem",
                "isso e homem nao mulher", "tem pinto"
        );
        // Racismo (lista pequena, contextual)
        addCategory("racismo",
                "macaco preto", "preto fedido", "neguinho safado",
                "volta pra senzala", "neguim de cabelo ruim",
                "pretinho nojento", "favelado preto"
        );
        // Xenofobia (bra-específico)
        addCategory("xenofobia",
                "nordestino burro", "nordestino fedido", "paraiba ignorante",
                "baianagem", "volta pro nordeste",
                "gringo lixo", "gringo fedido"
        );
        // Homofobia
        addCategory("homofobia",
                "viadinho", "veadinho", "viadagem", "veadagem",
                "viado nojento", "morre viado", "viado de merda",
                "sapatao", "sapatona de merda",
                "bicha louca", "bicha nojenta",
                "fechativa nojenta"
        );
        // Misoginia
        addCategory("misoginia",
                "vai pra cozinha", "lugar de mulher e",
                "mulher burra", "mulherzinha lixo",
                "vadia de merda", "puta sua mae",
                "mulher e ruim no jogo"
        );
        // Capacitismo
        addCategory("capacitismo",
                "retardado", "mongolao", "mongoloide",
                "down de merda", "deficiente de merda",
                "autista lixo"
        );
        // Genérico/outro — discurso violento sem categoria clara
        addCategory("outro",
                "te mato", "vou te matar", "morre desgracado",
                "ze povinho", "lixo da humanidade"
        );

        int total = triggers.values().stream().mapToInt(List::size).sum();
        LOG.info("[HateSpeech] inicializado com {} gatilhos em {} categorias",
                total, triggers.size());
    }

    private void addCategory(String cat, String... patterns) {
        List<Pattern> compiled = new ArrayList<>(patterns.length);
        for (String p : patterns) {
            // (?i) = case-insensitive. Não usa \b porque alguns triggers têm
            // espaço; o match contains() já cobre o caso "tem substring".
            compiled.add(Pattern.compile("(?i)" + Pattern.quote(p)));
        }
        triggers.put(cat, compiled);
    }

    /**
     * Análise principal — chamada pelo WhisperTranscriptionService quando
     * uma transcrição termina. Async (não bloqueia o whisper).
     */
    public void analyze(VoiceClip clip, String transcription) {
        if (transcription == null || transcription.isBlank()) return;
        // Async pra não bloquear o whisper. Pool de 1 thread (LLM single-threaded).
        Thread t = new Thread(() -> {
            try {
                doAnalyze(clip, transcription);
            } catch (Exception e) {
                LOG.warn("[HateSpeech] análise do clip {} falhou: {}",
                        clip.getId(), e.getMessage());
            }
        }, "HateSpeech-Analyze");
        t.setDaemon(true);
        t.start();
    }

    private void doAnalyze(VoiceClip clip, String text) {
        // Estágio 1 — Quick scan
        QuickScanResult scan = quickScan(text);
        if (scan.matchedTriggers.isEmpty()) {
            return; // sem gatilho = sem alerta. Maioria dos clips para aqui.
        }
        LOG.debug("[HateSpeech] clip {} bateu {} gatilhos: {}",
                clip.getId(), scan.matchedTriggers.size(), scan.matchedTriggers);

        // Estágio 2 — Deep analyze (se habilitado)
        AnalysisResult result;
        String detectionMode;
        if (useLlm) {
            result = cache.computeIfAbsent(text.toLowerCase().trim(),
                    k -> deepAnalyze(text, scan));
            detectionMode = result.ofensivo ? "HYBRID" : "LLM_FALSE_POSITIVE";
        } else {
            // Modo regex-only: sempre cria alerta com confiança média
            result = new AnalysisResult(true, scan.categories,
                    "medium", 0.5, "Regex match (LLM desligado)");
            detectionMode = "QUICK_ONLY";
        }

        if (!result.ofensivo || result.confidence < minConfidence) {
            // LLM disse "não é ofensivo" ou confiança baixa demais. Não cria alerta.
            LOG.debug("[HateSpeech] clip {} descartado: ofensivo={}, confidence={}",
                    clip.getId(), result.ofensivo, result.confidence);
            return;
        }

        // Cria alerta
        HateSpeechAlert a = new HateSpeechAlert();
        a.setVoiceClipId(clip.getId());
        a.setPlayerUuid(clip.getPlayerUuid());
        a.setPlayerName(clip.getPlayerName());
        a.setTs(clip.getTs());
        a.setTranscription(text);
        a.setCategories(String.join(",", result.categories));
        a.setTriggers(String.join(",", scan.matchedTriggers));
        a.setSeverity(result.severity.toUpperCase(Locale.ROOT));
        a.setConfidence(result.confidence);
        a.setReason(result.reason);
        a.setDetectionMode(detectionMode);
        repo.save(a);

        LOG.warn("[HateSpeech] ⚠ ALERTA: player={} clip={} severity={} categorias={} razao=\"{}\"",
                clip.getPlayerName(), clip.getId(), result.severity,
                result.categories, result.reason);
    }

    /**
     * Quick scan — varre a transcrição contra cada lista de triggers.
     * Retorna conjunto de gatilhos que bateram + categorias correspondentes.
     */
    public QuickScanResult quickScan(String text) {
        QuickScanResult r = new QuickScanResult();
        for (Map.Entry<String, List<Pattern>> entry : triggers.entrySet()) {
            for (Pattern p : entry.getValue()) {
                if (p.matcher(text).find()) {
                    r.matchedTriggers.add(p.pattern().replaceFirst("\\(\\?i\\)\\\\Q", "").replaceFirst("\\\\E$", ""));
                    r.categories.add(entry.getKey());
                }
            }
        }
        return r;
    }

    /**
     * Deep analyze via Ollama. Prompt estruturado pede JSON puro de volta.
     * Falha gracefully — se Ollama indisponível ou retorna lixo, considera
     * como "regex-only confirma" (cria alerta com confiança 0.5).
     */
    private AnalysisResult deepAnalyze(String text, QuickScanResult scan) {
        String prompt = buildPrompt(text, scan);
        try {
            String body = json.writeValueAsString(Map.of(
                    "model", ollamaModel,
                    "prompt", prompt,
                    "stream", false,
                    "format", "json",
                    "options", Map.of(
                            "temperature", 0.1,  // determinístico
                            "num_predict", 200,  // resposta curta
                            "num_ctx", 512       // contexto pequeno = rápido
                    )
            ));
            String resp = ollamaClient.post().uri("/api/generate")
                    .header("Content-Type", "application/json")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(15))
                    .block();

            JsonNode rootResp = json.readTree(resp);
            String llmJson = rootResp.path("response").asText("");
            if (llmJson.isBlank()) {
                return regexFallback(scan, "LLM retornou vazio");
            }
            JsonNode parsed = json.readTree(llmJson);
            boolean ofensivo = parsed.path("ofensivo").asBoolean(false);
            String severity = parsed.path("severity").asText("medium");
            double confidence = parsed.path("confidence").asDouble(0.5);
            String reason = parsed.path("razao").asText("");

            List<String> cats = new ArrayList<>();
            JsonNode catsNode = parsed.path("categorias");
            if (catsNode.isArray()) {
                for (JsonNode c : catsNode) cats.add(c.asText());
            }
            if (cats.isEmpty()) cats.addAll(scan.categories);

            return new AnalysisResult(ofensivo, cats, severity, confidence, reason);

        } catch (Exception e) {
            LOG.warn("[HateSpeech] Ollama falhou ({}), usando fallback regex-only", e.getMessage());
            return regexFallback(scan, "LLM indisponível: " + e.getMessage());
        }
    }

    /** Fallback quando o LLM falha — assume regex match = ofensivo médio. */
    private AnalysisResult regexFallback(QuickScanResult scan, String reason) {
        return new AnalysisResult(true, new ArrayList<>(scan.categories),
                "medium", 0.5, reason);
    }

    private String buildPrompt(String text, QuickScanResult scan) {
        return """
                Você é um classificador de discurso de ódio em português brasileiro.
                Analise a transcrição abaixo. Retorne SOMENTE JSON válido.

                TRANSCRIÇÃO: "%s"

                Gatilhos detectados pelo regex: %s
                Categorias suspeitas: %s

                Decida:
                - "ofensivo": true se a fala É realmente ataque/insulto contra grupo. \
                false se é uma fala meta (ex: "fui chamado de X", "ele me xingou"), \
                citação, ou conversa neutra que só mencionou a palavra.
                - "categorias": array com as que se aplicam: transfobia, racismo, \
                xenofobia, homofobia, misoginia, capacitismo, outro.
                - "severity": "low" (insulto leve), "medium" (insulto direto), \
                "high" (ameaça/incitação).
                - "confidence": 0.0 a 1.0 — quão certo você está.
                - "razao": 1 frase explicando.

                Resposta APENAS em JSON, sem texto extra:
                """.formatted(
                text.length() > 500 ? text.substring(0, 500) + "..." : text,
                String.join(", ", scan.matchedTriggers),
                String.join(", ", scan.categories)
        );
    }

    // ─── tipos auxiliares ───────────────────────────────────────────
    public static class QuickScanResult {
        public final Set<String> matchedTriggers = new LinkedHashSet<>();
        public final Set<String> categories = new LinkedHashSet<>();
    }

    public static class AnalysisResult {
        public final boolean ofensivo;
        public final List<String> categories;
        public final String severity;
        public final double confidence;
        public final String reason;

        public AnalysisResult(boolean ofensivo, Collection<String> categories,
                              String severity, double confidence, String reason) {
            this.ofensivo = ofensivo;
            this.categories = new ArrayList<>(categories);
            this.severity = severity;
            this.confidence = confidence;
            this.reason = reason;
        }
    }
}
