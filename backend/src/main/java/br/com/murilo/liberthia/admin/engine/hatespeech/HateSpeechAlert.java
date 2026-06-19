package br.com.murilo.liberthia.admin.engine.hatespeech;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Alerta de discurso de ódio detectado em transcrição de voz.
 *
 * <p>Gerado automaticamente pelo {@code HateSpeechDetectorService} após o
 * Whisper terminar a transcrição. Pipeline em 2 estágios:
 * <ol>
 *   <li>Quick scan (regex/keywords) — quase zero CPU. Só dispara o LLM se
 *       algum gatilho bate.</li>
 *   <li>Deep analyze (Ollama qwen2.5:1.5b) — confirma se é realmente ofensivo
 *       ou falso positivo (ex: "fui chamado de X" não é ofensivo).</li>
 * </ol>
 *
 * <p>Após gerado, o alerta fica no painel admin em /admin/hate-speech.
 * O moderador decide a ação (ignore / warn / mute / kick / ban) que vira
 * comando no servidor MC via mod bridge.
 */
@Entity
@Table(name = "hate_speech_alerts", indexes = {
        @Index(name = "idx_hsa_player", columnList = "playerUuid"),
        @Index(name = "idx_hsa_severity", columnList = "severity"),
        @Index(name = "idx_hsa_reviewed", columnList = "reviewed"),
        @Index(name = "idx_hsa_ts", columnList = "ts")
})
public class HateSpeechAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Clip original (referência pra player ouvir). */
    private Long voiceClipId;

    private String playerUuid;
    private String playerName;

    /** Timestamp epoch ms do clip (não do alerta). */
    private long ts;

    /** Texto completo da transcrição (até 8K chars). */
    @Column(columnDefinition = "TEXT")
    private String transcription;

    /** Categorias detectadas, separadas por vírgula:
     *  transfobia, racismo, xenofobia, homofobia, misoginia, capacitismo, outro */
    @Column(length = 255)
    private String categories;

    /** Gatilhos que bateram no quick scan (palavras encontradas). */
    @Column(length = 500)
    private String triggers;

    /** LOW | MEDIUM | HIGH */
    @Column(length = 16)
    private String severity;

    /** 0.0 a 1.0 — confiança do LLM. Quanto maior, mais certo de ofensivo. */
    private double confidence;

    /** Frase curta do LLM explicando por que classificou assim. */
    @Column(length = 500)
    private String reason;

    /** Modo de detecção: QUICK_ONLY (só regex bateu, LLM desabilitado),
     *  HYBRID (regex + LLM confirmou), LLM_FALSE_POSITIVE (regex bateu mas
     *  LLM rejeitou). */
    @Column(length = 32)
    private String detectionMode;

    /** True = admin já viu e tomou ação (ou ignorou). */
    private boolean reviewed = false;

    /** NULL | IGNORE | WARN | MUTE | KICK | BAN */
    @Column(length = 16)
    private String actionTaken;

    /** Admin que reviewou (se reviewed=true). */
    @Column(length = 64)
    private String reviewedBy;

    private Instant reviewedAt;

    private Instant createdAt = Instant.now();

    // ─── getters/setters ────────────────────────────────────────────
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getVoiceClipId() { return voiceClipId; }
    public void setVoiceClipId(Long voiceClipId) { this.voiceClipId = voiceClipId; }
    public String getPlayerUuid() { return playerUuid; }
    public void setPlayerUuid(String playerUuid) { this.playerUuid = playerUuid; }
    public String getPlayerName() { return playerName; }
    public void setPlayerName(String playerName) { this.playerName = playerName; }
    public long getTs() { return ts; }
    public void setTs(long ts) { this.ts = ts; }
    public String getTranscription() { return transcription; }
    public void setTranscription(String transcription) { this.transcription = transcription; }
    public String getCategories() { return categories; }
    public void setCategories(String categories) { this.categories = categories; }
    public String getTriggers() { return triggers; }
    public void setTriggers(String triggers) { this.triggers = triggers; }
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = confidence; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getDetectionMode() { return detectionMode; }
    public void setDetectionMode(String detectionMode) { this.detectionMode = detectionMode; }
    public boolean isReviewed() { return reviewed; }
    public void setReviewed(boolean reviewed) { this.reviewed = reviewed; }
    public String getActionTaken() { return actionTaken; }
    public void setActionTaken(String actionTaken) { this.actionTaken = actionTaken; }
    public String getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(String reviewedBy) { this.reviewedBy = reviewedBy; }
    public Instant getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(Instant reviewedAt) { this.reviewedAt = reviewedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
