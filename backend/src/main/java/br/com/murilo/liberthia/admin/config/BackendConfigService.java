package br.com.murilo.liberthia.admin.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

/**
 * Acesso simples ao {@link BackendConfig} (key-value persistente).
 *
 * <p>Engole exceções de DB no {@code getValue}: o boot do backend lê config daqui
 * antes de tudo, e se o Postgres tiver lento ou indisponível na inicialização
 * nada deve quebrar — o caller pode cair pro fallback (env var).
 */
@Service
public class BackendConfigService {

    private static final Logger log = LoggerFactory.getLogger(BackendConfigService.class);

    private final BackendConfigRepository repo;

    public BackendConfigService(BackendConfigRepository repo) {
        this.repo = repo;
    }

    /**
     * Lê o valor da chave. Retorna {@code null} se ausente ou se o DB falhar
     * (caller deve cair pro fallback).
     */
    public String getValue(String key) {
        try {
            Optional<BackendConfig> opt = repo.findById(key);
            return opt.map(BackendConfig::getValue).orElse(null);
        } catch (DataAccessException e) {
            log.warn("[BackendConfig] DB indisponível ao ler {} — retornando null. err={}", key, e.getMessage());
            return null;
        }
    }

    /**
     * Lê o {@code updatedAt} (último save). Útil pra mostrar no painel
     * "última atualização há X". Null se chave ausente ou DB indisponível.
     */
    public Instant getUpdatedAt(String key) {
        try {
            return repo.findById(key).map(BackendConfig::getUpdatedAt).orElse(null);
        } catch (DataAccessException e) {
            return null;
        }
    }

    @Transactional
    public void setValue(String key, String value) {
        try {
            BackendConfig cfg = repo.findById(key).orElseGet(() -> new BackendConfig(key, value));
            cfg.setKey(key);
            cfg.setValue(value);
            cfg.setUpdatedAt(Instant.now());
            repo.save(cfg);
            log.info("[BackendConfig] salvo: key={}", key);
        } catch (DataAccessException e) {
            // Não propaga — o ModRegistry continua funcionando em memória mesmo
            // se o DB recusar o save. Logamos pra auditoria.
            log.error("[BackendConfig] FALHA ao salvar {}: {}", key, e.getMessage());
        }
    }

    /** True se a chave existe no DB. */
    public boolean hasValue(String key) {
        try {
            return repo.existsById(key);
        } catch (DataAccessException e) {
            return false;
        }
    }
}
