package br.com.murilo.liberthia.admin.engine.pehkui;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * Preset de escalas Pehkui salvo pelo admin. Cada preset é um conjunto nomeado
 * de pares (scaleType, scaleValue) serializado como JSON. Aplica num player
 * disparando N comandos /scale set ... em sequência.
 *
 * Exemplo de scalesJson:
 * {
 *   "pehkui:width": 0.3,
 *   "pehkui:height": 5.0,
 *   "pehkui:motion": 0.5
 * }
 */
@Entity
@Table(name = "pehkui_presets", indexes = {
        @Index(name = "idx_pehkui_name", columnList = "name", unique = true)
})
public class PehkuiPreset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 64, nullable = false)
    private String name;

    @Column(length = 16)
    private String emoji = "🧍";

    @Column(length = 256)
    private String description;

    /** JSON: { "pehkui:width": 2.0, "pehkui:height": 0.5, ... } */
    @Column(columnDefinition = "TEXT", nullable = false)
    private String scalesJson = "{}";

    /** Custo de DM (matéria escura) pra aplicar — 0 = grátis, admin sempre passa. */
    @Column
    private double dmCost = 0.0;

    /** Duração em segundos — 0 = permanente. Se >0, schedule reset via /scale reset. */
    @Column
    private int durationSec = 0;

    @Column(length = 64)
    private String createdBy;

    @Column
    private Instant createdAt;

    @PrePersist public void prePersist() { if (createdAt == null) createdAt = Instant.now(); }

    public Long getId() { return id; }
    public String getName() { return name; } public void setName(String v) { this.name = v; }
    public String getEmoji() { return emoji; } public void setEmoji(String v) { this.emoji = v; }
    public String getDescription() { return description; } public void setDescription(String v) { this.description = v; }
    public String getScalesJson() { return scalesJson; } public void setScalesJson(String v) { this.scalesJson = v; }
    public double getDmCost() { return dmCost; } public void setDmCost(double v) { this.dmCost = v; }
    public int getDurationSec() { return durationSec; } public void setDurationSec(int v) { this.durationSec = v; }
    public String getCreatedBy() { return createdBy; } public void setCreatedBy(String v) { this.createdBy = v; }
    public Instant getCreatedAt() { return createdAt; } public void setCreatedAt(Instant v) { this.createdAt = v; }
}
