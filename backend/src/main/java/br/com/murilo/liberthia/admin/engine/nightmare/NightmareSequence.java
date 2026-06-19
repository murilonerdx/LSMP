package br.com.murilo.liberthia.admin.engine.nightmare;

import jakarta.persistence.*;

@Entity
@Table(name = "nightmare_sequences")
public class NightmareSequence {

    @Id
    @Column(length = 64)
    private String id;

    @Column(length = 16) private String emoji;
    @Column(length = 128) private String name;
    @Column(columnDefinition = "TEXT") private String description;
    @Column(columnDefinition = "TEXT") private String stepsJson;
    private boolean snapshotBefore;
    private boolean restoreAfter;

    public String getId() { return id; } public void setId(String v) { this.id = v; }
    public String getEmoji() { return emoji; } public void setEmoji(String v) { this.emoji = v; }
    public String getName() { return name; } public void setName(String v) { this.name = v; }
    public String getDescription() { return description; } public void setDescription(String v) { this.description = v; }
    public String getStepsJson() { return stepsJson; } public void setStepsJson(String v) { this.stepsJson = v; }
    public boolean isSnapshotBefore() { return snapshotBefore; } public void setSnapshotBefore(boolean v) { this.snapshotBefore = v; }
    public boolean isRestoreAfter() { return restoreAfter; } public void setRestoreAfter(boolean v) { this.restoreAfter = v; }
}
