package br.com.murilo.liberthia.admin.engine.possession;

import jakarta.persistence.*;

import java.time.Instant;

/** Possessão ativa em um player. */
@Entity
@Table(name = "possession_active")
public class PossessionActive {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 64)
    private String playerUuid;

    @Column(length = 64)
    private String playerName;

    @Column(length = 64)
    private String entityId;

    private Instant startedAt;
    private Instant endsAt;
    private Instant lastSpeak;

    public PossessionActive() {}

    public Long getId() { return id; }
    public String getPlayerUuid() { return playerUuid; }
    public void setPlayerUuid(String playerUuid) { this.playerUuid = playerUuid; }
    public String getPlayerName() { return playerName; }
    public void setPlayerName(String playerName) { this.playerName = playerName; }
    public String getEntityId() { return entityId; }
    public void setEntityId(String entityId) { this.entityId = entityId; }
    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }
    public Instant getEndsAt() { return endsAt; }
    public void setEndsAt(Instant endsAt) { this.endsAt = endsAt; }
    public Instant getLastSpeak() { return lastSpeak; }
    public void setLastSpeak(Instant lastSpeak) { this.lastSpeak = lastSpeak; }
}
