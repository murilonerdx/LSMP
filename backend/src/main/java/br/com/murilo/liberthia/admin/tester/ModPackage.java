package br.com.murilo.liberthia.admin.tester;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Pacote ZIP de mods publicado pelo admin para mod testers baixarem.
 * Arquivo binário fica em data/tester-packages/<id>.zip (fora do DB).
 */
@Entity
@Table(name = "tester_mod_packages")
public class ModPackage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 128, nullable = false)
    private String name;

    @Column(length = 32)
    private String version;

    @Column(columnDefinition = "TEXT")
    private String description;

    /** Nome original do arquivo (ex: liberthia-beta-0.1.8.zip). */
    @Column(length = 256, nullable = false)
    private String filename;

    /** Path relativo no storage (ex: 12.zip). */
    @Column(length = 64, nullable = false)
    private String storagePath;

    @Column(nullable = false)
    private long sizeBytes;

    @Column(length = 64)
    private String uploadedBy;

    @Column(nullable = false)
    private Instant uploadedAt;

    @Column(nullable = false)
    private boolean enabled = true;

    /** Contador pra estatística. */
    @Column(nullable = false)
    private int downloadCount = 0;

    @PrePersist
    public void prePersist() {
        if (uploadedAt == null) uploadedAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getName() { return name; } public void setName(String v) { name = v; }
    public String getVersion() { return version; } public void setVersion(String v) { version = v; }
    public String getDescription() { return description; } public void setDescription(String v) { description = v; }
    public String getFilename() { return filename; } public void setFilename(String v) { filename = v; }
    public String getStoragePath() { return storagePath; } public void setStoragePath(String v) { storagePath = v; }
    public long getSizeBytes() { return sizeBytes; } public void setSizeBytes(long v) { sizeBytes = v; }
    public String getUploadedBy() { return uploadedBy; } public void setUploadedBy(String v) { uploadedBy = v; }
    public Instant getUploadedAt() { return uploadedAt; }
    public boolean isEnabled() { return enabled; } public void setEnabled(boolean v) { enabled = v; }
    public int getDownloadCount() { return downloadCount; } public void setDownloadCount(int v) { downloadCount = v; }
}
