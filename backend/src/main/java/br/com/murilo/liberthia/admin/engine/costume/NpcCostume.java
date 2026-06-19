package br.com.murilo.liberthia.admin.engine.costume;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * "Personagem" RP pronto pra vestir num player.
 * Combina: fakename, skin CPM, armadura, escala Pehkui, equipamento, chat-title.
 */
@Entity
@Table(name = "npc_costumes", indexes = {
        @Index(name = "idx_costume_name", columnList = "name")
})
public class NpcCostume {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(length = 128) private String name;          // "Capitão Caesar"
    @Column(length = 32) private String archetype;      // "hero", "villain", "npc", "spirit"
    @Column(length = 512) private String description;

    // Visual
    @Column(length = 64) private String fakeName;       // §ePassageiro Solitário
    @Column(length = 64) private String chatTitle;      // [PRESO]
    @Column(length = 256) private String cpmProjectPath;// caminho do .cpmproject (ou URL)
    @Column(length = 256) private String skinUrl;       // skin alternativa (PNG URL)
    @Column(length = 256) private String armourerSkinId;// skin do ArmourersWorkshop (db ID)
    @Column private double scale = 1.0;                 // Pehkui

    // Equipamento (JSON: {mainhand, offhand, helmet, chest, legs, boots})
    // SEM @Lob — quebra LOWER() em Hibernate 6 se algum dia entrar em @Query
    @Column(columnDefinition = "TEXT") private String equipmentJson;
    @Column(columnDefinition = "TEXT") private String effectsJson;     // potion effects ao vestir

    @Column private int timesUsed = 0;
    @Column private Instant createdAt;
    @Column private Instant updatedAt;

    public Long getId() { return id; }
    public String getName() { return name; } public void setName(String v) { name = v; }
    public String getArchetype() { return archetype; } public void setArchetype(String v) { archetype = v; }
    public String getDescription() { return description; } public void setDescription(String v) { description = v; }
    public String getFakeName() { return fakeName; } public void setFakeName(String v) { fakeName = v; }
    public String getChatTitle() { return chatTitle; } public void setChatTitle(String v) { chatTitle = v; }
    public String getCpmProjectPath() { return cpmProjectPath; } public void setCpmProjectPath(String v) { cpmProjectPath = v; }
    public String getSkinUrl() { return skinUrl; } public void setSkinUrl(String v) { skinUrl = v; }
    public String getArmourerSkinId() { return armourerSkinId; } public void setArmourerSkinId(String v) { armourerSkinId = v; }
    public double getScale() { return scale; } public void setScale(double v) { scale = v; }
    public String getEquipmentJson() { return equipmentJson; } public void setEquipmentJson(String v) { equipmentJson = v; }
    public String getEffectsJson() { return effectsJson; } public void setEffectsJson(String v) { effectsJson = v; }
    public int getTimesUsed() { return timesUsed; } public void setTimesUsed(int v) { timesUsed = v; }
    public Instant getCreatedAt() { return createdAt; } public void setCreatedAt(Instant v) { createdAt = v; }
    public Instant getUpdatedAt() { return updatedAt; } public void setUpdatedAt(Instant v) { updatedAt = v; }
}
