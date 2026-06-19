package br.com.murilo.liberthia.admin.engine.videoeditor;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Vídeo persistido no servidor — com título, descrição, e flag "público"
 * (acessível sem login via /watch/{id} no frontend).
 *
 * Pode ser:
 *  - rendered: vídeo gerado pelo Video Editor (jobId reference)
 *  - uploaded: upload manual de MP4 já pronto
 */
@Entity
@Table(name = "saved_videos", indexes = {
        @Index(name = "idx_savedvid_pub_created", columnList = "isPublic, createdAt DESC"),
        @Index(name = "idx_savedvid_created", columnList = "createdAt DESC")
})
public class SavedVideo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 256, nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    /** Filename relativo em /app/data/videos/saved/ (UUID.mp4). */
    @Column(length = 256, nullable = false)
    private String filename;

    @Column(nullable = false)
    private long sizeBytes;

    /** Duração estimada em ms (probe via ffprobe na save). */
    @Column(nullable = false)
    private long durationMs;

    /** Se true, /watch/{id} é acessível sem login. */
    @Column(nullable = false)
    private boolean isPublic = true;

    @Column(length = 64)
    private String uploadedBy;

    /** Source: "rendered" (do editor) ou "uploaded" (manual). */
    @Column(length = 16, nullable = false)
    private String source = "uploaded";

    /** jobId do render original (se foi salvo do editor). Nullable. */
    @Column(length = 32)
    private String renderJobId;

    /** Contador de views (incrementa em GET /watch/{id}). */
    @Column(nullable = false)
    private long viewCount = 0;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
    }
    @PreUpdate
    public void preUpdate() { updatedAt = Instant.now(); }

    public Long getId() { return id; }
    public String getTitle() { return title; } public void setTitle(String v) { this.title = v; }
    public String getDescription() { return description; } public void setDescription(String v) { this.description = v; }
    public String getFilename() { return filename; } public void setFilename(String v) { this.filename = v; }
    public long getSizeBytes() { return sizeBytes; } public void setSizeBytes(long v) { this.sizeBytes = v; }
    public long getDurationMs() { return durationMs; } public void setDurationMs(long v) { this.durationMs = v; }
    public boolean isPublic() { return isPublic; } public void setPublic(boolean v) { this.isPublic = v; }
    public String getUploadedBy() { return uploadedBy; } public void setUploadedBy(String v) { this.uploadedBy = v; }
    public String getSource() { return source; } public void setSource(String v) { this.source = v; }
    public String getRenderJobId() { return renderJobId; } public void setRenderJobId(String v) { this.renderJobId = v; }
    public long getViewCount() { return viewCount; } public void setViewCount(long v) { this.viewCount = v; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
