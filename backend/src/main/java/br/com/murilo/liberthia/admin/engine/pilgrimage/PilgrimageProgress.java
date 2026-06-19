package br.com.murilo.liberthia.admin.engine.pilgrimage;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * Progresso por player em uma peregrinação. Singleton por (player, pilgrimage)
 * — quando completa, mantém o registro com completedAt setado.
 */
@Entity
@Table(name = "pilgrimage_progress", indexes = {
        @Index(name = "idx_pg_player", columnList = "playerUuid"),
        @Index(name = "idx_pg_pil", columnList = "pilgrimageId")
})
public class PilgrimageProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 64)
    private String playerUuid;

    @Column(length = 64)
    private String playerName;

    @Column(length = 64)
    private String pilgrimageId;

    /** Próxima estação que o player precisa visitar (0-indexed). */
    private int currentStep;

    private Instant startedAt;

    /** null = em andamento; not null = completou. */
    private Instant completedAt;

    public PilgrimageProgress() {}
    public PilgrimageProgress(String playerUuid, String playerName, String pilgrimageId) {
        this.playerUuid = playerUuid;
        this.playerName = playerName;
        this.pilgrimageId = pilgrimageId;
        this.currentStep = 0;
        this.startedAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getPlayerUuid() { return playerUuid; } public void setPlayerUuid(String v) { this.playerUuid = v; }
    public String getPlayerName() { return playerName; } public void setPlayerName(String v) { this.playerName = v; }
    public String getPilgrimageId() { return pilgrimageId; } public void setPilgrimageId(String v) { this.pilgrimageId = v; }
    public int getCurrentStep() { return currentStep; } public void setCurrentStep(int v) { this.currentStep = v; }
    public Instant getStartedAt() { return startedAt; } public void setStartedAt(Instant v) { this.startedAt = v; }
    public Instant getCompletedAt() { return completedAt; } public void setCompletedAt(Instant v) { this.completedAt = v; }
}
