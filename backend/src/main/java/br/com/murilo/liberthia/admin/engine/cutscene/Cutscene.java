package br.com.murilo.liberthia.admin.engine.cutscene;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * Cutscene agendada — roda em data/hora específica mesmo sem o painel aberto.
 * Steps são executados sequencialmente no backend via thread dedicada.
 */
@Entity
@Table(name = "cutscenes")
public class Cutscene {

    @Id
    @Column(length = 64)
    private String id;

    @Column(length = 16) private String emoji;
    @Column(length = 128) private String name;
    @Column(columnDefinition = "TEXT") private String description;

    /** Steps em JSON: [{type: tp|title|sound|particle|effect|spawn_mob|chat|command|lightning|wait, ...}] */
    @Column(columnDefinition = "TEXT")
    private String stepsJson;

    /** uuid de player, "@a", "@first" — usado pra resolver placeholder @s nos steps */
    @Column(length = 64)
    private String targetSelector;

    /** Quando agendar (null = manual). */
    private Instant scheduledAt;

    /** "once" | "daily" | "interval" */
    @Column(length = 16)
    private String scheduleMode;

    /** Para "interval": segundos entre execuções */
    private Integer intervalSec;

    /** Para "daily": HH (0-23) e MM (0-59) */
    private Integer dailyHour;
    private Integer dailyMinute;

    private boolean enabled;

    private Instant lastRunAt;
    private long runCount;

    public String getId() { return id; } public void setId(String v) { this.id = v; }
    public String getEmoji() { return emoji; } public void setEmoji(String v) { this.emoji = v; }
    public String getName() { return name; } public void setName(String v) { this.name = v; }
    public String getDescription() { return description; } public void setDescription(String v) { this.description = v; }
    public String getStepsJson() { return stepsJson; } public void setStepsJson(String v) { this.stepsJson = v; }
    public String getTargetSelector() { return targetSelector; } public void setTargetSelector(String v) { this.targetSelector = v; }
    public Instant getScheduledAt() { return scheduledAt; } public void setScheduledAt(Instant v) { this.scheduledAt = v; }
    public String getScheduleMode() { return scheduleMode; } public void setScheduleMode(String v) { this.scheduleMode = v; }
    public Integer getIntervalSec() { return intervalSec; } public void setIntervalSec(Integer v) { this.intervalSec = v; }
    public Integer getDailyHour() { return dailyHour; } public void setDailyHour(Integer v) { this.dailyHour = v; }
    public Integer getDailyMinute() { return dailyMinute; } public void setDailyMinute(Integer v) { this.dailyMinute = v; }
    public boolean isEnabled() { return enabled; } public void setEnabled(boolean v) { this.enabled = v; }
    public Instant getLastRunAt() { return lastRunAt; } public void setLastRunAt(Instant v) { this.lastRunAt = v; }
    public long getRunCount() { return runCount; } public void setRunCount(long v) { this.runCount = v; }
}
