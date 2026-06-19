package br.com.murilo.liberthia.admin.engine.voice;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Service que mantém um singleton (id=1) de {@link VoiceRetentionSettings}.
 * Lazy-init: se a tabela está vazia (primeiro boot), cria com defaults
 * usando o env VOICE_RETENTION_DAYS como semente — assim a migração de
 * env-based pra DB-based é transparente.
 */
@Service
public class VoiceRetentionService {

    public interface Repo extends JpaRepository<VoiceRetentionSettings, Long> {}

    private final Repo repo;

    /** Fallback inicial — usado APENAS na primeira vez que cria o registro. */
    @Value("${voice.retention-days:14}")
    private int defaultDays;

    public VoiceRetentionService(Repo repo) {
        this.repo = repo;
    }

    /** Sempre retorna o singleton; cria com defaults se ainda não existir. */
    @Transactional
    public VoiceRetentionSettings get() {
        return repo.findById(1L).orElseGet(() -> {
            VoiceRetentionSettings s = new VoiceRetentionSettings();
            s.setId(1L);
            s.setEnabled(true);
            s.setRetentionDays(Math.max(1, defaultDays));
            s.setUpdatedAt(Instant.now());
            s.setUpdatedBy("system-init");
            return repo.save(s);
        });
    }

    @Transactional
    public VoiceRetentionSettings update(Boolean enabled, Integer retentionDays,
                                         Integer maxClipsPerPlayer, String by) {
        VoiceRetentionSettings s = get();
        if (enabled != null) s.setEnabled(enabled);
        if (retentionDays != null) s.setRetentionDays(retentionDays);
        if (maxClipsPerPlayer != null) s.setMaxClipsPerPlayer(maxClipsPerPlayer);
        s.setUpdatedAt(Instant.now());
        if (by != null && !by.isBlank()) s.setUpdatedBy(by);
        return repo.save(s);
    }
}
