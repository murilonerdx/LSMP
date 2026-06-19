package br.com.murilo.liberthia.admin.engine.cutscene2;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * Cutscene cinematográfica: lista de tracks (spawn/sound/title/particle/weather/dialog/command)
 * com timestamp pra cada. Roda em sequência via /play.
 */
@Entity
@Table(name = "cutscenes_director", indexes = {
        @Index(name = "idx_cs2_name", columnList = "name"),
        @Index(name = "idx_cs2_updated", columnList = "updatedAt DESC")
})
public class Cutscene2 {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(length = 128) private String name;
    @Column(length = 512) private String description;
    @Column(columnDefinition = "TEXT") private String tracksJson; // [{type, atMs, payload:{...}}]
    @Column private int durationMs = 0;
    @Column(length = 32) private String tags;          // CSV de tags
    @Column private int plays = 0;
    @Column private Instant createdAt;
    @Column private Instant updatedAt;

    public Long getId() { return id; }
    public String getName() { return name; } public void setName(String v) { name = v; }
    public String getDescription() { return description; } public void setDescription(String v) { description = v; }
    public String getTracksJson() { return tracksJson; } public void setTracksJson(String v) { tracksJson = v; }
    public int getDurationMs() { return durationMs; } public void setDurationMs(int v) { durationMs = v; }
    public String getTags() { return tags; } public void setTags(String v) { tags = v; }
    public int getPlays() { return plays; } public void setPlays(int v) { plays = v; }
    public Instant getCreatedAt() { return createdAt; } public void setCreatedAt(Instant v) { createdAt = v; }
    public Instant getUpdatedAt() { return updatedAt; } public void setUpdatedAt(Instant v) { updatedAt = v; }
}
