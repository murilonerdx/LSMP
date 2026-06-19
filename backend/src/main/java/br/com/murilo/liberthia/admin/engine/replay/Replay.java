package br.com.murilo.liberthia.admin.engine.replay;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * Replay = sequência gravada de comandos/eventos. Pode ser reproduzida igual
 * cutscene mas com timestamps relativos exatos. Útil pra "regravar" eventos
 * importantes pra mostrar depois.
 */
@Entity
@Table(name = "replays", indexes = {
        @Index(name = "idx_replay_name", columnList = "name")
})
public class Replay {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(length = 128) private String name;
    @Column(length = 512) private String description;
    @Column(columnDefinition = "TEXT") private String eventsJson;     // [{atMs, type, payload}]
    @Column private int durationMs = 0;
    @Column private int eventCount = 0;
    @Column private boolean recording = false;     // só pra UX
    @Column private int plays = 0;
    @Column private Instant recordedAt;
    @Column private Instant updatedAt;

    public Long getId() { return id; }
    public String getName() { return name; } public void setName(String v) { name = v; }
    public String getDescription() { return description; } public void setDescription(String v) { description = v; }
    public String getEventsJson() { return eventsJson; } public void setEventsJson(String v) { eventsJson = v; }
    public int getDurationMs() { return durationMs; } public void setDurationMs(int v) { durationMs = v; }
    public int getEventCount() { return eventCount; } public void setEventCount(int v) { eventCount = v; }
    public boolean isRecording() { return recording; } public void setRecording(boolean v) { recording = v; }
    public int getPlays() { return plays; } public void setPlays(int v) { plays = v; }
    public Instant getRecordedAt() { return recordedAt; } public void setRecordedAt(Instant v) { recordedAt = v; }
    public Instant getUpdatedAt() { return updatedAt; } public void setUpdatedAt(Instant v) { updatedAt = v; }
}
