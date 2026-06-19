package br.com.murilo.liberthia.admin.engine.glyph;

import jakarta.persistence.*;

@Entity
@Table(name = "glyphs")
public class Glyph {

    @Id
    @Column(length = 64)
    private String id;

    @Column(length = 16)
    private String symbol;

    @Column(length = 16)
    private String emoji;

    @Column(length = 256)
    private String name;

    private double posX, posY, posZ;
    @Column(length = 32) private String posDim;
    private int radius;

    @Column(columnDefinition = "TEXT")
    private String loreFragment;

    @Column(columnDefinition = "TEXT")
    private String rewardsJson;

    @Column(length = 128) private String discoverSound;
    @Column(length = 128) private String discoverParticle;
    @Column(length = 128) private String hintParticle;
    private int hintEvery;
    private int hintCount;

    @Column(columnDefinition = "TEXT")
    private String discoveredByJson;  // array uuids

    private boolean enabled;

    public String getId() { return id; } public void setId(String v) { this.id = v; }
    public String getSymbol() { return symbol; } public void setSymbol(String v) { this.symbol = v; }
    public String getEmoji() { return emoji; } public void setEmoji(String v) { this.emoji = v; }
    public String getName() { return name; } public void setName(String v) { this.name = v; }
    public double getPosX() { return posX; } public void setPosX(double v) { this.posX = v; }
    public double getPosY() { return posY; } public void setPosY(double v) { this.posY = v; }
    public double getPosZ() { return posZ; } public void setPosZ(double v) { this.posZ = v; }
    public String getPosDim() { return posDim; } public void setPosDim(String v) { this.posDim = v; }
    public int getRadius() { return radius; } public void setRadius(int v) { this.radius = v; }
    public String getLoreFragment() { return loreFragment; } public void setLoreFragment(String v) { this.loreFragment = v; }
    public String getRewardsJson() { return rewardsJson; } public void setRewardsJson(String v) { this.rewardsJson = v; }
    public String getDiscoverSound() { return discoverSound; } public void setDiscoverSound(String v) { this.discoverSound = v; }
    public String getDiscoverParticle() { return discoverParticle; } public void setDiscoverParticle(String v) { this.discoverParticle = v; }
    public String getHintParticle() { return hintParticle; } public void setHintParticle(String v) { this.hintParticle = v; }
    public int getHintEvery() { return hintEvery; } public void setHintEvery(int v) { this.hintEvery = v; }
    public int getHintCount() { return hintCount; } public void setHintCount(int v) { this.hintCount = v; }
    public String getDiscoveredByJson() { return discoveredByJson; } public void setDiscoveredByJson(String v) { this.discoveredByJson = v; }
    public boolean isEnabled() { return enabled; } public void setEnabled(boolean v) { this.enabled = v; }
}
