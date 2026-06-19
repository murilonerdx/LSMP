package br.com.murilo.liberthia.admin.tester;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Item beta cadastrado pelo admin pra testers testarem.
 * Tester vê descrição/props/comando e pode pedir o item via comando in-game.
 */
@Entity
@Table(name = "tester_beta_items")
public class BetaItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 128, nullable = false)
    private String name;

    /** Item ID Minecraft (ex: "liberthia:matter_analyzer"). */
    @Column(length = 128)
    private String itemId;

    @Column(columnDefinition = "TEXT")
    private String description;

    /** JSON livre: propriedades destacáveis (ex: {"durability": 1500, "damage": 8}). */
    @Column(columnDefinition = "TEXT")
    private String propertiesJson;

    /** Comando completo que o tester executa pra receber o item. */
    @Column(length = 512)
    private String giveCommand;

    /** URL externa pra imagem/thumb (opcional). */
    @Column(length = 512)
    private String imageUrl;

    /**
     * Tipo do que está sendo testado. Antes só "item beta" — agora cobre todas
     * as 4 categorias do mod. Permite filtrar e renderizar UI específica.
     */
    public enum Kind { ITEM, BLOCK, FEATURE, ARTIFACT }

    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false)
    private Kind kind = Kind.ITEM;

    /**
     * JSON do recipe visual (ex: shaped 3x3).
     * Formato: { "type":"shaped|shapeless|smelting",
     *           "slots":[9 strings ou null],
     *           "result":"liberthia:foo",
     *           "resultCount":1 }
     * Frontend renderiza grid 3x3 com autocomplete dos items registrados.
     */
    @Column(columnDefinition = "TEXT")
    private String recipeJson;

    /** Efeitos/atributos in-game (JSON livre): {"damage":8, "effects":[{...}]}. */
    @Column(columnDefinition = "TEXT")
    private String effectsJson;

    /** Lore / texto narrativo do item (markdown). */
    @Column(columnDefinition = "TEXT")
    private String lore;

    @Column(length = 32)
    private String category;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(length = 64)
    private String createdBy;

    @Column(nullable = false)
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getName() { return name; } public void setName(String v) { name = v; }
    public String getItemId() { return itemId; } public void setItemId(String v) { itemId = v; }
    public String getDescription() { return description; } public void setDescription(String v) { description = v; }
    public String getPropertiesJson() { return propertiesJson; } public void setPropertiesJson(String v) { propertiesJson = v; }
    public String getGiveCommand() { return giveCommand; } public void setGiveCommand(String v) { giveCommand = v; }
    public String getImageUrl() { return imageUrl; } public void setImageUrl(String v) { imageUrl = v; }
    public Kind getKind() { return kind; } public void setKind(Kind v) { kind = v; }
    public String getRecipeJson() { return recipeJson; } public void setRecipeJson(String v) { recipeJson = v; }
    public String getEffectsJson() { return effectsJson; } public void setEffectsJson(String v) { effectsJson = v; }
    public String getLore() { return lore; } public void setLore(String v) { lore = v; }
    public String getCategory() { return category; } public void setCategory(String v) { category = v; }
    public boolean isEnabled() { return enabled; } public void setEnabled(boolean v) { enabled = v; }
    public String getCreatedBy() { return createdBy; } public void setCreatedBy(String v) { createdBy = v; }
    public Instant getCreatedAt() { return createdAt; }
}
