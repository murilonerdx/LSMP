package br.com.murilo.liberthia.admin.tester;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Modelo 3D do BlockBench publicado pelo admin pros testers visualizarem.
 *
 * O arquivo .bbmodel (JSON com texturas base64 embutidas) fica em
 * data/tester-models/&lt;uuid&gt;.bbmodel — fora do DB pra evitar bloat.
 *
 * Apenas admin pode subir/deletar. Tester só visualiza (galeria + viewer 3D).
 */
@Entity
@Table(name = "tester_models_3d")
public class Model3D {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 128, nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    /** Tag livre pra agrupar: "armas", "blocos", "decorativo", "monstros"... */
    @Column(length = 64)
    private String category;

    /**
     * Formato do arquivo: "bbmodel" | "gltf" | "glb" | "obj".
     * Default "bbmodel" pra compat com modelos existentes (que foram
     * uploadados antes desse campo existir).
     */
    @Column(length = 16, nullable = false)
    private String format = "bbmodel";

    /** Nome original do arquivo (ex: dragon_sword.bbmodel). */
    @Column(length = 256, nullable = false)
    private String filename;

    /** UUID + .bbmodel no disco (não usa o ID do banco — chicken-and-egg). */
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

    /** Contador de visualizações (best-effort). */
    @Column(nullable = false)
    private int viewCount = 0;

    @PrePersist
    public void prePersist() {
        if (uploadedAt == null) uploadedAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getName() { return name; } public void setName(String v) { name = v; }
    public String getDescription() { return description; } public void setDescription(String v) { description = v; }
    public String getCategory() { return category; } public void setCategory(String v) { category = v; }
    public String getFormat() { return format; } public void setFormat(String v) { format = v; }
    public String getFilename() { return filename; } public void setFilename(String v) { filename = v; }
    public String getStoragePath() { return storagePath; } public void setStoragePath(String v) { storagePath = v; }
    public long getSizeBytes() { return sizeBytes; } public void setSizeBytes(long v) { sizeBytes = v; }
    public String getUploadedBy() { return uploadedBy; } public void setUploadedBy(String v) { uploadedBy = v; }
    public Instant getUploadedAt() { return uploadedAt; }
    public boolean isEnabled() { return enabled; } public void setEnabled(boolean v) { enabled = v; }
    public int getViewCount() { return viewCount; } public void setViewCount(int v) { viewCount = v; }
}
