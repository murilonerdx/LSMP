package br.com.murilo.liberthia.admin.tester;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Registro de cada vez que um tester resgatou uma recompensa.
 * Tester gasta pontos → cria redemption (delivered=false) → admin marca delivered
 * (após executar comando in-game) ou o sistema executa via ModBridgeClient.
 */
@Entity
@Table(name = "tester_reward_redemptions", indexes = {
        @Index(name = "idx_redempt_tester", columnList = "testerMcName"),
        @Index(name = "idx_redempt_reward", columnList = "rewardId")
})
public class RewardRedemption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 64, nullable = false)
    private String testerMcName;

    @Column(nullable = false)
    private Long rewardId;

    /** Snapshot do nome/custo no momento do resgate (caso reward seja editada). */
    @Column(length = 128)
    private String rewardSnapshot;

    @Column(nullable = false)
    private int pointsSpent;

    @Column(nullable = false)
    private boolean delivered = false;

    /** Comando que foi/será executado. */
    @Column(length = 512)
    private String commandRun;

    @Column(columnDefinition = "TEXT")
    private String deliveryNote;

    @Column(nullable = false)
    private Instant redeemedAt;

    @Column
    private Instant deliveredAt;

    @PrePersist
    public void prePersist() {
        if (redeemedAt == null) redeemedAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getTesterMcName() { return testerMcName; } public void setTesterMcName(String v) { testerMcName = v; }
    public Long getRewardId() { return rewardId; } public void setRewardId(Long v) { rewardId = v; }
    public String getRewardSnapshot() { return rewardSnapshot; } public void setRewardSnapshot(String v) { rewardSnapshot = v; }
    public int getPointsSpent() { return pointsSpent; } public void setPointsSpent(int v) { pointsSpent = v; }
    public boolean isDelivered() { return delivered; } public void setDelivered(boolean v) { delivered = v; }
    public String getCommandRun() { return commandRun; } public void setCommandRun(String v) { commandRun = v; }
    public String getDeliveryNote() { return deliveryNote; } public void setDeliveryNote(String v) { deliveryNote = v; }
    public Instant getRedeemedAt() { return redeemedAt; }
    public Instant getDeliveredAt() { return deliveredAt; } public void setDeliveredAt(Instant v) { deliveredAt = v; }
}
