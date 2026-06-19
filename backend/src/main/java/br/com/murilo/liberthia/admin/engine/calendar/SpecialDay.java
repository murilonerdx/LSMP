package br.com.murilo.liberthia.admin.engine.calendar;

import jakarta.persistence.*;

@Entity
@Table(name = "calendar_special_days")
public class SpecialDay {

    @Id
    @Column(length = 64)
    private String id;

    private int dayOfYear;

    @Column(length = 128) private String name;
    @Column(length = 16) private String emoji;
    @Column(length = 16) private String color;

    @Column(columnDefinition = "TEXT") private String description;
    @Column(columnDefinition = "TEXT") private String tellraw;
    @Column(length = 128) private String sound;
    @Column(length = 16) private String weather; // "" | clear | rain | thunder
    private int setTimeTo; // -1 = não muda

    @Column(columnDefinition = "TEXT")
    private String effectsJson;  // [{effect, durationSec, amplifier}]

    @Column(columnDefinition = "TEXT")
    private String customCmd;

    /** ms desde epoch da última vez que disparou */
    private Long lastTriggerTs;

    public String getId() { return id; } public void setId(String v) { this.id = v; }
    public int getDayOfYear() { return dayOfYear; } public void setDayOfYear(int v) { this.dayOfYear = v; }
    public String getName() { return name; } public void setName(String v) { this.name = v; }
    public String getEmoji() { return emoji; } public void setEmoji(String v) { this.emoji = v; }
    public String getColor() { return color; } public void setColor(String v) { this.color = v; }
    public String getDescription() { return description; } public void setDescription(String v) { this.description = v; }
    public String getTellraw() { return tellraw; } public void setTellraw(String v) { this.tellraw = v; }
    public String getSound() { return sound; } public void setSound(String v) { this.sound = v; }
    public String getWeather() { return weather; } public void setWeather(String v) { this.weather = v; }
    public int getSetTimeTo() { return setTimeTo; } public void setSetTimeTo(int v) { this.setTimeTo = v; }
    public String getEffectsJson() { return effectsJson; } public void setEffectsJson(String v) { this.effectsJson = v; }
    public String getCustomCmd() { return customCmd; } public void setCustomCmd(String v) { this.customCmd = v; }
    public Long getLastTriggerTs() { return lastTriggerTs; } public void setLastTriggerTs(Long v) { this.lastTriggerTs = v; }
}
