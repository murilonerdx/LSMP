package br.com.murilo.liberthia.admin.engine.echoes;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "memory_echoes_active")
public class MemoryEchoActive {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 64) private String echoId;
    @Column(length = 64) private String playerUuid;
    private Instant startedAt;
    private Instant endsAt;
    private Instant lastWhisper;
    private int whisperIdx;

    public Long getId() { return id; }
    public String getEchoId() { return echoId; } public void setEchoId(String v) { this.echoId = v; }
    public String getPlayerUuid() { return playerUuid; } public void setPlayerUuid(String v) { this.playerUuid = v; }
    public Instant getStartedAt() { return startedAt; } public void setStartedAt(Instant v) { this.startedAt = v; }
    public Instant getEndsAt() { return endsAt; } public void setEndsAt(Instant v) { this.endsAt = v; }
    public Instant getLastWhisper() { return lastWhisper; } public void setLastWhisper(Instant v) { this.lastWhisper = v; }
    public int getWhisperIdx() { return whisperIdx; } public void setWhisperIdx(int v) { this.whisperIdx = v; }
}
