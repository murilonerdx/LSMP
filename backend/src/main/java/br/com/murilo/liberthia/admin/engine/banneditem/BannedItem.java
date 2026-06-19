package br.com.murilo.liberthia.admin.engine.banneditem;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * Item banido do servidor — gerenciado 100% via painel web. Backend roda
 * scheduler periódico que dispara /clear @a {itemId} pra remover o item
 * dos inventários dos players.
 *
 * Modes:
 *   - "auto_clear" (default) — backend faz /clear @a {itemId} a cada N segundos
 *   - "broadcast"            — só anuncia no chat, não remove
 *   - "silent"               — remove sem anunciar
 */
@Entity
@Table(name = "banned_items", indexes = {
        @Index(name = "idx_banned_itemid", columnList = "itemId", unique = true)
})
public class BannedItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Ex: "minecraft:diamond_sword", "create:rotational_chassis". */
    @Column(length = 128, nullable = false, unique = true)
    private String itemId;

    /** Nome amigável (mostra no painel + no broadcast). */
    @Column(length = 128)
    private String displayName;

    @Column(length = 512)
    private String reason;

    @Column(length = 16)
    private String mode = "auto_clear"; // auto_clear | broadcast | silent

    @Column(length = 64)
    private String bannedBy;

    @Column
    private Instant bannedAt;

    /** Se true, dispara /clear no scheduler. Pode desativar sem deletar. */
    @Column
    private boolean active = true;

    /** Contador de quantas vezes o /clear já foi disparado (estatística). */
    @Column
    private long clearCount = 0;

    @PrePersist
    public void prePersist() { if (bannedAt == null) bannedAt = Instant.now(); }

    public Long getId() { return id; }
    public String getItemId() { return itemId; } public void setItemId(String v) { this.itemId = v; }
    public String getDisplayName() { return displayName; } public void setDisplayName(String v) { this.displayName = v; }
    public String getReason() { return reason; } public void setReason(String v) { this.reason = v; }
    public String getMode() { return mode; } public void setMode(String v) { this.mode = v; }
    public String getBannedBy() { return bannedBy; } public void setBannedBy(String v) { this.bannedBy = v; }
    public Instant getBannedAt() { return bannedAt; } public void setBannedAt(Instant v) { this.bannedAt = v; }
    public boolean isActive() { return active; } public void setActive(boolean v) { this.active = v; }
    public long getClearCount() { return clearCount; } public void setClearCount(long v) { this.clearCount = v; }
}
