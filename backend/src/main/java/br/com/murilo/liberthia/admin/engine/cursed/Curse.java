package br.com.murilo.liberthia.admin.engine.cursed;

import jakarta.persistence.*;

@Entity
@Table(name = "cursed_items")
public class Curse {

    @Id
    @Column(length = 64)
    private String id;

    @Column(length = 16)
    private String emoji;

    @Column(length = 128)
    private String itemId;

    @Column(length = 256)
    private String displayName;     // §-formatted

    @Column(columnDefinition = "TEXT")
    private String loreJson;        // array of strings

    @Column(columnDefinition = "TEXT")
    private String enchantmentsJson; // array of {id, level}

    @Column(columnDefinition = "TEXT")
    private String curseEffectsJson; // array of {effect, durationSec, amplifier}

    @Column(columnDefinition = "TEXT")
    private String whispersJson;    // array of strings

    @Column(length = 64)
    private String particle;

    @Column(length = 128)
    private String sound;

    private int tickIntervalSec;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getEmoji() { return emoji; }
    public void setEmoji(String emoji) { this.emoji = emoji; }
    public String getItemId() { return itemId; }
    public void setItemId(String itemId) { this.itemId = itemId; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getLoreJson() { return loreJson; }
    public void setLoreJson(String loreJson) { this.loreJson = loreJson; }
    public String getEnchantmentsJson() { return enchantmentsJson; }
    public void setEnchantmentsJson(String enchantmentsJson) { this.enchantmentsJson = enchantmentsJson; }
    public String getCurseEffectsJson() { return curseEffectsJson; }
    public void setCurseEffectsJson(String curseEffectsJson) { this.curseEffectsJson = curseEffectsJson; }
    public String getWhispersJson() { return whispersJson; }
    public void setWhispersJson(String whispersJson) { this.whispersJson = whispersJson; }
    public String getParticle() { return particle; }
    public void setParticle(String particle) { this.particle = particle; }
    public String getSound() { return sound; }
    public void setSound(String sound) { this.sound = sound; }
    public int getTickIntervalSec() { return tickIntervalSec; }
    public void setTickIntervalSec(int tickIntervalSec) { this.tickIntervalSec = tickIntervalSec; }
}
