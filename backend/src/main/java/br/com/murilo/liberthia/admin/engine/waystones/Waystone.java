package br.com.murilo.liberthia.admin.engine.waystones;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * Catálogo de waystones públicas registradas pelos players.
 * Mod (ou admin) chama POST quando uma waystone é criada/destruída.
 */
@Entity
@Table(name = "waystones", indexes = {
        @Index(name = "idx_ws_owner", columnList = "ownerUuid"),
        @Index(name = "idx_ws_public", columnList = "publicAccess"),
        @Index(name = "idx_ws_dim", columnList = "dimension")
})
public class Waystone {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(length = 128) private String name;
    @Column(length = 64) private String ownerUuid;
    @Column(length = 64) private String ownerName;
    @Column(length = 64) private String dimension;
    @Column private int x, y, z;
    @Column private boolean publicAccess = true;
    @Column private int useCount = 0;
    @Column private Instant createdAt;
    @Column private Instant updatedAt;

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String v) { name = v; }
    public String getOwnerUuid() { return ownerUuid; }
    public void setOwnerUuid(String v) { ownerUuid = v; }
    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String v) { ownerName = v; }
    public String getDimension() { return dimension; }
    public void setDimension(String v) { dimension = v; }
    public int getX() { return x; } public void setX(int v) { x = v; }
    public int getY() { return y; } public void setY(int v) { y = v; }
    public int getZ() { return z; } public void setZ(int v) { z = v; }
    public boolean isPublicAccess() { return publicAccess; }
    public void setPublicAccess(boolean v) { publicAccess = v; }
    public int getUseCount() { return useCount; }
    public void setUseCount(int v) { useCount = v; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant v) { createdAt = v; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant v) { updatedAt = v; }
}
