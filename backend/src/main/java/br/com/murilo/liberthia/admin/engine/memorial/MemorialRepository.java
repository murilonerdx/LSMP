package br.com.murilo.liberthia.admin.engine.memorial;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemorialRepository extends JpaRepository<Memorial, Long> {
    Page<Memorial> findAllByOrderByDiedAtDesc(Pageable p);
    Page<Memorial> findByPermanentTrueOrderByDiedAtDesc(Pageable p);
    Page<Memorial> findByPlayerUuidOrderByDiedAtDesc(String uuid, Pageable p);
    Page<Memorial> findAllByOrderByReactionsDesc(Pageable p);
}
