package br.com.murilo.liberthia.admin.engine.snapshot;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * Snapshot completo de player armazenado como JSON no PostgreSQL.
 * Substitui o storage em arquivo do mod.
 */
@Entity
@Table(name = "player_snapshots", indexes = {
        @Index(name = "idx_snap_uuid_ts", columnList = "uuid, ts DESC")
})
public class PlayerSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 64)
    private String uuid;

    @Column(length = 64)
    private String name;

    private Instant ts;

    /** JSON com inventário, posição, stats, etc. */
    @Column(columnDefinition = "TEXT")
    private String dataJson;

    public PlayerSnapshot() {}
    public PlayerSnapshot(String uuid, String name, String dataJson) {
        this.uuid = uuid;
        this.name = name;
        this.ts = Instant.now();
        this.dataJson = dataJson;
    }

    public Long getId() { return id; }
    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Instant getTs() { return ts; }
    public void setTs(Instant ts) { this.ts = ts; }
    public String getDataJson() { return dataJson; }
    public void setDataJson(String dataJson) { this.dataJson = dataJson; }
}
