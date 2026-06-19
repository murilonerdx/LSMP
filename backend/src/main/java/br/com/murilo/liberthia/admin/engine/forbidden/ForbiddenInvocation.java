package br.com.murilo.liberthia.admin.engine.forbidden;

import jakarta.persistence.*;

import java.time.Instant;

/** Histórico de invocações (palavra proibida disparada). */
@Entity
@Table(name = "forbidden_invocations", indexes = {
        @Index(name = "idx_inv_ts", columnList = "ts DESC"),
        @Index(name = "idx_inv_rule", columnList = "ruleId")
})
public class ForbiddenInvocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Instant ts;

    @Column(length = 64)
    private String speakerUuid;

    @Column(length = 64)
    private String speakerName;

    @Column(length = 64)
    private String ruleId;

    @Column(length = 128)
    private String ruleName;

    @Column(length = 256)
    private String word;

    public ForbiddenInvocation() {}

    public ForbiddenInvocation(String speakerUuid, String speakerName, String ruleId, String ruleName, String word) {
        this.ts = Instant.now();
        this.speakerUuid = speakerUuid;
        this.speakerName = speakerName;
        this.ruleId = ruleId;
        this.ruleName = ruleName;
        this.word = word;
    }

    public Long getId() { return id; }
    public Instant getTs() { return ts; }
    public String getSpeakerUuid() { return speakerUuid; }
    public String getSpeakerName() { return speakerName; }
    public String getRuleId() { return ruleId; }
    public String getRuleName() { return ruleName; }
    public String getWord() { return word; }
}
