package br.com.murilo.liberthia.admin.engine.boxes;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "time_boxes")
public class TimeBox {

    @Id
    @Column(length = 64)
    private String id;

    @Column(length = 16) private String emoji;
    @Column(length = 128) private String name;
    @Column(columnDefinition = "TEXT") private String description;

    private Instant unlockAt;

    /** uuid de player, ou "@a" / "@first" */
    @Column(length = 64)
    private String recipient;

    @Column(columnDefinition = "TEXT") private String itemsJson;     // [{itemId, count}]
    @Column(columnDefinition = "TEXT") private String effectsJson;   // [{effect, durationSec, amplifier}]
    @Column(columnDefinition = "TEXT") private String unlockMessage;
    @Column(length = 128) private String unlockSound;
    @Column(length = 128) private String unlockParticle;

    private boolean countdownEnabled;
    private int countdownEveryMin;
    @Column(length = 256) private String countdownMsg;

    private boolean delivered;
    private Instant lastCountdownTs;

    public String getId() { return id; } public void setId(String v) { this.id = v; }
    public String getEmoji() { return emoji; } public void setEmoji(String v) { this.emoji = v; }
    public String getName() { return name; } public void setName(String v) { this.name = v; }
    public String getDescription() { return description; } public void setDescription(String v) { this.description = v; }
    public Instant getUnlockAt() { return unlockAt; } public void setUnlockAt(Instant v) { this.unlockAt = v; }
    public String getRecipient() { return recipient; } public void setRecipient(String v) { this.recipient = v; }
    public String getItemsJson() { return itemsJson; } public void setItemsJson(String v) { this.itemsJson = v; }
    public String getEffectsJson() { return effectsJson; } public void setEffectsJson(String v) { this.effectsJson = v; }
    public String getUnlockMessage() { return unlockMessage; } public void setUnlockMessage(String v) { this.unlockMessage = v; }
    public String getUnlockSound() { return unlockSound; } public void setUnlockSound(String v) { this.unlockSound = v; }
    public String getUnlockParticle() { return unlockParticle; } public void setUnlockParticle(String v) { this.unlockParticle = v; }
    public boolean isCountdownEnabled() { return countdownEnabled; } public void setCountdownEnabled(boolean v) { this.countdownEnabled = v; }
    public int getCountdownEveryMin() { return countdownEveryMin; } public void setCountdownEveryMin(int v) { this.countdownEveryMin = v; }
    public String getCountdownMsg() { return countdownMsg; } public void setCountdownMsg(String v) { this.countdownMsg = v; }
    public boolean isDelivered() { return delivered; } public void setDelivered(boolean v) { this.delivered = v; }
    public Instant getLastCountdownTs() { return lastCountdownTs; } public void setLastCountdownTs(Instant v) { this.lastCountdownTs = v; }
}
