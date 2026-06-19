package br.com.murilo.liberthia.admin.engine.quotes;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * Citação capturada do chat in-game (TalkBalloons / Comics Bubbles / chat normal).
 * Players podem votar ⬆️⬇️ pra eleger frase do dia / mês.
 */
@Entity
@Table(name = "quotes", indexes = {
        @Index(name = "idx_quotes_player_ts", columnList = "playerUuid, capturedAt DESC"),
        @Index(name = "idx_quotes_score", columnList = "score DESC"),
        @Index(name = "idx_quotes_ts", columnList = "capturedAt DESC")
})
public class Quote {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(length = 64) private String playerUuid;
    @Column(length = 64) private String playerName;
    @Column(length = 1024) private String text;
    @Column private Instant capturedAt;
    @Column private int upvotes = 0;
    @Column private int downvotes = 0;
    @Column private int score = 0;          // upvotes - downvotes (cached)
    @Column(length = 64) private String dimension;
    @Column private double x, y, z;

    public Long getId() { return id; }
    public String getPlayerUuid() { return playerUuid; }
    public void setPlayerUuid(String v) { playerUuid = v; }
    public String getPlayerName() { return playerName; }
    public void setPlayerName(String v) { playerName = v; }
    public String getText() { return text; }
    public void setText(String v) { text = v; }
    public Instant getCapturedAt() { return capturedAt; }
    public void setCapturedAt(Instant v) { capturedAt = v; }
    public int getUpvotes() { return upvotes; }
    public void setUpvotes(int v) { upvotes = v; }
    public int getDownvotes() { return downvotes; }
    public void setDownvotes(int v) { downvotes = v; }
    public int getScore() { return score; }
    public void setScore(int v) { score = v; }
    public String getDimension() { return dimension; }
    public void setDimension(String v) { dimension = v; }
    public double getX() { return x; }
    public void setX(double v) { x = v; }
    public double getY() { return y; }
    public void setY(double v) { y = v; }
    public double getZ() { return z; }
    public void setZ(double v) { z = v; }
}
