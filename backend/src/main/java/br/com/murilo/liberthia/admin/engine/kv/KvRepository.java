package br.com.murilo.liberthia.admin.engine.kv;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositório JPA pra tabela {@code kv_configs}. Tirado de dentro do
 * {@link KvController} porque uma classe pública precisa estar em arquivo
 * próprio com mesmo nome — exigência do Java.
 *
 * <p>Usado tanto pelo KvController (REST genérico) quanto pelo
 * {@link br.com.murilo.liberthia.admin.engine.voice.WhisperRuntimeConfig}
 * (que persiste config do Whisper como key={@code whisper.config}).
 */
@Repository
public interface KvRepository extends JpaRepository<KvConfig, String> {}
