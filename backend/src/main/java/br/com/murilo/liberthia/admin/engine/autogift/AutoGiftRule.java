package br.com.murilo.liberthia.admin.engine.autogift;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * Regra de auto-gift por cron — sistema que premia os top N players de
 * alguma métrica (voice, chat, etc) numa periodicidade configurada.
 *
 * Fluxo:
 *   1. Admin cria regra: "Top 3 voice minutes semanal → diamantes"
 *   2. Scheduler dispara conforme `nextRunAt`
 *   3. Calcula ranking pela métrica
 *   4. Executa rewards (give + commands) pros top N
 *   5. Loga em AutoGiftRun
 *   6. Atualiza nextRunAt
 */
@Entity
@Table(name = "auto_gift_rules", indexes = {
        @Index(name = "idx_autogift_active", columnList = "active"),
        @Index(name = "idx_autogift_next", columnList = "nextRunAt")
})
public class AutoGiftRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 128, nullable = false)
    private String name;

    @Column(length = 512)
    private String description;

    @Column
    private boolean active = true;

    /** Métrica de ranking. Valores:
     *  VOICE_DURATION_MS, VOICE_CLIPS, VOICE_BYTES,
     *  CHAT_MESSAGES, LOGIN_COUNT, DEATHS, INVENTORY_ITEMS */
    @Column(length = 32, nullable = false)
    private String metric = "VOICE_DURATION_MS";

    /** Quantos top players ganham. */
    @Column
    private int topN = 3;

    /**
     * Periodicidade. Aceita presets: "hourly", "every6h", "daily", "weekly",
     * "monthly", OU string cron (futuro). Default daily 12:00 UTC.
     */
    @Column(length = 64, nullable = false)
    private String schedule = "daily";

    /**
     * JSON array de rewards. Cada reward:
     *  {"type":"item","id":"minecraft:diamond","count":5,"nbt":""}
     *  {"type":"command","value":"give {player} netherite_ingot 1"}
     *  {"type":"broadcast","value":"🎁 {player} ganhou top voice!"}
     */
    @Column(columnDefinition = "TEXT", nullable = false)
    private String rewardsJson = "[]";

    /** Se true, escalona rewards pelos top (1º ganha mais, 2º menos, etc). */
    @Column
    private boolean scaleByRank = false;

    /** Critério mínimo pra ser elegível (ex: minMs=60000 = só quem falou >1min). */
    @Column
    private long minValue = 0;

    @Column
    private Instant lastRunAt;

    @Column
    private Instant nextRunAt;

    @Column
    private long runCount = 0;

    @Column(length = 64)
    private String createdBy;

    @Column
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
        if (nextRunAt == null) nextRunAt = computeNextRunAt(Instant.now());
    }

    /** Calcula próxima execução a partir de `from`. */
    public Instant computeNextRunAt(Instant from) {
        return switch (schedule == null ? "daily" : schedule.toLowerCase()) {
            case "every_minute" -> from.plusSeconds(60);
            case "hourly" -> from.plusSeconds(3600);
            case "every6h" -> from.plusSeconds(6 * 3600);
            case "every12h" -> from.plusSeconds(12 * 3600);
            case "daily" -> from.plusSeconds(24 * 3600);
            case "weekly" -> from.plusSeconds(7 * 24 * 3600);
            case "monthly" -> from.plusSeconds(30 * 24 * 3600);
            default -> from.plusSeconds(24 * 3600);
        };
    }

    public Long getId() { return id; }
    public String getName() { return name; } public void setName(String v) { this.name = v; }
    public String getDescription() { return description; } public void setDescription(String v) { this.description = v; }
    public boolean isActive() { return active; } public void setActive(boolean v) { this.active = v; }
    public String getMetric() { return metric; } public void setMetric(String v) { this.metric = v; }
    public int getTopN() { return topN; } public void setTopN(int v) { this.topN = v; }
    public String getSchedule() { return schedule; } public void setSchedule(String v) { this.schedule = v; }
    public String getRewardsJson() { return rewardsJson; } public void setRewardsJson(String v) { this.rewardsJson = v; }
    public boolean isScaleByRank() { return scaleByRank; } public void setScaleByRank(boolean v) { this.scaleByRank = v; }
    public long getMinValue() { return minValue; } public void setMinValue(long v) { this.minValue = v; }
    public Instant getLastRunAt() { return lastRunAt; } public void setLastRunAt(Instant v) { this.lastRunAt = v; }
    public Instant getNextRunAt() { return nextRunAt; } public void setNextRunAt(Instant v) { this.nextRunAt = v; }
    public long getRunCount() { return runCount; } public void setRunCount(long v) { this.runCount = v; }
    public String getCreatedBy() { return createdBy; } public void setCreatedBy(String v) { this.createdBy = v; }
    public Instant getCreatedAt() { return createdAt; } public void setCreatedAt(Instant v) { this.createdAt = v; }
}
