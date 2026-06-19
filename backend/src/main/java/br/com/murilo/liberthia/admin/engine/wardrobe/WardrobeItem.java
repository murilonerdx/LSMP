package br.com.murilo.liberthia.admin.engine.wardrobe;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * Item cosmético compartilhado: skin/outfit do ArmourersWorkshop, CakeCosmetics,
 * CustomPlayerModels. Backend só guarda metadata + path do arquivo cosmético.
 */
@Entity
@Table(name = "wardrobe_items", indexes = {
        @Index(name = "idx_ward_type", columnList = "kind"),
        @Index(name = "idx_ward_uploader", columnList = "uploaderUuid")
})
public class WardrobeItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(length = 128) private String name;
    @Column(length = 512) private String description;
    @Column(length = 32) private String kind;            // "armor", "cpm", "cosmetic", "skin"
    @Column(length = 512) private String filePath;       // ex "wardrobe/abcd.armour" / "wardrobe/xyz.cpmproject"
    @Column private long sizeBytes;
    @Column(length = 64) private String uploaderUuid;
    @Column(length = 64) private String uploaderName;
    @Column private int downloads = 0;
    @Column private int upvotes = 0;
    @Column private int priceMatter = 0;                 // 0 = grátis; > 0 = custo em matéria pra equipar
    @Column private Instant createdAt;

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String v) { name = v; }
    public String getDescription() { return description; }
    public void setDescription(String v) { description = v; }
    public String getKind() { return kind; }
    public void setKind(String v) { kind = v; }
    public String getFilePath() { return filePath; }
    public void setFilePath(String v) { filePath = v; }
    public long getSizeBytes() { return sizeBytes; }
    public void setSizeBytes(long v) { sizeBytes = v; }
    public String getUploaderUuid() { return uploaderUuid; }
    public void setUploaderUuid(String v) { uploaderUuid = v; }
    public String getUploaderName() { return uploaderName; }
    public void setUploaderName(String v) { uploaderName = v; }
    public int getDownloads() { return downloads; }
    public void setDownloads(int v) { downloads = v; }
    public int getUpvotes() { return upvotes; }
    public void setUpvotes(int v) { upvotes = v; }
    public int getPriceMatter() { return priceMatter; }
    public void setPriceMatter(int v) { priceMatter = v; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant v) { createdAt = v; }
}
