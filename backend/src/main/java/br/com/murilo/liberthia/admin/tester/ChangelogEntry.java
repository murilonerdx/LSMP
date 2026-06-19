package br.com.murilo.liberthia.admin.tester;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Entrada de changelog público — lista o que veio em cada release do mod.
 * Visível em /changelog SEM autenticação (qualquer um vê).
 *
 * Cada lista (items, bugs, buffs, debuffs, integrations) é armazenada
 * como JSON de string com items separados por linha. Frontend faz parse
 * e renderiza como bullet list.
 */
@Entity
@Table(name = "changelog_entries", indexes = {
        @Index(name = "idx_changelog_version", columnList = "version"),
        @Index(name = "idx_changelog_release", columnList = "releaseDate")
})
public class ChangelogEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Ex: "v85", "1.2.3-beta". Único na prática mas não enforced. */
    @Column(length = 32, nullable = false)
    private String version;

    @Column(nullable = false)
    private Instant releaseDate;

    @Column(length = 256, nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String summary;

    /** Items novos. Uma linha por item. Markdown leve permitido. */
    @Column(columnDefinition = "TEXT")
    private String itemsAdded;

    /** Bugs resolvidos. Pode incluir crédito a quem reportou. */
    @Column(columnDefinition = "TEXT")
    private String bugsFixed;

    /** Buffs aplicados (items ficaram mais fortes). */
    @Column(columnDefinition = "TEXT")
    private String buffs;

    /** Debuffs / nerfs aplicados. */
    @Column(columnDefinition = "TEXT")
    private String debuffs;

    /** Integrações com outros mods adicionadas/melhoradas. */
    @Column(columnDefinition = "TEXT")
    private String integrations;

    /** Créditos: quem encontrou bugs, sugeriu features, contribuiu. */
    @Column(columnDefinition = "TEXT")
    private String credits;

    /** Notas adicionais (warnings, known issues, etc). */
    @Column(columnDefinition = "TEXT")
    private String notes;

    /**
     * JSON-array de bugs estruturados resolvidos nesta versão. Schema v2:
     * cada entrada é {@code {id, title, severity, priority, reporterMcName,
     * pointsAwarded, itemId, description, screenshotUrl, status, fixDetails,
     * fixedIn, triagedAt}}. Frontend renderiza cards com detalhe completo
     * no ChangelogDetailModal (reporter, data, severidade, screenshot).
     *
     * <p>Antes os bugs viviam só como string concatenada em {@code bugsFixed}.
     * Agora persistimos a estrutura inteira pra modal mostrar tudo. {@code bugsFixed}
     * continua sendo gerado/usado como resumo legado.
     */
    @Column(columnDefinition = "TEXT")
    private String bugsJson;

    /**
     * JSON-array de sugestões da comunidade incluídas nesta versão. Schema:
     * {@code {id, title, type, authorMcName, score, upvotes, downvotes,
     * description, suggestedItemId, iconUrl, status, createdAt, adminNote}}.
     * Frontend renderiza no ChangelogDetailModal junto com os bugs.
     */
    @Column(columnDefinition = "TEXT")
    private String suggestionsJson;

    /** Se true, aparece destacada no topo. Para releases importantes. */
    @Column(nullable = false)
    private boolean highlighted = false;

    @Column(length = 64)
    private String createdBy;

    @Column(nullable = false)
    private Instant createdAt;

    @Column
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
        if (releaseDate == null) releaseDate = createdAt;
        if (updatedAt == null) updatedAt = createdAt;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getVersion() { return version; } public void setVersion(String v) { version = v; }
    public Instant getReleaseDate() { return releaseDate; } public void setReleaseDate(Instant v) { releaseDate = v; }
    public String getTitle() { return title; } public void setTitle(String v) { title = v; }
    public String getSummary() { return summary; } public void setSummary(String v) { summary = v; }
    public String getItemsAdded() { return itemsAdded; } public void setItemsAdded(String v) { itemsAdded = v; }
    public String getBugsFixed() { return bugsFixed; } public void setBugsFixed(String v) { bugsFixed = v; }
    public String getBuffs() { return buffs; } public void setBuffs(String v) { buffs = v; }
    public String getDebuffs() { return debuffs; } public void setDebuffs(String v) { debuffs = v; }
    public String getIntegrations() { return integrations; } public void setIntegrations(String v) { integrations = v; }
    public String getCredits() { return credits; } public void setCredits(String v) { credits = v; }
    public String getNotes() { return notes; } public void setNotes(String v) { notes = v; }
    public String getBugsJson() { return bugsJson; } public void setBugsJson(String v) { bugsJson = v; }
    public String getSuggestionsJson() { return suggestionsJson; } public void setSuggestionsJson(String v) { suggestionsJson = v; }
    public boolean isHighlighted() { return highlighted; } public void setHighlighted(boolean v) { highlighted = v; }
    public String getCreatedBy() { return createdBy; } public void setCreatedBy(String v) { createdBy = v; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
