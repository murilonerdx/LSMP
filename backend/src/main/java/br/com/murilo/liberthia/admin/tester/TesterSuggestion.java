package br.com.murilo.liberthia.admin.tester;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Sugestão de tester pra adicionar novo item/bloco/artefato ao mod.
 * Outros testers votam (upvote/downvote). Admin desempata e decide.
 *
 * Votos guardados em JSON: { "mcName1": 1, "mcName2": -1, ... }
 */
@Entity
@Table(name = "tester_suggestions", indexes = {
        @Index(name = "idx_sug_author", columnList = "authorMcName"),
        @Index(name = "idx_sug_status", columnList = "status")
})
public class TesterSuggestion {

    /**
     * Workflow expandido pra acompanhar desenvolvimento:
     *  PENDING        → recém criada
     *  UNDER_REVIEW   → admin olhou e tá ponderando
     *  APPROVED       → admin aprovou conceitualmente
     *  IN_DEVELOPMENT → tá sendo implementada agora
     *  PAUSED         → começou mas pausou (outras prioridades)
     *  IMPLEMENTED    → release saiu com ela
     *  REJECTED       → admin recusou
     */
    public enum Status { PENDING, UNDER_REVIEW, APPROVED, IN_DEVELOPMENT, PAUSED, IMPLEMENTED, REJECTED }
    public enum Type { ITEM, BLOCK, ARTIFACT, MECHANIC, MOB, OTHER }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 64, nullable = false)
    private String authorMcName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Type type = Type.ITEM;

    @Column(length = 256, nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    /** Detalhes técnicos: propriedades, comportamento, balanço. */
    @Column(columnDefinition = "TEXT")
    private String technicalDetails;

    /** URL externa de referência (imagem/inspiração). */
    @Column(length = 512)
    private String referenceUrl;

    /** URL de imagem/textura proposta pro item/bloco. */
    @Column(length = 512)
    private String iconUrl;

    /**
     * Recipe visual em JSON: { "slots": [9 strings], "result": "...", "type": "shaped|shapeless|smelting" }
     * slots[0..8] = 3x3 grid (item ID Minecraft ou null se vazio).
     * Frontend renderiza com autocomplete de items do mod.
     */
    @Column(columnDefinition = "TEXT")
    private String recipeJson;

    /**
     * Efeitos/propriedades em JSON: { "damage": 8, "durability": 1500, "effects": [{ "id":"poison", "duration":200 }] }
     */
    @Column(columnDefinition = "TEXT")
    private String effectsJson;

    /** Sugestão de ID Minecraft (ex: "liberthia:void_blade"). */
    @Column(length = 256)
    private String suggestedItemId;

    /** Status de desenvolvimento detalhado (texto livre): "fazendo modelo 3D...", "esperando testers". */
    @Column(columnDefinition = "TEXT")
    private String developmentNote;

    /** Quando o status foi alterado pela última vez. */
    @Column
    private Instant statusChangedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private Status status = Status.PENDING;

    /** JSON: {mcName: +1|-1, ...}. */
    @Column(columnDefinition = "TEXT")
    private String votesJson;

    /** Cache de upvotes/downvotes pra ordenar rápido. */
    @Column(nullable = false)
    private int upvotes = 0;

    @Column(nullable = false)
    private int downvotes = 0;

    @Column(columnDefinition = "TEXT")
    private String adminNote;

    @Column(length = 64)
    private String triagedBy;

    @Column
    private Instant triagedAt;

    @Column(nullable = false)
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getAuthorMcName() { return authorMcName; } public void setAuthorMcName(String v) { authorMcName = v; }
    public Type getType() { return type; } public void setType(Type v) { type = v; }
    public String getTitle() { return title; } public void setTitle(String v) { title = v; }
    public String getDescription() { return description; } public void setDescription(String v) { description = v; }
    public String getTechnicalDetails() { return technicalDetails; } public void setTechnicalDetails(String v) { technicalDetails = v; }
    public String getReferenceUrl() { return referenceUrl; } public void setReferenceUrl(String v) { referenceUrl = v; }
    public String getIconUrl() { return iconUrl; } public void setIconUrl(String v) { iconUrl = v; }
    public String getRecipeJson() { return recipeJson; } public void setRecipeJson(String v) { recipeJson = v; }
    public String getEffectsJson() { return effectsJson; } public void setEffectsJson(String v) { effectsJson = v; }
    public String getSuggestedItemId() { return suggestedItemId; } public void setSuggestedItemId(String v) { suggestedItemId = v; }
    public String getDevelopmentNote() { return developmentNote; } public void setDevelopmentNote(String v) { developmentNote = v; }
    public Instant getStatusChangedAt() { return statusChangedAt; } public void setStatusChangedAt(Instant v) { statusChangedAt = v; }
    public Status getStatus() { return status; } public void setStatus(Status v) { status = v; }
    public String getVotesJson() { return votesJson; } public void setVotesJson(String v) { votesJson = v; }
    public int getUpvotes() { return upvotes; } public void setUpvotes(int v) { upvotes = v; }
    public int getDownvotes() { return downvotes; } public void setDownvotes(int v) { downvotes = v; }
    public String getAdminNote() { return adminNote; } public void setAdminNote(String v) { adminNote = v; }
    public String getTriagedBy() { return triagedBy; } public void setTriagedBy(String v) { triagedBy = v; }
    public Instant getTriagedAt() { return triagedAt; } public void setTriagedAt(Instant v) { triagedAt = v; }
    public Instant getCreatedAt() { return createdAt; }
}
