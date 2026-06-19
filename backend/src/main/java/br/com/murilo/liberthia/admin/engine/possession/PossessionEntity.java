package br.com.murilo.liberthia.admin.engine.possession;

import jakarta.persistence.*;

/**
 * Template de entidade cósmica (Yh'kuath, Vorashen, Mor'Ghaur, etc.).
 * voiceLinesJson + effectsJson armazenam arrays JSON.
 */
@Entity
@Table(name = "possession_entities")
public class PossessionEntity {

    @Id
    @Column(length = 64)
    private String id;

    @Column(length = 16)
    private String emoji;

    @Column(length = 256)
    private String displayName;     // §-formatted

    @Column(length = 64)
    private String particle;

    @Column(length = 128)
    private String sound;

    /** JSON array of strings */
    @Column(columnDefinition = "TEXT")
    private String voiceLinesJson;

    @Column(length = 256)
    private String enterMsg;

    @Column(length = 256)
    private String exitMsg;

    private int speakIntervalSec;

    /** JSON array of {effect, amplifier} */
    @Column(columnDefinition = "TEXT")
    private String effectsJson;

    @Column(length = 16)
    private String auraColor;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getEmoji() { return emoji; }
    public void setEmoji(String emoji) { this.emoji = emoji; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getParticle() { return particle; }
    public void setParticle(String particle) { this.particle = particle; }
    public String getSound() { return sound; }
    public void setSound(String sound) { this.sound = sound; }
    public String getVoiceLinesJson() { return voiceLinesJson; }
    public void setVoiceLinesJson(String voiceLinesJson) { this.voiceLinesJson = voiceLinesJson; }
    public String getEnterMsg() { return enterMsg; }
    public void setEnterMsg(String enterMsg) { this.enterMsg = enterMsg; }
    public String getExitMsg() { return exitMsg; }
    public void setExitMsg(String exitMsg) { this.exitMsg = exitMsg; }
    public int getSpeakIntervalSec() { return speakIntervalSec; }
    public void setSpeakIntervalSec(int speakIntervalSec) { this.speakIntervalSec = speakIntervalSec; }
    public String getEffectsJson() { return effectsJson; }
    public void setEffectsJson(String effectsJson) { this.effectsJson = effectsJson; }
    public String getAuraColor() { return auraColor; }
    public void setAuraColor(String auraColor) { this.auraColor = auraColor; }
}
