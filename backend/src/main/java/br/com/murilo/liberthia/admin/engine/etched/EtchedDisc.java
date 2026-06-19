package br.com.murilo.liberthia.admin.engine.etched;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * Disco musical do mod Etched. Admin cadastra URL/título/autor; backend
 * dispara /give com NBT custom pra player receber o disco no jogo.
 *
 * NBT formato (Etched 3.x):
 *   etched:music_label_disc{Music:{title:"X",author:"Y",url:"https://...",length:120}}
 */
@Entity
@Table(name = "etched_discs", indexes = {
        @Index(name = "idx_etched_title", columnList = "title"),
        @Index(name = "idx_etched_category", columnList = "category")
})
public class EtchedDisc {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 128, nullable = false)
    private String title;

    @Column(length = 128)
    private String author = "Servidor";

    /** URL: YouTube, Soundcloud, mp3 direto. Etched aceita várias. */
    @Column(length = 512, nullable = false)
    private String url;

    /** Tipo de disco visual: "blank" (default), "lapis", "gold", "diamond", "emerald", "nether"... */
    @Column(length = 32)
    private String discColor = "blank";

    @Column
    private int durationSec = 0;

    @Column(length = 64)
    private String category = "music"; // music | ambient | sfx | meme | event

    @Column(length = 512)
    private String description;

    @Column(length = 64)
    private String addedBy;

    @Column
    private long playCount = 0;

    /**
     * Após extração via yt-dlp, fica em data/etched-audio/{id}.mp3 e o disco
     * passa a apontar pra URL local /api/etched/audio/{id} no comando /give.
     * Status: NONE | EXTRACTING | READY | FAILED.
     */
    @Column(length = 16)
    private String audioStatus = "NONE";

    @Column(length = 512)
    private String audioError;

    @Column
    private Long audioSizeBytes;

    @Column
    private Instant createdAt;

    @PrePersist
    public void prePersist() { if (createdAt == null) createdAt = Instant.now(); }

    public Long getId() { return id; }
    public String getTitle() { return title; } public void setTitle(String v) { this.title = v; }
    public String getAuthor() { return author; } public void setAuthor(String v) { this.author = v; }
    public String getUrl() { return url; } public void setUrl(String v) { this.url = v; }
    public String getDiscColor() { return discColor; } public void setDiscColor(String v) { this.discColor = v; }
    public int getDurationSec() { return durationSec; } public void setDurationSec(int v) { this.durationSec = v; }
    public String getCategory() { return category; } public void setCategory(String v) { this.category = v; }
    public String getDescription() { return description; } public void setDescription(String v) { this.description = v; }
    public String getAddedBy() { return addedBy; } public void setAddedBy(String v) { this.addedBy = v; }
    public long getPlayCount() { return playCount; } public void setPlayCount(long v) { this.playCount = v; }
    public String getAudioStatus() { return audioStatus; } public void setAudioStatus(String v) { this.audioStatus = v; }
    public String getAudioError() { return audioError; } public void setAudioError(String v) { this.audioError = v; }
    public Long getAudioSizeBytes() { return audioSizeBytes; } public void setAudioSizeBytes(Long v) { this.audioSizeBytes = v; }
    public Instant getCreatedAt() { return createdAt; } public void setCreatedAt(Instant v) { this.createdAt = v; }
}
