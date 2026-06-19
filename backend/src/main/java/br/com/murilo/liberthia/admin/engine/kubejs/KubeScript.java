package br.com.murilo.liberthia.admin.engine.kubejs;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * Script JS submetido pelos players pra rodar via KubeJS.
 * Admin aprova → backend escreve em kubejs/server_scripts/ (path configurável).
 */
@Entity
@Table(name = "kubejs_scripts", indexes = {
        @Index(name = "idx_kjs_status", columnList = "status"),
        @Index(name = "idx_kjs_updated", columnList = "updatedAt DESC")
})
public class KubeScript {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(length = 128) private String name;
    @Column(length = 1024) private String description;
    @Column(columnDefinition = "TEXT") private String sourceCode;
    @Column(length = 32) private String scope = "server"; // server | client | startup
    @Column(length = 16) private String status = "pending"; // pending | approved | rejected
    @Column(length = 64) private String authorUuid;
    @Column(length = 64) private String authorName;
    @Column(length = 64) private String reviewedBy;
    @Column(length = 1024) private String reviewNote;
    @Column private int downloads = 0;
    @Column private Instant createdAt;
    @Column private Instant updatedAt;

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String v) { name = v; }
    public String getDescription() { return description; }
    public void setDescription(String v) { description = v; }
    public String getSourceCode() { return sourceCode; }
    public void setSourceCode(String v) { sourceCode = v; }
    public String getScope() { return scope; }
    public void setScope(String v) { scope = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { status = v; }
    public String getAuthorUuid() { return authorUuid; }
    public void setAuthorUuid(String v) { authorUuid = v; }
    public String getAuthorName() { return authorName; }
    public void setAuthorName(String v) { authorName = v; }
    public String getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(String v) { reviewedBy = v; }
    public String getReviewNote() { return reviewNote; }
    public void setReviewNote(String v) { reviewNote = v; }
    public int getDownloads() { return downloads; }
    public void setDownloads(int v) { downloads = v; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant v) { createdAt = v; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant v) { updatedAt = v; }
}
