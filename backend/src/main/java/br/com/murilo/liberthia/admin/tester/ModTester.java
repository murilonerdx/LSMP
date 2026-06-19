package br.com.murilo.liberthia.admin.tester;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Conta de mod tester (separada do admin). Identificado pelo nome
 * Minecraft (mcName). Senha hash salva em {@link #passwordHash}.
 *
 * Pontos somam quando admin confirma bug report válido.
 */
@Entity
@Table(name = "mod_testers", indexes = {
        @Index(name = "idx_tester_mc", columnList = "mcName", unique = true)
})
public class ModTester {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 64, nullable = false, unique = true)
    private String mcName;

    /** Hash SHA-256 com salt (formato: base64salt$base64hash). */
    @Column(length = 256, nullable = false)
    private String passwordHash;

    /** Código de convite que esse tester usou (auditoria). */
    @Column(length = 64)
    private String inviteCodeUsed;

    @Column(nullable = false)
    private int points = 0;

    @Column(nullable = false)
    private Instant createdAt;

    @Column
    private Instant lastLoginAt;

    /** Permite desabilitar tester sem deletar conta. */
    @Column(nullable = false)
    private boolean enabled = true;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getMcName() { return mcName; }
    public void setMcName(String v) { mcName = v; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String v) { passwordHash = v; }
    public String getInviteCodeUsed() { return inviteCodeUsed; }
    public void setInviteCodeUsed(String v) { inviteCodeUsed = v; }
    public int getPoints() { return points; }
    public void setPoints(int v) { points = v; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(Instant v) { lastLoginAt = v; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean v) { enabled = v; }
}
