package br.com.murilo.liberthia.admin.engine.memorial;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * Lápide virtual de player morto. Criada automaticamente quando o mod
 * detecta morte permanente (hardcore) ou manualmente via painel.
 *
 * Carrega epitáfio escrito pelo player + items que estava carregando.
 */
@Entity
@Table(name = "memorials", indexes = {
        @Index(name = "idx_mem_player", columnList = "playerUuid"),
        @Index(name = "idx_mem_ts", columnList = "diedAt DESC"),
        @Index(name = "idx_mem_perma", columnList = "permanent")
})
public class Memorial {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(length = 64) private String playerUuid;
    @Column(length = 64) private String playerName;
    @Column(length = 128) private String causeOfDeath;       // "lava", "creeper", "withering_eye", "killed by melvs"
    @Column(length = 1024) private String epitaph;
    @Column(columnDefinition = "TEXT") private String snapshotJson; // inventário/stats no momento da morte
    @Column(length = 64) private String dimension;
    @Column private int x, y, z;
    @Column private Instant diedAt;
    @Column private boolean permanent = false;               // true = hardcore / lápide física
    @Column private int reactions = 0;                       // ❤ pelos outros players

    public Long getId() { return id; }
    public String getPlayerUuid() { return playerUuid; }
    public void setPlayerUuid(String v) { playerUuid = v; }
    public String getPlayerName() { return playerName; }
    public void setPlayerName(String v) { playerName = v; }
    public String getCauseOfDeath() { return causeOfDeath; }
    public void setCauseOfDeath(String v) { causeOfDeath = v; }
    public String getEpitaph() { return epitaph; }
    public void setEpitaph(String v) { epitaph = v; }
    public String getSnapshotJson() { return snapshotJson; }
    public void setSnapshotJson(String v) { snapshotJson = v; }
    public String getDimension() { return dimension; }
    public void setDimension(String v) { dimension = v; }
    public int getX() { return x; } public void setX(int v) { x = v; }
    public int getY() { return y; } public void setY(int v) { y = v; }
    public int getZ() { return z; } public void setZ(int v) { z = v; }
    public Instant getDiedAt() { return diedAt; }
    public void setDiedAt(Instant v) { diedAt = v; }
    public boolean isPermanent() { return permanent; }
    public void setPermanent(boolean v) { permanent = v; }
    public int getReactions() { return reactions; }
    public void setReactions(int v) { reactions = v; }
}
