package br.com.murilo.liberthia.admin.engine.loot;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * Item na loot table com raridade + peso. Quanto maior weight, maior chance.
 * Rarity é um tier visual; o weight é o que controla probabilidade real.
 *
 * Defaults sugeridos por rarity:
 *   common: weight 50
 *   uncommon: 25
 *   rare: 10
 *   epic: 4
 *   legendary: 1
 */
@Entity
@Table(name = "loot_items", indexes = {
        @Index(name = "idx_loot_rarity", columnList = "rarity"),
        @Index(name = "idx_loot_enabled", columnList = "enabled"),
        @Index(name = "idx_loot_category", columnList = "category")
})
public class LootItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 128, nullable = false)
    private String name;

    @Column(length = 128, nullable = false)
    private String itemId;

    @Column(nullable = false)
    private int count = 1;

    /** SNBT opcional pra encantamentos, durabilidade, custom name etc. */
    @Column(columnDefinition = "TEXT")
    private String nbt;

    /** common | uncommon | rare | epic | legendary | mythic */
    @Column(length = 16, nullable = false)
    private String rarity = "common";

    /** Peso pro sorteio ponderado. Quanto maior, maior probabilidade. */
    @Column(nullable = false)
    private int weight = 50;

    /** gear, food, magic, currency, tool, decoration, special. */
    @Column(length = 32)
    private String category = "misc";

    @Column(length = 512)
    private String description;

    @Column
    private boolean enabled = true;

    /** Quantas vezes esse item já foi sorteado. */
    @Column
    private long drawCount = 0;

    @Column(length = 64)
    private String createdBy;

    @Column
    private Instant createdAt;

    @PrePersist
    public void prePersist() { if (createdAt == null) createdAt = Instant.now(); }

    public Long getId() { return id; }
    public String getName() { return name; } public void setName(String v) { this.name = v; }
    public String getItemId() { return itemId; } public void setItemId(String v) { this.itemId = v; }
    public int getCount() { return count; } public void setCount(int v) { this.count = v; }
    public String getNbt() { return nbt; } public void setNbt(String v) { this.nbt = v; }
    public String getRarity() { return rarity; } public void setRarity(String v) { this.rarity = v; }
    public int getWeight() { return weight; } public void setWeight(int v) { this.weight = v; }
    public String getCategory() { return category; } public void setCategory(String v) { this.category = v; }
    public String getDescription() { return description; } public void setDescription(String v) { this.description = v; }
    public boolean isEnabled() { return enabled; } public void setEnabled(boolean v) { this.enabled = v; }
    public long getDrawCount() { return drawCount; } public void setDrawCount(long v) { this.drawCount = v; }
    public String getCreatedBy() { return createdBy; } public void setCreatedBy(String v) { this.createdBy = v; }
    public Instant getCreatedAt() { return createdAt; } public void setCreatedAt(Instant v) { this.createdAt = v; }
}
