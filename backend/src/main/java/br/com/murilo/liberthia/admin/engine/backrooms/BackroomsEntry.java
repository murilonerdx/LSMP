package br.com.murilo.liberthia.admin.engine.backrooms;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * Rastreia entradas e saídas em dimensões backrooms (level_0, level_1, ...).
 * Mod faz POST quando dimension change é detectado.
 */
@Entity
@Table(name = "backrooms_entries", indexes = {
        @Index(name = "idx_back_player_ts", columnList = "playerUuid, occurredAt DESC"),
        @Index(name = "idx_back_level", columnList = "levelId"),
        @Index(name = "idx_back_ts", columnList = "occurredAt DESC")
})
public class BackroomsEntry {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(length = 64) private String playerUuid;
    @Column(length = 64) private String playerName;
    @Column(length = 96) private String levelId;         // ex "backrooms:level_5"
    @Column(length = 16) private String eventType;       // "enter" | "exit"
    @Column private Instant occurredAt;
    @Column private double x, y, z;

    public Long getId() { return id; }
    public String getPlayerUuid() { return playerUuid; }
    public void setPlayerUuid(String v) { playerUuid = v; }
    public String getPlayerName() { return playerName; }
    public void setPlayerName(String v) { playerName = v; }
    public String getLevelId() { return levelId; }
    public void setLevelId(String v) { levelId = v; }
    public String getEventType() { return eventType; }
    public void setEventType(String v) { eventType = v; }
    public Instant getOccurredAt() { return occurredAt; }
    public void setOccurredAt(Instant v) { occurredAt = v; }
    public double getX() { return x; } public void setX(double v) { x = v; }
    public double getY() { return y; } public void setY(double v) { y = v; }
    public double getZ() { return z; } public void setZ(double v) { z = v; }
}
