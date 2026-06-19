package br.com.murilo.liberthia.admin.engine.calendar;

import jakarta.persistence.*;

@Entity
@Table(name = "calendar_config")
public class CalendarConfig {

    @Id
    private Integer id = 1;

    @Column(length = 128)
    private String yearName;

    /** ms desde epoch — marca o "Dia 1" do ano cósmico (modo real) */
    private Long dayZeroTs;

    /** "real" ou "ingame" */
    @Column(length = 16)
    private String mode;

    @Column(columnDefinition = "TEXT")
    private String monthsJson;   // [{name, days, color, emoji}]

    private boolean paused;

    public Integer getId() { return id; } public void setId(Integer v) { this.id = v; }
    public String getYearName() { return yearName; } public void setYearName(String v) { this.yearName = v; }
    public Long getDayZeroTs() { return dayZeroTs; } public void setDayZeroTs(Long v) { this.dayZeroTs = v; }
    public String getMode() { return mode; } public void setMode(String v) { this.mode = v; }
    public String getMonthsJson() { return monthsJson; } public void setMonthsJson(String v) { this.monthsJson = v; }
    public boolean isPaused() { return paused; } public void setPaused(boolean v) { this.paused = v; }
}
