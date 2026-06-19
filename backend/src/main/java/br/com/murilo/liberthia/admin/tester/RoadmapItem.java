package br.com.murilo.liberthia.admin.tester;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Item público do roadmap. Mostrado em /roadmap (sem auth).
 *
 * Categorias representam o pipeline:
 *   IDEA       → ideia bruta, ainda não confirmada
 *   PLANNED    → confirmado, vai entrar em alguma release
 *   IN_DEV     → em desenvolvimento agora
 *   NEXT       → marcado pra próxima release
 *   DONE       → já lançado (move pro Changelog quando faz release)
 *   CANCELLED  → não vai ser feito
 *
 * Voting: qualquer um vota (1 voto por sessão de browser via voterKey
 * salvo no localStorage do cliente). Backend só guarda o counter total
 * pra evitar gravar JSON gigante de voterKeys.
 */
@Entity
@Table(name = "roadmap_items", indexes = {
        @Index(name = "idx_roadmap_category", columnList = "category"),
        @Index(name = "idx_roadmap_priority", columnList = "priority")
})
public class RoadmapItem {

    public enum Category { IDEA, PLANNED, IN_DEV, NEXT, DONE, CANCELLED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 256, nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Category category = Category.IDEA;

    /** Emoji de capa pra UI ficar visual. */
    @Column(length = 8)
    private String emoji;

    /** Tag livre: "voice", "performance", "lore", "mod-integration" etc. */
    @Column(length = 64)
    private String tag;

    /** Total de votos públicos (incrementado por POST /api/roadmap/public/{id}/vote). */
    @Column(nullable = false)
    private int votes = 0;

    /** Ordenação manual dentro da categoria (admin pode pinar items). */
    @Column(nullable = false)
    private int priority = 0;

    /** Versão alvo (ex: "v90") se já planejado. */
    @Column(length = 32)
    private String targetVersion;

    @Column(length = 64)
    private String createdBy;

    @Column(nullable = false)
    private Instant createdAt;

    @Column
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
        if (updatedAt == null) updatedAt = createdAt;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getTitle() { return title; } public void setTitle(String v) { title = v; }
    public String getDescription() { return description; } public void setDescription(String v) { description = v; }
    public Category getCategory() { return category; } public void setCategory(Category v) { category = v; }
    public String getEmoji() { return emoji; } public void setEmoji(String v) { emoji = v; }
    public String getTag() { return tag; } public void setTag(String v) { tag = v; }
    public int getVotes() { return votes; } public void setVotes(int v) { votes = v; }
    public int getPriority() { return priority; } public void setPriority(int v) { priority = v; }
    public String getTargetVersion() { return targetVersion; } public void setTargetVersion(String v) { targetVersion = v; }
    public String getCreatedBy() { return createdBy; } public void setCreatedBy(String v) { createdBy = v; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
