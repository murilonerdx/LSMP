package br.com.murilo.liberthia.admin.tester;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Inscrição no processo seletivo de mod tester.
 * Sem auth — qualquer pessoa pode se inscrever via {@code /tester/apply}.
 *
 * Fluxo:
 *  1. Pessoa preenche form → cria TesterApplication com status=PENDING
 *  2. Admin avalia → APPROVED ou REJECTED
 *  3. Se APPROVED, sistema gera InviteCode auto-vinculado → admin manda código pro inscrito
 *  4. Pessoa usa código pra criar conta de tester
 */
@Entity
@Table(name = "tester_applications", indexes = {
        @Index(name = "idx_app_mc", columnList = "mcName"),
        @Index(name = "idx_app_status", columnList = "status")
})
public class TesterApplication {

    public enum Status { PENDING, APPROVED, REJECTED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Nome real declarado. */
    @Column(length = 128, nullable = false)
    private String realName;

    /** Nick do Minecraft. */
    @Column(length = 64, nullable = false)
    private String mcName;

    /** Discord ou contato opcional. */
    @Column(length = 128)
    private String contact;

    /** Resposta: disponibilidade semanal. */
    @Column(nullable = false)
    private boolean weeklyAvailability;

    /** Resposta: por que quer ser tester (texto livre). */
    @Column(columnDefinition = "TEXT", nullable = false)
    private String motivation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Status status = Status.PENDING;

    /** Nota do admin na avaliação. */
    @Column(columnDefinition = "TEXT")
    private String adminNote;

    /** Quando o admin avaliou. */
    @Column
    private Instant reviewedAt;

    @Column(length = 64)
    private String reviewedBy;

    /** Código de convite gerado ao aprovar (auto-link). */
    @Column(length = 32)
    private String generatedInviteCode;

    @Column(nullable = false)
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getRealName() { return realName; } public void setRealName(String v) { realName = v; }
    public String getMcName() { return mcName; } public void setMcName(String v) { mcName = v; }
    public String getContact() { return contact; } public void setContact(String v) { contact = v; }
    public boolean isWeeklyAvailability() { return weeklyAvailability; }
    public void setWeeklyAvailability(boolean v) { weeklyAvailability = v; }
    public String getMotivation() { return motivation; } public void setMotivation(String v) { motivation = v; }
    public Status getStatus() { return status; } public void setStatus(Status v) { status = v; }
    public String getAdminNote() { return adminNote; } public void setAdminNote(String v) { adminNote = v; }
    public Instant getReviewedAt() { return reviewedAt; } public void setReviewedAt(Instant v) { reviewedAt = v; }
    public String getReviewedBy() { return reviewedBy; } public void setReviewedBy(String v) { reviewedBy = v; }
    public String getGeneratedInviteCode() { return generatedInviteCode; }
    public void setGeneratedInviteCode(String v) { generatedInviteCode = v; }
    public Instant getCreatedAt() { return createdAt; }
}
