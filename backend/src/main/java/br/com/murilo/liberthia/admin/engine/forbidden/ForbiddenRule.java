package br.com.murilo.liberthia.admin.engine.forbidden;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * Regra de palavra proibida. Persistida em PostgreSQL.
 * `consequencesJson` armazena a lista de actions como JSON cru (mantém a flexibilidade
 * de adicionar novos tipos de consequência sem mudar schema).
 */
@Entity
@Table(name = "forbidden_rules")
public class ForbiddenRule {

    @Id
    @Column(length = 64)
    private String id;

    @Column(length = 16)
    private String emoji;

    @Column(length = 128)
    private String name;

    @Column(length = 256)
    private String pattern;

    /** exact | contains | regex */
    @Column(length = 16)
    private String matchMode;

    private boolean caseSensitive;

    /** speaker | everyone | both */
    @Column(length = 16)
    private String target;

    @Column(columnDefinition = "TEXT")
    private String consequencesJson;

    private int cooldownSec;

    private boolean enabled;

    private long triggered;

    private Instant updatedAt;

    public ForbiddenRule() {}

    @PrePersist
    @PreUpdate
    protected void touch() {
        this.updatedAt = Instant.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getEmoji() { return emoji; }
    public void setEmoji(String emoji) { this.emoji = emoji; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPattern() { return pattern; }
    public void setPattern(String pattern) { this.pattern = pattern; }
    public String getMatchMode() { return matchMode; }
    public void setMatchMode(String matchMode) { this.matchMode = matchMode; }
    public boolean isCaseSensitive() { return caseSensitive; }
    public void setCaseSensitive(boolean caseSensitive) { this.caseSensitive = caseSensitive; }
    public String getTarget() { return target; }
    public void setTarget(String target) { this.target = target; }
    public String getConsequencesJson() { return consequencesJson; }
    public void setConsequencesJson(String consequencesJson) { this.consequencesJson = consequencesJson; }
    public int getCooldownSec() { return cooldownSec; }
    public void setCooldownSec(int cooldownSec) { this.cooldownSec = cooldownSec; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public long getTriggered() { return triggered; }
    public void setTriggered(long triggered) { this.triggered = triggered; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
