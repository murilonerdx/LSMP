package br.com.murilo.liberthia.admin.engine.voice;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * Um clipe de voz capturado de um player via Simple Voice Chat.
 * Mod faz upload do WAV em disco; o registro aqui guarda metadata + caminho.
 *
 * Lifecycle:
 *  1. Mod chama POST /api/mod/voice/reserve → cria row com filePath = null,
 *     uploadStatus = PENDING.
 *  2. Mod chama PUT /api/mod/voice/clips/{id}/audio → escreve WAV em disco,
 *     atualiza filePath + sizeBytes + uploadStatus = DONE.
 *  3. Background job pode chamar Whisper/STT pra preencher transcription.
 */
@Entity
@Table(name = "voice_clips", indexes = {
        @Index(name = "idx_voice_player_ts", columnList = "playerUuid, ts DESC"),
        @Index(name = "idx_voice_ts", columnList = "ts DESC"),
        @Index(name = "idx_voice_status", columnList = "uploadStatus")
})
public class VoiceClip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 64)
    private String playerUuid;

    @Column(length = 64)
    private String playerName;

    /** Timestamp do início do utterance (millis epoch). */
    private long ts;

    private long durationMs;

    private long sizeBytes;

    /** Caminho relativo do WAV em disco (data/voice-clips/yyyy-MM/xxx.wav). */
    @Column(length = 512)
    private String filePath;

    private Double posX, posY, posZ;

    @Column(length = 64)
    private String dimension;

    /** Transcrição (preenchida por STT job). Null = ainda não transcrito. */
    @Column(columnDefinition = "TEXT")
    private String transcription;

    /** PENDING / DONE / TRANSCRIBED / FAILED. */
    @Column(length = 16)
    private String uploadStatus = "PENDING";

    /** PENDING / DONE / FAILED / SKIPPED. */
    @Column(length = 16)
    private String transcriptionStatus = "PENDING";

    @Column(length = 16)
    private String language = "pt-BR";

    /** JSON array de tags pra organização ("death", "memorial:abc", "haunted"). */
    @Column(columnDefinition = "TEXT")
    private String tagsJson = "[]";

    /**
     * Se true, NÃO é deletado pela auto-cleanup (>14 dias). Admin marca via painel.
     * Útil pra preservar clipes especiais (mortes icônicas, falas memoráveis).
     *
     * NOTA: usar Boolean (wrapper) em vez de boolean primitive porque rows antigas
     * (criadas antes deste campo existir) têm NULL nessa coluna. Hibernate falha
     * ao mapear NULL → boolean primitive com "Can not set boolean field to null".
     * Wrapper aceita null; getter normaliza pra false.
     */
    @Column
    private Boolean protectedFromCleanup = false;

    /** Motivo da proteção — opcional, pra log. */
    @Column(length = 256)
    private String protectionReason;

    @Column(length = 64)
    private String protectedBy;

    private Instant protectedAt;

    private Instant createdAt;

    public VoiceClip() {}

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getPlayerUuid() { return playerUuid; } public void setPlayerUuid(String v) { this.playerUuid = v; }
    public String getPlayerName() { return playerName; } public void setPlayerName(String v) { this.playerName = v; }
    public long getTs() { return ts; } public void setTs(long v) { this.ts = v; }
    public long getDurationMs() { return durationMs; } public void setDurationMs(long v) { this.durationMs = v; }
    public long getSizeBytes() { return sizeBytes; } public void setSizeBytes(long v) { this.sizeBytes = v; }
    public String getFilePath() { return filePath; } public void setFilePath(String v) { this.filePath = v; }
    public Double getPosX() { return posX; } public void setPosX(Double v) { this.posX = v; }
    public Double getPosY() { return posY; } public void setPosY(Double v) { this.posY = v; }
    public Double getPosZ() { return posZ; } public void setPosZ(Double v) { this.posZ = v; }
    public String getDimension() { return dimension; } public void setDimension(String v) { this.dimension = v; }
    public String getTranscription() { return transcription; } public void setTranscription(String v) { this.transcription = v; }
    public String getUploadStatus() { return uploadStatus; } public void setUploadStatus(String v) { this.uploadStatus = v; }
    public String getTranscriptionStatus() { return transcriptionStatus; } public void setTranscriptionStatus(String v) { this.transcriptionStatus = v; }
    public String getLanguage() { return language; } public void setLanguage(String v) { this.language = v; }
    public String getTagsJson() { return tagsJson; } public void setTagsJson(String v) { this.tagsJson = v; }
    public boolean isProtectedFromCleanup() { return Boolean.TRUE.equals(protectedFromCleanup); }
    public void setProtectedFromCleanup(boolean v) { this.protectedFromCleanup = v; }
    public String getProtectionReason() { return protectionReason; }
    public void setProtectionReason(String v) { this.protectionReason = v; }
    public String getProtectedBy() { return protectedBy; }
    public void setProtectedBy(String v) { this.protectedBy = v; }
    public Instant getProtectedAt() { return protectedAt; }
    public void setProtectedAt(Instant v) { this.protectedAt = v; }
    public Instant getCreatedAt() { return createdAt; } public void setCreatedAt(Instant v) { this.createdAt = v; }
}
