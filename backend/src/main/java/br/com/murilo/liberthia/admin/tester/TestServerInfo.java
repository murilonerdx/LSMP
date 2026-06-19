package br.com.murilo.liberthia.admin.tester;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Configuração singleton (id sempre = 1) com info do servidor de teste:
 * endereço, porta, versão MC, MOD requerido, NDA text, etc.
 *
 * Editável só por admin; testers veem em aba dedicada do dashboard.
 */
@Entity
@Table(name = "tester_server_info")
public class TestServerInfo {

    @Id
    private Long id = 1L;

    /** Endereço do servidor (ex: test.liberthia.com:25565). */
    @Column(length = 256)
    private String serverAddress;

    /** Versão MC (ex: 1.20.1 Forge). */
    @Column(length = 64)
    private String mcVersion;

    /** Modpack ou versão dos mods de beta (ex: 0.1.8-beta). */
    @Column(length = 64)
    private String modVersion;

    /** Notas e instruções de conexão (markdown). */
    @Column(columnDefinition = "TEXT")
    private String connectionNotes;

    /** Texto de NDA / avisos (será exibido em destaque pro tester). */
    @Column(columnDefinition = "TEXT")
    private String ndaText;

    /** URL/link extra (Discord do beta, etc). */
    @Column(length = 512)
    private String discordLink;

    /** Server Voice Chat ID se aplicável. */
    @Column(length = 128)
    private String voiceChatInfo;

    /** Online/offline (manual toggle). */
    @Column(nullable = false)
    private boolean active = false;

    @Column(length = 64)
    private String updatedBy;

    @Column
    private Instant updatedAt;

    public Long getId() { return id; }
    public void setId(Long v) { id = v; }
    public String getServerAddress() { return serverAddress; } public void setServerAddress(String v) { serverAddress = v; }
    public String getMcVersion() { return mcVersion; } public void setMcVersion(String v) { mcVersion = v; }
    public String getModVersion() { return modVersion; } public void setModVersion(String v) { modVersion = v; }
    public String getConnectionNotes() { return connectionNotes; } public void setConnectionNotes(String v) { connectionNotes = v; }
    public String getNdaText() { return ndaText; } public void setNdaText(String v) { ndaText = v; }
    public String getDiscordLink() { return discordLink; } public void setDiscordLink(String v) { discordLink = v; }
    public String getVoiceChatInfo() { return voiceChatInfo; } public void setVoiceChatInfo(String v) { voiceChatInfo = v; }
    public boolean isActive() { return active; } public void setActive(boolean v) { active = v; }
    public String getUpdatedBy() { return updatedBy; } public void setUpdatedBy(String v) { updatedBy = v; }
    public Instant getUpdatedAt() { return updatedAt; } public void setUpdatedAt(Instant v) { updatedAt = v; }
}
