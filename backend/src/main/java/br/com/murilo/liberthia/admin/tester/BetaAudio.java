package br.com.murilo.liberthia.admin.tester;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Áudio beta (criatura, instrumento, ambient, UI) publicado pelo admin
 * pros testers escutarem e votarem.
 *
 * Arquivo binário fica em data/tester-audios/&lt;uuid&gt;.{mp3|wav|ogg}.
 * Streaming pelo backend via /api/tester/audios/{id}/stream — frontend
 * carrega como blob (precisa do Authorization header).
 */
@Entity
@Table(name = "tester_beta_audios")
public class BetaAudio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 128, nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    /** Tag livre: "criatura", "instrumento", "ambient", "UI"... */
    @Column(length = 64)
    private String category;

    /** ID da criatura/mob vinculada (opcional, free text — ex: "minecraft:zombie"). */
    @Column(length = 128)
    private String creatureId;

    @Column(length = 256, nullable = false)
    private String filename;

    /** UUID + ext no disco. */
    @Column(length = 64, nullable = false)
    private String storagePath;

    /** audio/mpeg, audio/wav, audio/ogg. Browser usa pra escolher decoder. */
    @Column(length = 64)
    private String mimeType;

    @Column(nullable = false)
    private long sizeBytes;

    /** Duração em segundos. Opcional — pode ser nullable se admin não souber. */
    @Column
    private Integer durationSec;

    @Column(length = 64)
    private String uploadedBy;

    @Column(nullable = false)
    private Instant uploadedAt;

    @Column(nullable = false)
    private boolean enabled = true;

    /** Contador de plays (best-effort, incrementado por stream). */
    @Column(nullable = false)
    private int playCount = 0;

    @PrePersist
    public void prePersist() {
        if (uploadedAt == null) uploadedAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getName() { return name; } public void setName(String v) { name = v; }
    public String getDescription() { return description; } public void setDescription(String v) { description = v; }
    public String getCategory() { return category; } public void setCategory(String v) { category = v; }
    public String getCreatureId() { return creatureId; } public void setCreatureId(String v) { creatureId = v; }
    public String getFilename() { return filename; } public void setFilename(String v) { filename = v; }
    public String getStoragePath() { return storagePath; } public void setStoragePath(String v) { storagePath = v; }
    public String getMimeType() { return mimeType; } public void setMimeType(String v) { mimeType = v; }
    public long getSizeBytes() { return sizeBytes; } public void setSizeBytes(long v) { sizeBytes = v; }
    public Integer getDurationSec() { return durationSec; } public void setDurationSec(Integer v) { durationSec = v; }
    public String getUploadedBy() { return uploadedBy; } public void setUploadedBy(String v) { uploadedBy = v; }
    public Instant getUploadedAt() { return uploadedAt; }
    public boolean isEnabled() { return enabled; } public void setEnabled(boolean v) { enabled = v; }
    public int getPlayCount() { return playCount; } public void setPlayCount(int v) { playCount = v; }
}
