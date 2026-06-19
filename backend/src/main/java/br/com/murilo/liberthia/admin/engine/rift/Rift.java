package br.com.murilo.liberthia.admin.engine.rift;

import jakarta.persistence.*;

@Entity
@Table(name = "rifts")
public class Rift {

    @Id
    @Column(length = 64)
    private String id;

    @Column(length = 16)
    private String emoji;

    @Column(length = 128)
    private String name;

    private double posX, posY, posZ;

    @Column(length = 32)
    private String posDim;

    private int radius;

    private Double destX, destY, destZ;
    @Column(length = 32)
    private String destDim;

    private int randomDestRadius;

    @Column(length = 64)
    private String particle;

    private int particleCount;

    private int visualEvery;

    @Column(length = 128)
    private String ambientSound;

    @Column(length = 256)
    private String preMsg;

    @Column(length = 256)
    private String postMsg;

    @Column(columnDefinition = "TEXT")
    private String preEffectsJson;

    @Column(columnDefinition = "TEXT")
    private String postEffectsJson;

    private int cooldownSec;

    private boolean enabled;

    private boolean visualize;

    private long triggers;

    // Getters/setters
    public String getId() { return id; } public void setId(String v) { this.id = v; }
    public String getEmoji() { return emoji; } public void setEmoji(String v) { this.emoji = v; }
    public String getName() { return name; } public void setName(String v) { this.name = v; }
    public double getPosX() { return posX; } public void setPosX(double v) { this.posX = v; }
    public double getPosY() { return posY; } public void setPosY(double v) { this.posY = v; }
    public double getPosZ() { return posZ; } public void setPosZ(double v) { this.posZ = v; }
    public String getPosDim() { return posDim; } public void setPosDim(String v) { this.posDim = v; }
    public int getRadius() { return radius; } public void setRadius(int v) { this.radius = v; }
    public Double getDestX() { return destX; } public void setDestX(Double v) { this.destX = v; }
    public Double getDestY() { return destY; } public void setDestY(Double v) { this.destY = v; }
    public Double getDestZ() { return destZ; } public void setDestZ(Double v) { this.destZ = v; }
    public String getDestDim() { return destDim; } public void setDestDim(String v) { this.destDim = v; }
    public int getRandomDestRadius() { return randomDestRadius; } public void setRandomDestRadius(int v) { this.randomDestRadius = v; }
    public String getParticle() { return particle; } public void setParticle(String v) { this.particle = v; }
    public int getParticleCount() { return particleCount; } public void setParticleCount(int v) { this.particleCount = v; }
    public int getVisualEvery() { return visualEvery; } public void setVisualEvery(int v) { this.visualEvery = v; }
    public String getAmbientSound() { return ambientSound; } public void setAmbientSound(String v) { this.ambientSound = v; }
    public String getPreMsg() { return preMsg; } public void setPreMsg(String v) { this.preMsg = v; }
    public String getPostMsg() { return postMsg; } public void setPostMsg(String v) { this.postMsg = v; }
    public String getPreEffectsJson() { return preEffectsJson; } public void setPreEffectsJson(String v) { this.preEffectsJson = v; }
    public String getPostEffectsJson() { return postEffectsJson; } public void setPostEffectsJson(String v) { this.postEffectsJson = v; }
    public int getCooldownSec() { return cooldownSec; } public void setCooldownSec(int v) { this.cooldownSec = v; }
    public boolean isEnabled() { return enabled; } public void setEnabled(boolean v) { this.enabled = v; }
    public boolean isVisualize() { return visualize; } public void setVisualize(boolean v) { this.visualize = v; }
    public long getTriggers() { return triggers; } public void setTriggers(long v) { this.triggers = v; }
}
