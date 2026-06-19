package br.com.murilo.liberthia.admin.tester;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Código de convite gerado pelo admin pra liberar registro de tester.
 * Cada código usa-uma-vez: ao ser usado, marca {@link #usedBy} e
 * {@link #usedAt} e não pode mais ser reutilizado.
 */
@Entity
@Table(name = "tester_invite_codes", indexes = {
        @Index(name = "idx_invite_code", columnList = "code", unique = true)
})
public class InviteCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 32, nullable = false, unique = true)
    private String code;

    /** Quem criou o código (admin nome ou "admin"). */
    @Column(length = 64)
    private String createdBy;

    /** mcName do tester que usou; null = ainda não usado. */
    @Column(length = 64)
    private String usedBy;

    @Column(nullable = false)
    private Instant createdAt;

    @Column
    private Instant usedAt;

    /** Nota livre (ex: "pra fulano testar beta do mod X"). */
    @Column(length = 256)
    private String note;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
    }

    public boolean isUsed() { return usedBy != null && !usedBy.isBlank(); }

    public Long getId() { return id; }
    public String getCode() { return code; }
    public void setCode(String v) { code = v; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String v) { createdBy = v; }
    public String getUsedBy() { return usedBy; }
    public void setUsedBy(String v) { usedBy = v; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUsedAt() { return usedAt; }
    public void setUsedAt(Instant v) { usedAt = v; }
    public String getNote() { return note; }
    public void setNote(String v) { note = v; }
}
