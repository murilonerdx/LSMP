package br.com.murilo.liberthia.admin.engine.encounter;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * Encontro de mobs em waves. Cada wave tem: lista de mobs, posição (relativa
 * ao centro), delay desde wave anterior, mensagem de anúncio.
 */
@Entity
@Table(name = "mob_encounters", indexes = {
        @Index(name = "idx_enc_name", columnList = "name")
})
public class MobEncounter {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(length = 128) private String name;
    @Column(length = 512) private String description;
    @Column(columnDefinition = "TEXT") private String wavesJson;  // [{delay, mobs:[{id,count,x,y,z,nbt,tag}], announce}]
    @Column(length = 64) private String dimension;
    @Column private double centerX, centerY, centerZ;
    @Column private int totalDifficulty = 1;       // estimado, 1-10
    @Column private int timesRun = 0;
    @Column private Instant createdAt;
    @Column private Instant updatedAt;

    public Long getId() { return id; }
    public String getName() { return name; } public void setName(String v) { name = v; }
    public String getDescription() { return description; } public void setDescription(String v) { description = v; }
    public String getWavesJson() { return wavesJson; } public void setWavesJson(String v) { wavesJson = v; }
    public String getDimension() { return dimension; } public void setDimension(String v) { dimension = v; }
    public double getCenterX() { return centerX; } public void setCenterX(double v) { centerX = v; }
    public double getCenterY() { return centerY; } public void setCenterY(double v) { centerY = v; }
    public double getCenterZ() { return centerZ; } public void setCenterZ(double v) { centerZ = v; }
    public int getTotalDifficulty() { return totalDifficulty; } public void setTotalDifficulty(int v) { totalDifficulty = v; }
    public int getTimesRun() { return timesRun; } public void setTimesRun(int v) { timesRun = v; }
    public Instant getCreatedAt() { return createdAt; } public void setCreatedAt(Instant v) { createdAt = v; }
    public Instant getUpdatedAt() { return updatedAt; } public void setUpdatedAt(Instant v) { updatedAt = v; }
}
