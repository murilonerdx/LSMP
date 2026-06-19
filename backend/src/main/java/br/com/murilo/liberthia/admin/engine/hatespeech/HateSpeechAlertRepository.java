package br.com.murilo.liberthia.admin.engine.hatespeech;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface HateSpeechAlertRepository extends JpaRepository<HateSpeechAlert, Long> {

    /** Lista alertas com filtros opcionais. Null = sem filtro. */
    @Query("SELECT a FROM HateSpeechAlert a WHERE " +
           "  (:reviewed IS NULL OR a.reviewed = :reviewed) AND " +
           "  (:playerUuid IS NULL OR a.playerUuid = :playerUuid) AND " +
           "  (:severity IS NULL OR a.severity = :severity) " +
           "ORDER BY a.ts DESC")
    List<HateSpeechAlert> search(@Param("reviewed") Boolean reviewed,
                                  @Param("playerUuid") String playerUuid,
                                  @Param("severity") String severity,
                                  Pageable pageable);

    /** Conta total pendente de review (pra badge na UI). */
    long countByReviewedFalse();

    /** Conta total por player (pra ranking de reportados). */
    @Query("SELECT a.playerUuid, MAX(a.playerName), COUNT(a) " +
           "FROM HateSpeechAlert a WHERE a.playerUuid IS NOT NULL " +
           "GROUP BY a.playerUuid " +
           "ORDER BY COUNT(a) DESC")
    List<Object[]> rankByPlayer(Pageable pageable);

    /** Conta por categoria (estatística pra dashboard). */
    @Query("SELECT a.severity, COUNT(a) FROM HateSpeechAlert a GROUP BY a.severity")
    List<Object[]> countBySeverity();
}
