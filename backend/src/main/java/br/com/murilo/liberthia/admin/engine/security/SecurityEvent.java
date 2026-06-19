package br.com.murilo.liberthia.admin.engine.security;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * Evento de segurança capturado de mods como SecurityCraft, Corpse, etc.
 * Mod loga via POST /api/security/event quando detecta:
 *  - Tentativa de senha falha
 *  - Quebra de bloco protegido
 *  - Acesso a chest com lock
 *  - Camera ativa detectou movimento
 */
@Entity
@Table(name = "security_events", indexes = {
        @Index(name = "idx_sec_ts", columnList = "occurredAt DESC"),
        @Index(name = "idx_sec_intruder", columnList = "intruderUuid"),
        @Index(name = "idx_sec_owner", columnList = "ownerUuid"),
        @Index(name = "idx_sec_kind", columnList = "kind")
})
public class SecurityEvent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(length = 32) private String kind;       // "password_fail", "block_break_protected", "chest_lock_break", "camera_motion"
    @Column(length = 64) private String intruderUuid;
    @Column(length = 64) private String intruderName;
    @Column(length = 64) private String ownerUuid;
    @Column(length = 64) private String ownerName;
    @Column(length = 64) private String dimension;
    @Column private int x, y, z;
    @Column(length = 256) private String blockTypeId;
    @Column(length = 512) private String detail;
    @Column private Instant occurredAt;

    public Long getId() { return id; }
    public String getKind() { return kind; }
    public void setKind(String v) { kind = v; }
    public String getIntruderUuid() { return intruderUuid; }
    public void setIntruderUuid(String v) { intruderUuid = v; }
    public String getIntruderName() { return intruderName; }
    public void setIntruderName(String v) { intruderName = v; }
    public String getOwnerUuid() { return ownerUuid; }
    public void setOwnerUuid(String v) { ownerUuid = v; }
    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String v) { ownerName = v; }
    public String getDimension() { return dimension; }
    public void setDimension(String v) { dimension = v; }
    public int getX() { return x; } public void setX(int v) { x = v; }
    public int getY() { return y; } public void setY(int v) { y = v; }
    public int getZ() { return z; } public void setZ(int v) { z = v; }
    public String getBlockTypeId() { return blockTypeId; }
    public void setBlockTypeId(String v) { blockTypeId = v; }
    public String getDetail() { return detail; }
    public void setDetail(String v) { detail = v; }
    public Instant getOccurredAt() { return occurredAt; }
    public void setOccurredAt(Instant v) { occurredAt = v; }
}
