package br.com.murilo.liberthia.admin.engine.anchor;

import jakarta.persistence.*;

@Entity
@Table(name = "save_anchors")
public class SaveAnchor {

    @Id
    @Column(length = 64)
    private String id;

    @Column(length = 16) private String emoji;
    @Column(length = 128) private String name;
    @Column(columnDefinition = "TEXT") private String description;

    private double posX, posY, posZ;
    @Column(length = 32) private String posDim;
    private int radius;

    /** JSON array de uuids que já ativaram. */
    @Column(columnDefinition = "TEXT")
    private String usesJson;

    private boolean enabled;

    public String getId() { return id; } public void setId(String v) { this.id = v; }
    public String getEmoji() { return emoji; } public void setEmoji(String v) { this.emoji = v; }
    public String getName() { return name; } public void setName(String v) { this.name = v; }
    public String getDescription() { return description; } public void setDescription(String v) { this.description = v; }
    public double getPosX() { return posX; } public void setPosX(double v) { this.posX = v; }
    public double getPosY() { return posY; } public void setPosY(double v) { this.posY = v; }
    public double getPosZ() { return posZ; } public void setPosZ(double v) { this.posZ = v; }
    public String getPosDim() { return posDim; } public void setPosDim(String v) { this.posDim = v; }
    public int getRadius() { return radius; } public void setRadius(int v) { this.radius = v; }
    public String getUsesJson() { return usesJson; } public void setUsesJson(String v) { this.usesJson = v; }
    public boolean isEnabled() { return enabled; } public void setEnabled(boolean v) { this.enabled = v; }
}
