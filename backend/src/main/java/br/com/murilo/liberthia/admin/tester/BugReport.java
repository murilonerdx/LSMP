package br.com.murilo.liberthia.admin.tester;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Bug report criado pelo tester. Admin triagga: confirma (dá pontos)
 * ou rejeita (sem pontos).
 */
@Entity
@Table(name = "tester_bug_reports", indexes = {
        @Index(name = "idx_bug_tester", columnList = "testerMcName"),
        @Index(name = "idx_bug_status", columnList = "status")
})
public class BugReport {

    public enum Status { PENDING, CONFIRMED, REJECTED, DUPLICATE }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** MC name do tester que reportou (linka pra ModTester por mcName). */
    @Column(length = 64, nullable = false)
    private String testerMcName;

    /** Item beta relacionado (opcional). */
    @Column
    private Long betaItemId;

    /** ID Minecraft do item que tava sendo testado quando o bug aconteceu. */
    @Column(length = 256)
    private String itemId;

    @Column(length = 256, nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    /** Steps pra reproduzir (markdown ou texto livre). */
    @Column(columnDefinition = "TEXT")
    private String stepsToReproduce;

    /** Como o tester simulou/encontrou o bug (texto livre). */
    @Column(columnDefinition = "TEXT")
    private String howFound;

    /** Versão do mod testada quando o bug aconteceu (ex: v89). */
    @Column(length = 32)
    private String modVersion;

    /** Versão do MC (ex: 1.20.1). */
    @Column(length = 32)
    private String mcVersion;

    /** Nome do mundo / dimensão onde ocorreu (ex: "overworld", "the_nether"). */
    @Column(length = 128)
    private String worldContext;

    /** URL pra screenshot/vídeo do bug. */
    @Column(length = 512)
    private String screenshotUrl;

    /** Severidade declarada pelo tester (low/medium/high/critical). */
    @Column(length = 16)
    private String severity;

    /** Frequência: sempre, frequente, raro, único. */
    @Column(length = 16)
    private String frequency;

    /** Prioridade sugerida (low/medium/high/blocker). Admin pode reclassificar. */
    @Column(length = 16)
    private String priority;

    /** O tester conseguiu reproduzir o bug consistentemente? */
    @Column(nullable = false)
    private boolean canReplicate = false;

    /** Afeta outros players ou só o reporter? */
    @Column(nullable = false)
    private boolean affectsOthers = false;

    /** Comportamento esperado (oposto ao bug). */
    @Column(columnDefinition = "TEXT")
    private String expectedBehavior;

    /** Workaround temporário descoberto pelo tester. */
    @Column(columnDefinition = "TEXT")
    private String workaround;

    /** Tags livres separadas por vírgula (ex: "crash,combat,multiplayer"). */
    @Column(length = 256)
    private String tags;

    /**
     * JSON array de mcNames que confirmaram que conseguiram replicar.
     * Permite testers ajudarem na triagem ("vi isso acontecer aqui também").
     */
    @Column(columnDefinition = "TEXT")
    private String confirmationsJson;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Status status = Status.PENDING;

    /** Pontos dados quando admin confirmou (0 se rejeitado/pendente). */
    @Column(nullable = false)
    private int pointsAwarded = 0;

    /** Nota do admin na triagem. */
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
    public String getTesterMcName() { return testerMcName; } public void setTesterMcName(String v) { testerMcName = v; }
    public Long getBetaItemId() { return betaItemId; } public void setBetaItemId(Long v) { betaItemId = v; }
    public String getItemId() { return itemId; } public void setItemId(String v) { itemId = v; }
    public String getTitle() { return title; } public void setTitle(String v) { title = v; }
    public String getDescription() { return description; } public void setDescription(String v) { description = v; }
    public String getStepsToReproduce() { return stepsToReproduce; } public void setStepsToReproduce(String v) { stepsToReproduce = v; }
    public String getHowFound() { return howFound; } public void setHowFound(String v) { howFound = v; }
    public String getModVersion() { return modVersion; } public void setModVersion(String v) { modVersion = v; }
    public String getMcVersion() { return mcVersion; } public void setMcVersion(String v) { mcVersion = v; }
    public String getWorldContext() { return worldContext; } public void setWorldContext(String v) { worldContext = v; }
    public String getScreenshotUrl() { return screenshotUrl; } public void setScreenshotUrl(String v) { screenshotUrl = v; }
    public String getSeverity() { return severity; } public void setSeverity(String v) { severity = v; }
    public String getFrequency() { return frequency; } public void setFrequency(String v) { frequency = v; }
    public String getPriority() { return priority; } public void setPriority(String v) { priority = v; }
    public boolean isCanReplicate() { return canReplicate; } public void setCanReplicate(boolean v) { canReplicate = v; }
    public boolean isAffectsOthers() { return affectsOthers; } public void setAffectsOthers(boolean v) { affectsOthers = v; }
    public String getExpectedBehavior() { return expectedBehavior; } public void setExpectedBehavior(String v) { expectedBehavior = v; }
    public String getWorkaround() { return workaround; } public void setWorkaround(String v) { workaround = v; }
    public String getTags() { return tags; } public void setTags(String v) { tags = v; }
    public String getConfirmationsJson() { return confirmationsJson; } public void setConfirmationsJson(String v) { confirmationsJson = v; }
    public Status getStatus() { return status; } public void setStatus(Status v) { status = v; }
    public int getPointsAwarded() { return pointsAwarded; } public void setPointsAwarded(int v) { pointsAwarded = v; }
    public String getAdminNote() { return adminNote; } public void setAdminNote(String v) { adminNote = v; }
    public String getTriagedBy() { return triagedBy; } public void setTriagedBy(String v) { triagedBy = v; }
    public Instant getTriagedAt() { return triagedAt; } public void setTriagedAt(Instant v) { triagedAt = v; }
    public Instant getCreatedAt() { return createdAt; }
}
