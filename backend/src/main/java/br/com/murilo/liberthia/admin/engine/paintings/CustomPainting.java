package br.com.murilo.liberthia.admin.engine.paintings;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * Pintura custom feita por player (mod xercapaint). Painel admin recebe upload
 * de PNG do trabalho e mantém galeria curada.
 */
@Entity
@Table(name = "custom_paintings", indexes = {
        @Index(name = "idx_painting_author", columnList = "authorUuid"),
        @Index(name = "idx_painting_created", columnList = "createdAt DESC")
})
public class CustomPainting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 128, nullable = false)
    private String title;

    @Column(length = 64)
    private String authorUuid;

    @Column(length = 64)
    private String authorName;

    @Column(length = 512)
    private String description;

    /** Path relativo do PNG em disco. */
    @Column(length = 512)
    private String filePath;

    private int width;
    private int height;
    private long sizeBytes;

    @Column(length = 32)
    private String category = "art"; // art | meme | event | sketch

    private boolean featured = false;

    @Column
    private long viewCount = 0;

    @Column
    private Instant createdAt;

    @PrePersist
    public void prePersist() { if (createdAt == null) createdAt = Instant.now(); }

    public Long getId() { return id; }
    public String getTitle() { return title; } public void setTitle(String v) { this.title = v; }
    public String getAuthorUuid() { return authorUuid; } public void setAuthorUuid(String v) { this.authorUuid = v; }
    public String getAuthorName() { return authorName; } public void setAuthorName(String v) { this.authorName = v; }
    public String getDescription() { return description; } public void setDescription(String v) { this.description = v; }
    public String getFilePath() { return filePath; } public void setFilePath(String v) { this.filePath = v; }
    public int getWidth() { return width; } public void setWidth(int v) { this.width = v; }
    public int getHeight() { return height; } public void setHeight(int v) { this.height = v; }
    public long getSizeBytes() { return sizeBytes; } public void setSizeBytes(long v) { this.sizeBytes = v; }
    public String getCategory() { return category; } public void setCategory(String v) { this.category = v; }
    public boolean isFeatured() { return featured; } public void setFeatured(boolean v) { this.featured = v; }
    public long getViewCount() { return viewCount; } public void setViewCount(long v) { this.viewCount = v; }
    public Instant getCreatedAt() { return createdAt; } public void setCreatedAt(Instant v) { this.createdAt = v; }
}
