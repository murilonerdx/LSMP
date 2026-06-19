package br.com.murilo.liberthia.admin.engine.pilgrimage;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * Peregrinação: sequência ordenada de "estações" (memorial / glyph / anchor /
 * coords) que o player precisa visitar pra completar.
 *
 * Steps são guardados como JSON pra evitar tabela filha — formato:
 *   [
 *     {"type":"memorial","refId":"mem_abc"},
 *     {"type":"glyph","refId":"glyph_xyz"},
 *     {"type":"anchor","refId":"anchor_123"},
 *     {"type":"coords","x":123,"y":64,"z":-200,"dim":"overworld","radius":5,"label":"Cratera"}
 *   ]
 *
 * Engine consome esse JSON e detecta avanço de step via:
 *  - integração com EchoEngine (memorial é checado via posição perto)
 *  - integração com GlyphEngine (escuta evento de descoberta)
 *  - integração com SaveAnchorEngine
 *  - polling de posição (pra coords avulsas)
 */
@Entity
@Table(name = "pilgrimages")
public class Pilgrimage {

    @Id
    @Column(length = 64)
    private String id;

    @Column(length = 16)
    private String emoji = "⚔";

    @Column(length = 128)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    /** JSON array de steps. */
    @Column(columnDefinition = "TEXT")
    private String stepsJson;

    /**
     * Janela de tempo permitida pra completar (em segundos). 0 = sem limite.
     * Player que ultrapassar reseta o progresso.
     */
    private int timeLimitSec = 0;

    /** Comando MC executado no completion (suporta {player}). */
    @Column(columnDefinition = "TEXT")
    private String rewardCmd;

    /** Tellraw ao começar (suporta §-codes e {player}). */
    @Column(columnDefinition = "TEXT")
    private String startMsg;

    /** Tellraw ao avançar uma estação. */
    @Column(columnDefinition = "TEXT")
    private String stepMsg;

    /** Tellraw ao completar. */
    @Column(columnDefinition = "TEXT")
    private String completeMsg;

    /** Som ao avançar/completar. */
    @Column(length = 128)
    private String stepSound;

    @Column(length = 128)
    private String completeSound;

    private boolean enabled;

    private Instant createdAt;

    public Pilgrimage() {}

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
    }

    public String getId() { return id; } public void setId(String v) { this.id = v; }
    public String getEmoji() { return emoji; } public void setEmoji(String v) { this.emoji = v; }
    public String getName() { return name; } public void setName(String v) { this.name = v; }
    public String getDescription() { return description; } public void setDescription(String v) { this.description = v; }
    public String getStepsJson() { return stepsJson; } public void setStepsJson(String v) { this.stepsJson = v; }
    public int getTimeLimitSec() { return timeLimitSec; } public void setTimeLimitSec(int v) { this.timeLimitSec = v; }
    public String getRewardCmd() { return rewardCmd; } public void setRewardCmd(String v) { this.rewardCmd = v; }
    public String getStartMsg() { return startMsg; } public void setStartMsg(String v) { this.startMsg = v; }
    public String getStepMsg() { return stepMsg; } public void setStepMsg(String v) { this.stepMsg = v; }
    public String getCompleteMsg() { return completeMsg; } public void setCompleteMsg(String v) { this.completeMsg = v; }
    public String getStepSound() { return stepSound; } public void setStepSound(String v) { this.stepSound = v; }
    public String getCompleteSound() { return completeSound; } public void setCompleteSound(String v) { this.completeSound = v; }
    public boolean isEnabled() { return enabled; } public void setEnabled(boolean v) { this.enabled = v; }
    public Instant getCreatedAt() { return createdAt; } public void setCreatedAt(Instant v) { this.createdAt = v; }
}
