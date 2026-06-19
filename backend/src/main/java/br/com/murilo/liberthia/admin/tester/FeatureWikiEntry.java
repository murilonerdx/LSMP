package br.com.murilo.liberthia.admin.tester;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Entrada de wiki por feature/item/bloco do mod. Admin escreve docs
 * explicativos pra cada coisa nova (como usar, crafting, efeitos, lore).
 *
 * Não confundir com WikiPage genérica do projeto — essa aqui é específica
 * do contexto tester: lista features novas e quem contribuiu (bug found,
 * suggestion approved). Aparece numa aba "Wiki" no dashboard do tester.
 *
 * Fluxo:
 *  - Admin cria entry pra cada nova feature
 *  - Linka pro item/bloco (slug ou itemId)
 *  - Cita créditos (mcNames dos testers que contribuíram)
 *  - Quando linka, sistema notifica os mcNames com NotificationType.WIKI_CREDIT
 */
@Entity
@Table(name = "feature_wiki_entries", indexes = {
        @Index(name = "idx_wiki_slug", columnList = "slug", unique = true),
        @Index(name = "idx_wiki_itemid", columnList = "itemId"),
        @Index(name = "idx_wiki_published", columnList = "published")
})
public class FeatureWikiEntry {

    public enum Category {
        ITEM, BLOCK, ARTIFACT, MECHANIC, MOB, RITUAL, TOOL, ARMOR, WEAPON, OTHER
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Slug pra URL: "/wiki/{slug}". Ex: "dark-matter-sword". */
    @Column(length = 128, nullable = false, unique = true)
    private String slug;

    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false)
    private Category category = Category.ITEM;

    @Column(length = 256, nullable = false)
    private String title;

    /** ID minecraft (ex: "liberthia:dark_matter_sword"). Opcional. */
    @Column(length = 256)
    private String itemId;

    /** URL de imagem/textura. */
    @Column(length = 512)
    private String imageUrl;

    /** Resumo curto (1-2 frases). */
    @Column(length = 512)
    private String summary;

    /** Conteúdo completo em markdown. */
    @Column(columnDefinition = "TEXT", nullable = false)
    private String contentMd;

    /** JSON array de mcNames com crédito: ["Bob", "Alice"]. */
    @Column(columnDefinition = "TEXT")
    private String creditsJson;

    /** JSON com receita visual (slots 3x3 + resultado). Pode ser null. */
    @Column(columnDefinition = "TEXT")
    private String recipeJson;

    /** Versão do mod onde foi adicionado (changelog ref). */
    @Column(length = 32)
    private String addedInVersion;

    @Column(nullable = false)
    private boolean published = false;

    /** Tags livres separadas por vírgula: "weapon,endgame,boss". */
    @Column(length = 256)
    private String tags;

    @Column(length = 64)
    private String authorAdmin;

    @Column(nullable = false)
    private Instant createdAt;

    @Column
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getSlug() { return slug; } public void setSlug(String v) { slug = v; }
    public Category getCategory() { return category; } public void setCategory(Category v) { category = v; }
    public String getTitle() { return title; } public void setTitle(String v) { title = v; }
    public String getItemId() { return itemId; } public void setItemId(String v) { itemId = v; }
    public String getImageUrl() { return imageUrl; } public void setImageUrl(String v) { imageUrl = v; }
    public String getSummary() { return summary; } public void setSummary(String v) { summary = v; }
    public String getContentMd() { return contentMd; } public void setContentMd(String v) { contentMd = v; }
    public String getCreditsJson() { return creditsJson; } public void setCreditsJson(String v) { creditsJson = v; }
    public String getRecipeJson() { return recipeJson; } public void setRecipeJson(String v) { recipeJson = v; }
    public String getAddedInVersion() { return addedInVersion; } public void setAddedInVersion(String v) { addedInVersion = v; }
    public boolean isPublished() { return published; } public void setPublished(boolean v) { published = v; }
    public String getTags() { return tags; } public void setTags(String v) { tags = v; }
    public String getAuthorAdmin() { return authorAdmin; } public void setAuthorAdmin(String v) { authorAdmin = v; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
