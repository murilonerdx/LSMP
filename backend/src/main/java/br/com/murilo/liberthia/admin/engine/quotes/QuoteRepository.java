package br.com.murilo.liberthia.admin.engine.quotes;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface QuoteRepository extends JpaRepository<Quote, Long> {
    Page<Quote> findByPlayerUuidOrderByCapturedAtDesc(String uuid, Pageable p);
    Page<Quote> findAllByOrderByScoreDesc(Pageable p);
    Page<Quote> findAllByOrderByCapturedAtDesc(Pageable p);

    /**
     * Busca texto/jogador. Caller embrulha com % e faz toLowerCase
     * (Hibernate 6 quebra com LOWER(CONCAT(...)) por inferência de tipo
     * — derruba o app no startup).
     */
    @Query("SELECT q FROM Quote q WHERE LOWER(q.text) LIKE :pattern OR LOWER(q.playerName) LIKE :pattern")
    Page<Quote> search(@Param("pattern") String pattern, Pageable p);
}
