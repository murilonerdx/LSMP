package br.com.murilo.liberthia.admin.engine.wiki;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * Página de wiki comunitária. Renderiza como livro Patchouli in-game
 * via JSON gerado (POST /api/wiki/{id}/export-patchouli).
 */
@Entity
@Table(name = "wiki_pages", indexes = {
        @Index(name = "idx_wiki_slug", columnList = "slug", unique = true),
        @Index(name = "idx_wiki_status", columnList = "status"),
        @Index(name = "idx_wiki_updated", columnList = "updatedAt DESC")
})
public class WikiPage {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(length = 128, unique = true) private String slug;
    @Column(length = 256) private String title;
    @Column(length = 64) private String category;       // "lore", "tutorial", "history"...
    // SEM @Lob — Hibernate 6 trata @Lob em String como CLOB materializado, e LOWER()
    // rejeita esse tipo no startup com "Parameter 1 of function 'lower()' has type
    // 'STRING', but argument is of type 'java.lang.String'". TEXT por columnDefinition
    // já é o suficiente pro Postgres usar texto sem limite.
    @Column(columnDefinition = "TEXT") private String contentMarkdown;
    @Column(length = 64) private String authorUuid;
    @Column(length = 64) private String authorName;
    @Column(length = 16) private String status = "draft"; // draft | published | archived
    @Column private int views = 0;
    @Column private Instant createdAt;
    @Column private Instant updatedAt;

    public Long getId() { return id; }
    public String getSlug() { return slug; }
    public void setSlug(String v) { slug = v; }
    public String getTitle() { return title; }
    public void setTitle(String v) { title = v; }
    public String getCategory() { return category; }
    public void setCategory(String v) { category = v; }
    public String getContentMarkdown() { return contentMarkdown; }
    public void setContentMarkdown(String v) { contentMarkdown = v; }
    public String getAuthorUuid() { return authorUuid; }
    public void setAuthorUuid(String v) { authorUuid = v; }
    public String getAuthorName() { return authorName; }
    public void setAuthorName(String v) { authorName = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { status = v; }
    public int getViews() { return views; }
    public void setViews(int v) { views = v; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant v) { createdAt = v; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant v) { updatedAt = v; }
}
