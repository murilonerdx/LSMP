package br.com.murilo.liberthia.admin.engine.cursed;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "cursed_bindings", indexes = {
        @Index(name = "idx_bind_player", columnList = "playerUuid")
})
public class CurseBinding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 64)
    private String playerUuid;

    @Column(length = 64)
    private String playerName;

    @Column(length = 64)
    private String curseId;

    private Instant startedAt;
    private Instant lastTick;

    public Long getId() { return id; }
    public String getPlayerUuid() { return playerUuid; }
    public void setPlayerUuid(String playerUuid) { this.playerUuid = playerUuid; }
    public String getPlayerName() { return playerName; }
    public void setPlayerName(String playerName) { this.playerName = playerName; }
    public String getCurseId() { return curseId; }
    public void setCurseId(String curseId) { this.curseId = curseId; }
    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }
    public Instant getLastTick() { return lastTick; }
    public void setLastTick(Instant lastTick) { this.lastTick = lastTick; }
}
