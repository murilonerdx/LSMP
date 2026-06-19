package br.com.murilo.liberthia.admin.engine.echoes;

import jakarta.persistence.*;

@Entity
@Table(name = "memory_echoes")
public class MemoryEcho {

    @Id
    @Column(length = 64)
    private String id;

    @Column(length = 16) private String emoji;
    @Column(length = 128) private String name;
    @Column(length = 16) private String color;

    private double posX, posY, posZ;
    @Column(length = 32) private String posDim;
    private int radius;

    @Column(columnDefinition = "TEXT")
    private String ghostsJson;     // [{playerName, offset:{x,y,z}, rotation, showArms}]

    @Column(columnDefinition = "TEXT")
    private String whispersJson;   // array of strings

    private int whisperEverySec;
    @Column(length = 128) private String ambientSound;
    @Column(length = 128) private String ambientParticle;
    private int playDurationSec;
    private int cooldownSec;

    private boolean enabled;

    public String getId() { return id; } public void setId(String v) { this.id = v; }
    public String getEmoji() { return emoji; } public void setEmoji(String v) { this.emoji = v; }
    public String getName() { return name; } public void setName(String v) { this.name = v; }
    public String getColor() { return color; } public void setColor(String v) { this.color = v; }
    public double getPosX() { return posX; } public void setPosX(double v) { this.posX = v; }
    public double getPosY() { return posY; } public void setPosY(double v) { this.posY = v; }
    public double getPosZ() { return posZ; } public void setPosZ(double v) { this.posZ = v; }
    public String getPosDim() { return posDim; } public void setPosDim(String v) { this.posDim = v; }
    public int getRadius() { return radius; } public void setRadius(int v) { this.radius = v; }
    public String getGhostsJson() { return ghostsJson; } public void setGhostsJson(String v) { this.ghostsJson = v; }
    public String getWhispersJson() { return whispersJson; } public void setWhispersJson(String v) { this.whispersJson = v; }
    public int getWhisperEverySec() { return whisperEverySec; } public void setWhisperEverySec(int v) { this.whisperEverySec = v; }
    public String getAmbientSound() { return ambientSound; } public void setAmbientSound(String v) { this.ambientSound = v; }
    public String getAmbientParticle() { return ambientParticle; } public void setAmbientParticle(String v) { this.ambientParticle = v; }
    public int getPlayDurationSec() { return playDurationSec; } public void setPlayDurationSec(int v) { this.playDurationSec = v; }
    public int getCooldownSec() { return cooldownSec; } public void setCooldownSec(int v) { this.cooldownSec = v; }
    public boolean isEnabled() { return enabled; } public void setEnabled(boolean v) { this.enabled = v; }
}
