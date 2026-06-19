package br.com.murilo.liberthia.admin.engine.kv;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * Tabela genérica chave→JSON pra páginas que só precisam persistir um config
 * (sem engine de fundo). Substitui localStorage do frontend.
 *
 * Examples de keys:
 *   - "dice_rolls"          (DiceRoller — histórico de rolls)
 *   - "wheel"               (RandomWheel/Roleta)
 *   - "voice_modulator"     (VoiceModulator)
 *   - "atmospheres.horror", "atmospheres.cosmic", ... (atmosphere configs)
 *   - "macros", "cron"      (Macro/Cron pages)
 *   - "scripts"
 *   - "story_beats"
 *   - etc.
 */
@Entity
@Table(name = "kv_configs")
public class KvConfig {

    @Id
    @Column(length = 128)
    private String key;

    @Column(columnDefinition = "TEXT")
    private String dataJson;

    private Instant updatedAt;

    public KvConfig() {}
    public KvConfig(String key, String dataJson) {
        this.key = key;
        this.dataJson = dataJson;
        this.updatedAt = Instant.now();
    }

    @PreUpdate @PrePersist
    void touch() { this.updatedAt = Instant.now(); }

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }
    public String getDataJson() { return dataJson; }
    public void setDataJson(String dataJson) { this.dataJson = dataJson; }
    public Instant getUpdatedAt() { return updatedAt; }
}
