package br.com.murilo.liberthia.admin.tester;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Voto genérico (like/dislike) de um tester em qualquer target votável.
 *
 * Targets suportados:
 *  - BETA_ITEM  → BetaItem.id
 *  - BETA_AUDIO → BetaAudio.id
 *  - MODEL_3D   → Model3D.id (futuro)
 *
 * Unique constraint garante 1 voto por tester+target. Toggle: clicar de
 * novo no mesmo botão remove o voto (não troca pro outro). Clicar no
 * outro botão troca o voto.
 */
@Entity
@Table(name = "tester_beta_votes", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"voter_mc_name", "target_type", "target_id"})
}, indexes = {
        @Index(name = "idx_vote_target", columnList = "target_type, target_id"),
        @Index(name = "idx_vote_voter", columnList = "voter_mc_name")
})
public class BetaVote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "voter_mc_name", length = 32, nullable = false)
    private String voterMcName;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", length = 16, nullable = false)
    private Target target;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @Enumerated(EnumType.STRING)
    @Column(length = 8, nullable = false)
    private Vote vote;

    @Column(nullable = false)
    private Instant votedAt;

    public enum Target { BETA_ITEM, BETA_AUDIO, MODEL_3D }
    public enum Vote { LIKE, DISLIKE }

    @PrePersist
    public void prePersist() {
        if (votedAt == null) votedAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getVoterMcName() { return voterMcName; } public void setVoterMcName(String v) { voterMcName = v; }
    public Target getTarget() { return target; } public void setTarget(Target v) { target = v; }
    public Long getTargetId() { return targetId; } public void setTargetId(Long v) { targetId = v; }
    public Vote getVote() { return vote; } public void setVote(Vote v) { vote = v; }
    public Instant getVotedAt() { return votedAt; } public void setVotedAt(Instant v) { votedAt = v; }
}
