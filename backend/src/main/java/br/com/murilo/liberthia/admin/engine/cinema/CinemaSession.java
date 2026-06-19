package br.com.murilo.liberthia.admin.engine.cinema;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * Sessão de cinema agendada — usa mod watermedia pra reproduzir vídeo in-game.
 * Admin agenda, players confirmam, backend dispara comando no horário.
 */
@Entity
@Table(name = "cinema_sessions", indexes = {
        @Index(name = "idx_cinema_starts", columnList = "startsAt"),
        @Index(name = "idx_cinema_status", columnList = "status")
})
public class CinemaSession {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(length = 256) private String title;
    @Column(length = 1024) private String description;
    @Column(length = 1024) private String mediaUrl;
    @Column(length = 256) private String theaterLocation;  // ex "overworld:100,64,200"
    @Column private Instant startsAt;
    @Column(length = 16) private String status = "scheduled"; // scheduled | live | ended | cancelled
    @Column(length = 64) private String createdBy;
    @Column private Instant createdAt;
    @Column(columnDefinition = "TEXT") private String rsvpsJson; // ["uuid1","uuid2"...]

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public void setTitle(String v) { title = v; }
    public String getDescription() { return description; }
    public void setDescription(String v) { description = v; }
    public String getMediaUrl() { return mediaUrl; }
    public void setMediaUrl(String v) { mediaUrl = v; }
    public String getTheaterLocation() { return theaterLocation; }
    public void setTheaterLocation(String v) { theaterLocation = v; }
    public Instant getStartsAt() { return startsAt; }
    public void setStartsAt(Instant v) { startsAt = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { status = v; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String v) { createdBy = v; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant v) { createdAt = v; }
    public String getRsvpsJson() { return rsvpsJson; }
    public void setRsvpsJson(String v) { rsvpsJson = v; }
}
