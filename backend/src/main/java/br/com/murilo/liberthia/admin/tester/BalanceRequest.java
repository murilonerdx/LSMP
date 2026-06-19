package br.com.murilo.liberthia.admin.tester;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Pedido de buff ou nerf de um item/bloco/mecânica do mod feito pelo tester
 * após testar in-game. Admin avalia e move pro changelog ou rejeita.
 *
 * Diferente de bug report (que é um defeito) e de suggestion (que é coisa
 * nova). Balance request é "X já existe mas tá overpowered/underpowered".
 *
 * Estados de fluxo de desenvolvimento:
 *  PENDING       → tester acabou de criar, aguardando admin olhar
 *  UNDER_REVIEW  → admin reconheceu, tá pensando
 *  APPROVED      → admin concordou, vai implementar
 *  IN_DEVELOPMENT → mudança sendo codada
 *  PAUSED        → outras prioridades, vamos voltar depois
 *  IMPLEMENTED   → release com o ajuste já saiu
 *  REJECTED      → admin discordou, item fica como tá
 *
 * Tester pode acompanhar o status na própria tela.
 */
@Entity
@Table(name = "tester_balance_requests", indexes = {
        @Index(name = "idx_bal_tester", columnList = "testerMcName"),
        @Index(name = "idx_bal_status", columnList = "status"),
        @Index(name = "idx_bal_item", columnList = "itemId")
})
public class BalanceRequest {

    public enum Type { BUFF, NERF, REWORK, REMOVE }
    public enum Status {
        PENDING, UNDER_REVIEW, APPROVED, IN_DEVELOPMENT, PAUSED, IMPLEMENTED, REJECTED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 64, nullable = false)
    private String testerMcName;

    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false)
    private Type type = Type.BUFF;

    /** ID Minecraft do item/bloco alvo (ex: "liberthia:dark_matter_sword"). */
    @Column(length = 256, nullable = false)
    private String itemId;

    /** Display name pra mostrar bonito no UI sem precisar resolver via mod. */
    @Column(length = 256)
    private String itemDisplayName;

    @Column(length = 256, nullable = false)
    private String title;

    /** O que o tester quer mudar e por quê. */
    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    /** Comportamento atual em texto livre. */
    @Column(columnDefinition = "TEXT")
    private String currentBehavior;

    /** Comportamento proposto. */
    @Column(columnDefinition = "TEXT")
    private String proposedBehavior;

    /** Evidência (link pra vídeo, screenshot, etc). */
    @Column(length = 512)
    private String evidenceUrl;

    /** Como simulou: solo, PvP, raid, dungeon. */
    @Column(length = 128)
    private String testContext;

    @Enumerated(EnumType.STRING)
    @Column(length = 24, nullable = false)
    private Status status = Status.PENDING;

    @Column(columnDefinition = "TEXT")
    private String adminNote;

    @Column(length = 64)
    private String triagedBy;

    @Column
    private Instant triagedAt;

    /** Pontos dados quando admin implementou (incentivo pra balance feedback). */
    @Column(nullable = false)
    private int pointsAwarded = 0;

    @Column(nullable = false)
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getTesterMcName() { return testerMcName; } public void setTesterMcName(String v) { testerMcName = v; }
    public Type getType() { return type; } public void setType(Type v) { type = v; }
    public String getItemId() { return itemId; } public void setItemId(String v) { itemId = v; }
    public String getItemDisplayName() { return itemDisplayName; } public void setItemDisplayName(String v) { itemDisplayName = v; }
    public String getTitle() { return title; } public void setTitle(String v) { title = v; }
    public String getDescription() { return description; } public void setDescription(String v) { description = v; }
    public String getCurrentBehavior() { return currentBehavior; } public void setCurrentBehavior(String v) { currentBehavior = v; }
    public String getProposedBehavior() { return proposedBehavior; } public void setProposedBehavior(String v) { proposedBehavior = v; }
    public String getEvidenceUrl() { return evidenceUrl; } public void setEvidenceUrl(String v) { evidenceUrl = v; }
    public String getTestContext() { return testContext; } public void setTestContext(String v) { testContext = v; }
    public Status getStatus() { return status; } public void setStatus(Status v) { status = v; }
    public String getAdminNote() { return adminNote; } public void setAdminNote(String v) { adminNote = v; }
    public String getTriagedBy() { return triagedBy; } public void setTriagedBy(String v) { triagedBy = v; }
    public Instant getTriagedAt() { return triagedAt; } public void setTriagedAt(Instant v) { triagedAt = v; }
    public int getPointsAwarded() { return pointsAwarded; } public void setPointsAwarded(int v) { pointsAwarded = v; }
    public Instant getCreatedAt() { return createdAt; }
}
