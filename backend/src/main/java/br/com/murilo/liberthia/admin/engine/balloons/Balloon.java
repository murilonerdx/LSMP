package br.com.murilo.liberthia.admin.engine.balloons;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * Balão de fala (TalkBalloons / Comics Bubbles / Emojiful). Pode ser capturado
 * automaticamente do chat dos players (via hook ServerChatEvent quando admin
 * habilitar) OU criado manualmente pelo painel pra fazer player "falar".
 *
 * Type:
 *   - "talk"      → balão simples (TalkBalloons)
 *   - "comic"     → estilo quadrinho (Comics Bubbles)
 *   - "shout"     → grito (com !!)
 *   - "thought"   → pensamento (bolha)
 *   - "image"     → URL de imagem em vez de texto
 */
@Entity
@Table(name = "chat_balloons", indexes = {
        @Index(name = "idx_balloon_player_ts", columnList = "playerUuid, ts DESC"),
        @Index(name = "idx_balloon_ts", columnList = "ts DESC")
})
public class Balloon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 64)
    private String playerUuid;

    @Column(length = 64)
    private String playerName;

    @Column(columnDefinition = "TEXT")
    private String text;

    @Column(length = 16)
    private String type = "talk";

    /** URL de imagem (se type=image). */
    @Column(length = 512)
    private String imageUrl;

    /** captured = veio do chat real do player. admin = criado pelo painel. */
    @Column(length = 16)
    private String source = "captured";

    private long ts;

    @Column(length = 64)
    private String dimension;
    private Double posX, posY, posZ;

    @Column(length = 64)
    private String createdBy;

    @Column
    private Instant createdAt;

    @PrePersist
    public void prePersist() { if (createdAt == null) createdAt = Instant.now(); }

    public Long getId() { return id; }
    public String getPlayerUuid() { return playerUuid; } public void setPlayerUuid(String v) { this.playerUuid = v; }
    public String getPlayerName() { return playerName; } public void setPlayerName(String v) { this.playerName = v; }
    public String getText() { return text; } public void setText(String v) { this.text = v; }
    public String getType() { return type; } public void setType(String v) { this.type = v; }
    public String getImageUrl() { return imageUrl; } public void setImageUrl(String v) { this.imageUrl = v; }
    public String getSource() { return source; } public void setSource(String v) { this.source = v; }
    public long getTs() { return ts; } public void setTs(long v) { this.ts = v; }
    public String getDimension() { return dimension; } public void setDimension(String v) { this.dimension = v; }
    public Double getPosX() { return posX; } public void setPosX(Double v) { this.posX = v; }
    public Double getPosY() { return posY; } public void setPosY(Double v) { this.posY = v; }
    public Double getPosZ() { return posZ; } public void setPosZ(Double v) { this.posZ = v; }
    public String getCreatedBy() { return createdBy; } public void setCreatedBy(String v) { this.createdBy = v; }
    public Instant getCreatedAt() { return createdAt; } public void setCreatedAt(Instant v) { this.createdAt = v; }
}
