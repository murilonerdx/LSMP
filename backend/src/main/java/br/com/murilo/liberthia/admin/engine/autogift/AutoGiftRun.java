package br.com.murilo.liberthia.admin.engine.autogift;

import jakarta.persistence.*;

import java.time.Instant;

/** Log de cada execução de uma regra. */
@Entity
@Table(name = "auto_gift_runs", indexes = {
        @Index(name = "idx_run_rule_ts", columnList = "ruleId, ranAt DESC")
})
public class AutoGiftRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long ruleId;

    @Column(length = 128)
    private String ruleName;

    @Column
    private Instant ranAt;

    /** Total de winners (recebeu pelo menos 1 reward). */
    @Column
    private int winnersCount;

    /** JSON: array de winners com {uuid, name, score, rank}. */
    @Column(columnDefinition = "TEXT")
    private String winnersJson;

    /** JSON: array de comandos executados (pra debug/audit). */
    @Column(columnDefinition = "TEXT")
    private String commandsJson;

    @Column(length = 16)
    private String status = "SUCCESS"; // SUCCESS | NO_WINNERS | ERROR

    @Column(length = 512)
    private String errorMessage;

    @PrePersist
    public void prePersist() { if (ranAt == null) ranAt = Instant.now(); }

    public Long getId() { return id; }
    public Long getRuleId() { return ruleId; } public void setRuleId(Long v) { this.ruleId = v; }
    public String getRuleName() { return ruleName; } public void setRuleName(String v) { this.ruleName = v; }
    public Instant getRanAt() { return ranAt; } public void setRanAt(Instant v) { this.ranAt = v; }
    public int getWinnersCount() { return winnersCount; } public void setWinnersCount(int v) { this.winnersCount = v; }
    public String getWinnersJson() { return winnersJson; } public void setWinnersJson(String v) { this.winnersJson = v; }
    public String getCommandsJson() { return commandsJson; } public void setCommandsJson(String v) { this.commandsJson = v; }
    public String getStatus() { return status; } public void setStatus(String v) { this.status = v; }
    public String getErrorMessage() { return errorMessage; } public void setErrorMessage(String v) { this.errorMessage = v; }
}
