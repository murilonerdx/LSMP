package br.com.murilo.liberthia.admin.tester;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Notificação in-app pro tester. Sistema simples:
 *  - Bug confirmado/rejeitado → notifica autor
 *  - Suggestion movida de status → notifica autor
 *  - Reward redeemada → notifica autor quando admin entregar
 *  - Buff/Nerf request status changed → notifica autor
 *  - Wiki entry referenciando seu bug → notifica autor (crédito)
 *
 * Front lê via /api/tester/notifications (paginado, sort desc).
 * Marca como lida via PATCH /api/tester/notifications/{id}.
 */
@Entity
@Table(name = "tester_notifications", indexes = {
        @Index(name = "idx_notif_recipient", columnList = "recipientMcName"),
        @Index(name = "idx_notif_read", columnList = "isRead")
})
public class TesterNotification {

    public enum Type {
        BUG_CONFIRMED, BUG_REJECTED,
        SUGGESTION_APPROVED, SUGGESTION_REJECTED, SUGGESTION_IMPLEMENTED, SUGGESTION_STATUS_CHANGED,
        SPLASH_APPROVED, SPLASH_REJECTED,
        REDEMPTION_DELIVERED,
        BALANCE_APPROVED, BALANCE_REJECTED, BALANCE_IMPLEMENTED,
        WIKI_CREDIT,           // crédito por contribuição numa wiki entry
        ADMIN_MESSAGE,         // mensagem direta do admin
        SYSTEM                 // alertas do sistema (sem categoria)
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 64, nullable = false)
    private String recipientMcName;

    @Enumerated(EnumType.STRING)
    @Column(length = 32, nullable = false)
    private Type type;

    @Column(length = 256, nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String body;

    /** Deep-link pra abrir a tela relevante no front (ex: "/tester/dashboard?tab=bugs&id=42"). */
    @Column(length = 512)
    private String link;

    @Column(nullable = false)
    private boolean isRead = false;

    @Column
    private Instant readAt;

    @Column(nullable = false)
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getRecipientMcName() { return recipientMcName; } public void setRecipientMcName(String v) { recipientMcName = v; }
    public Type getType() { return type; } public void setType(Type v) { type = v; }
    public String getTitle() { return title; } public void setTitle(String v) { title = v; }
    public String getBody() { return body; } public void setBody(String v) { body = v; }
    public String getLink() { return link; } public void setLink(String v) { link = v; }
    public boolean isRead() { return isRead; } public void setRead(boolean v) { isRead = v; }
    public Instant getReadAt() { return readAt; } public void setReadAt(Instant v) { readAt = v; }
    public Instant getCreatedAt() { return createdAt; }
}
