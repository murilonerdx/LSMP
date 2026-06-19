package br.com.murilo.liberthia.admin.engine.choice;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "choice_history", indexes = {
        @Index(name = "idx_choice_hist_ts", columnList = "ts DESC")
})
public class ChoiceHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Instant ts;

    @Column(length = 64) private String decisionId;
    @Column(length = 256) private String decisionTitle;
    @Column(length = 64) private String playerUuid;
    @Column(length = 64) private String playerName;
    private int optionIdx;
    @Column(length = 256) private String optionText;

    public ChoiceHistory() {}
    public ChoiceHistory(String decId, String decTitle, String playerUuid, String playerName, int idx, String optText) {
        this.ts = Instant.now();
        this.decisionId = decId; this.decisionTitle = decTitle;
        this.playerUuid = playerUuid; this.playerName = playerName;
        this.optionIdx = idx; this.optionText = optText;
    }

    public Long getId() { return id; }
    public Instant getTs() { return ts; }
    public String getDecisionId() { return decisionId; }
    public String getDecisionTitle() { return decisionTitle; }
    public String getPlayerUuid() { return playerUuid; }
    public String getPlayerName() { return playerName; }
    public int getOptionIdx() { return optionIdx; }
    public String getOptionText() { return optionText; }
}
