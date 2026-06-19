package br.com.murilo.liberthia.admin.engine.kv;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository público pra outras packages acessarem KvConfig (feature flags,
 * configs runtime, etc). O {@code KvRepository} dentro do KvController é
 * package-private — esse aqui é o expostro pra uso geral.
 */
@Repository
public interface KvConfigRepository extends JpaRepository<KvConfig, String> {
}
