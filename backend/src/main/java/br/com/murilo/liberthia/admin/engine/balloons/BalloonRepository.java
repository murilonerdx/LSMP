package br.com.murilo.liberthia.admin.engine.balloons;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BalloonRepository extends JpaRepository<Balloon, Long> {
    Page<Balloon> findByPlayerUuidOrderByTsDesc(String playerUuid, Pageable p);
    Page<Balloon> findAllByOrderByTsDesc(Pageable p);
    Page<Balloon> findBySourceOrderByTsDesc(String source, Pageable p);

    @Query("SELECT b.playerUuid, MAX(b.playerName), COUNT(b), MAX(b.ts) FROM Balloon b " +
           "GROUP BY b.playerUuid ORDER BY COUNT(b) DESC")
    List<Object[]> aggregateByPlayer();

    @Query("SELECT COUNT(b) FROM Balloon b WHERE b.source = :source")
    long countBySource(@Param("source") String source);
}
