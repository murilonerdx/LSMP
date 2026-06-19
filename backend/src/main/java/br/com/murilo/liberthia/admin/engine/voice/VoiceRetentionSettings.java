package br.com.murilo.liberthia.admin.engine.voice;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Single-row table (id sempre = 1) que guarda configuração de retenção dos
 * voice clips. Tá em DB pra admin poder ligar/desligar e ajustar via painel
 * SEM precisar rebuild + redeploy da imagem.
 *
 * Antes, retenção era só env var (VOICE_RETENTION_DAYS), o que obrigava
 * mudança no stack pra ajustar. Agora o cleanup() consulta esse registro
 * a cada execução, então mudanças via UI valem na próxima rodada (5h AM).
 */
@Entity
@Table(name = "voice_retention_settings")
public class VoiceRetentionSettings {

    /** Sempre id=1 (singleton). Repository garante. */
    @Id
    private Long id = 1L;

    /** Se false, cleanup NÃO roda — clipes ficam pra sempre (até disco encher). */
    @Column(nullable = false)
    private boolean enabled = true;

    /** Quantos dias de retenção. Padrão 14d. Mínimo 1, máximo 3650 (10 anos). */
    @Column(nullable = false)
    private int retentionDays = 14;

    /**
     * Limite MÁXIMO de clipes por player. 0 = ilimitado (default).
     * Quando um player ultrapassa esse número, os clipes mais antigos
     * (que não estão protegidos com protectedFromCleanup) são deletados
     * automaticamente. Aplicado a cada novo upload e via cleanup agendado.
     *
     * columnDefinition com DEFAULT 0 evita que ALTER TABLE quebre quando
     * a tabela já tem linhas (migração de banco existente em produção).
     */
    @Column(nullable = false, columnDefinition = "INTEGER NOT NULL DEFAULT 0")
    private int maxClipsPerPlayer = 0;

    /** Quem mudou pela última vez — só pra audit log. */
    @Column(length = 64)
    private String updatedBy;

    @Column
    private Instant updatedAt;

    public VoiceRetentionSettings() {}

    public Long getId() { return id; }
    public void setId(Long v) { this.id = v; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean v) { this.enabled = v; }
    public int getRetentionDays() { return retentionDays; }
    public void setRetentionDays(int v) { this.retentionDays = Math.max(1, Math.min(3650, v)); }
    public int getMaxClipsPerPlayer() { return maxClipsPerPlayer; }
    /** 0 = ilimitado; senão clamped em [1, 100_000]. */
    public void setMaxClipsPerPlayer(int v) {
        if (v <= 0) this.maxClipsPerPlayer = 0;
        else this.maxClipsPerPlayer = Math.min(100_000, v);
    }
    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String v) { this.updatedBy = v; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant v) { this.updatedAt = v; }
}
