package br.com.murilo.liberthia.admin.engine.madness;

import jakarta.persistence.*;

/**
 * Configuração global do MadnessMeter (singleton, id=1).
 * Mensagens whisper, FX no mundo, intervalo, decay, etc.
 *
 * Campos JSON guardam estruturas dinâmicas pra evitar migrations a cada novo
 * tipo de configuração.
 */
@Entity
@Table(name = "madness_config")
public class MadnessConfig {

    @Id
    private Integer id = 1;

    private boolean enabled;

    private int tickIntervalSec = 12;

    private double decayPerMinute = 1.5;

    /** "player" (só pro afetado) ou "everyone" (broadcast no chat) */
    @Column(length = 16)
    private String whisperBroadcastMode = "player";

    /** Habilita efeitos visuais aleatórios no mundo perto de players afetados */
    private boolean worldFxEnabled = true;

    /** Probabilidade (0-1) de spawnar efeito visual no mundo a cada tick zone-perturbado+ */
    private double worldFxChance = 0.4;

    /**
     * JSON com mensagens whisper por zona (zone -> array de strings).
     * Ex: {"inquieto":["§8§o...alguém te chama"], "perturbado":["§5§o...algo respira"], ...}
     */
    @Column(columnDefinition = "TEXT")
    private String zoneMessagesJson;

    /**
     * JSON com sons por zona (zone -> array de strings de sound id).
     */
    @Column(columnDefinition = "TEXT")
    private String zoneSoundsJson;

    /**
     * JSON com tipos de FX mundial habilitados.
     * Ex: ["random_particle", "ghost_armor_stand", "falling_block", "random_lightning"]
     */
    @Column(columnDefinition = "TEXT")
    private String worldFxTypesJson;

    /**
     * JSON com comandos MC por zona (zone -> array de comandos).
     * Executado pelo engine quando o player entra na zona. Suporta {player}
     * como placeholder pro nome do player afetado.
     * Ex: {"perturbado":["effect give {player} minecraft:nausea 30 0"], "consumido":["kill {player}"]}
     */
    @Column(columnDefinition = "TEXT")
    private String zoneCommandsJson;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public int getTickIntervalSec() { return tickIntervalSec; }
    public void setTickIntervalSec(int tickIntervalSec) { this.tickIntervalSec = tickIntervalSec; }
    public double getDecayPerMinute() { return decayPerMinute; }
    public void setDecayPerMinute(double decayPerMinute) { this.decayPerMinute = decayPerMinute; }
    public String getWhisperBroadcastMode() { return whisperBroadcastMode; }
    public void setWhisperBroadcastMode(String whisperBroadcastMode) { this.whisperBroadcastMode = whisperBroadcastMode; }
    public boolean isWorldFxEnabled() { return worldFxEnabled; }
    public void setWorldFxEnabled(boolean worldFxEnabled) { this.worldFxEnabled = worldFxEnabled; }
    public double getWorldFxChance() { return worldFxChance; }
    public void setWorldFxChance(double worldFxChance) { this.worldFxChance = worldFxChance; }
    public String getZoneMessagesJson() { return zoneMessagesJson; }
    public void setZoneMessagesJson(String zoneMessagesJson) { this.zoneMessagesJson = zoneMessagesJson; }
    public String getZoneSoundsJson() { return zoneSoundsJson; }
    public void setZoneSoundsJson(String zoneSoundsJson) { this.zoneSoundsJson = zoneSoundsJson; }
    public String getWorldFxTypesJson() { return worldFxTypesJson; }
    public void setWorldFxTypesJson(String worldFxTypesJson) { this.worldFxTypesJson = worldFxTypesJson; }
    public String getZoneCommandsJson() { return zoneCommandsJson; }
    public void setZoneCommandsJson(String zoneCommandsJson) { this.zoneCommandsJson = zoneCommandsJson; }
}
