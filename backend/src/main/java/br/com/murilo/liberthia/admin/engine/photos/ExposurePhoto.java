package br.com.murilo.liberthia.admin.engine.photos;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * Foto tirada in-game via mod Exposure.
 * Workflow: usuario carrega PNG no painel → backend salva PNG em disco + DB.
 */
@Entity
@Table(name = "exposure_photos", indexes = {
        @Index(name = "idx_photos_player_ts", columnList = "authorUuid, takenAt DESC"),
        @Index(name = "idx_photos_ts", columnList = "takenAt DESC")
})
public class ExposurePhoto {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(length = 64) private String authorUuid;
    @Column(length = 64) private String authorName;
    @Column(length = 256) private String title;
    @Column(length = 1024) private String description;
    @Column(length = 512) private String filePath;       // caminho relativo em VOICE_STORAGE_DIR/../photos/
    @Column private long sizeBytes;
    @Column private int width, height;
    @Column private Instant takenAt;
    @Column private boolean cursed = false;              // admin marca = aparece em "evidências"
    @Column(length = 64) private String dimension;
    @Column private double x, y, z;

    public Long getId() { return id; }
    public String getAuthorUuid() { return authorUuid; }
    public void setAuthorUuid(String v) { authorUuid = v; }
    public String getAuthorName() { return authorName; }
    public void setAuthorName(String v) { authorName = v; }
    public String getTitle() { return title; }
    public void setTitle(String v) { title = v; }
    public String getDescription() { return description; }
    public void setDescription(String v) { description = v; }
    public String getFilePath() { return filePath; }
    public void setFilePath(String v) { filePath = v; }
    public long getSizeBytes() { return sizeBytes; }
    public void setSizeBytes(long v) { sizeBytes = v; }
    public int getWidth() { return width; } public void setWidth(int v) { width = v; }
    public int getHeight() { return height; } public void setHeight(int v) { height = v; }
    public Instant getTakenAt() { return takenAt; }
    public void setTakenAt(Instant v) { takenAt = v; }
    public boolean isCursed() { return cursed; }
    public void setCursed(boolean v) { cursed = v; }
    public String getDimension() { return dimension; } public void setDimension(String v) { dimension = v; }
    public double getX() { return x; } public void setX(double v) { x = v; }
    public double getY() { return y; } public void setY(double v) { y = v; }
    public double getZ() { return z; } public void setZ(double v) { z = v; }
}
