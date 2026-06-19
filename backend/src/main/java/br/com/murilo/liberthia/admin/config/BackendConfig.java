package br.com.murilo.liberthia.admin.config;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * Key-value config persistente do backend. Permite que valores de configuração
 * que normalmente vêm do {@code application.yml}/env var possam ser sobrescritos
 * em runtime e SOBREVIVAM ao restart do backend.
 *
 * Caso de uso original: {@code MOD_TOKEN} — o mod gera/lê seu próprio token em
 * {@code world/serverconfig/liberthia-server.toml} e ele divergia do
 * {@code MOD_TOKEN} do docker-compose toda vez que o operador esquecia de
 * sincronizar. Agora o ModRegistry salva o token recebido via
 * {@code POST /api/mod/register} aqui, e ele vira a fonte de verdade no boot
 * (sobrepondo o env var).
 *
 * Schema deliberadamente simples — chave string, valor TEXT, timestamp. Não vale
 * a pena criar uma entity por chave: cresce sem migração.
 */
@Entity
@Table(name = "backend_config")
public class BackendConfig {

    /** Chave usada pra armazenar o token override do mod. */
    public static final String KEY_MOD_TOKEN = "MOD_TOKEN";

    /**
     * Quando "true", o {@link br.com.murilo.liberthia.admin.mod.ModRegistry}
     * IGNORA o token enviado pelo mod via {@code POST /api/mod/register}. Setado
     * pelo painel admin quando o operador salva manualmente um token — evita o
     * loop "o mod re-registra a cada 60s e sobrescreve o que eu acabei de salvar".
     */
    public static final String KEY_MOD_TOKEN_LOCKED = "MOD_TOKEN_LOCKED";

    @Id
    @Column(length = 64)
    private String key;

    @Column(columnDefinition = "TEXT")
    private String value;

    @Column
    private Instant updatedAt;

    public BackendConfig() {}

    public BackendConfig(String key, String value) {
        this.key = key;
        this.value = value;
        this.updatedAt = Instant.now();
    }

    @PrePersist
    @PreUpdate
    public void touch() {
        this.updatedAt = Instant.now();
    }

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
