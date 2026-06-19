package br.com.murilo.liberthia.admin.engine.photos;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ExposurePhotoRepository extends JpaRepository<ExposurePhoto, Long> {
    Page<ExposurePhoto> findAllByOrderByTakenAtDesc(Pageable p);
    Page<ExposurePhoto> findByCursedTrueOrderByTakenAtDesc(Pageable p);
    Page<ExposurePhoto> findByAuthorUuidOrderByTakenAtDesc(String uuid, Pageable p);

    /**
     * Agregação por player — usado pela Photo Library na grid inicial.
     * Retorna Object[]: uuid, name, count, totalBytes, firstTs, lastTs, cursedCount.
     */
    @Query("SELECT p.authorUuid, MAX(p.authorName), COUNT(p), " +
           "       COALESCE(SUM(p.sizeBytes), 0), " +
           "       MIN(p.takenAt), MAX(p.takenAt), " +
           "       SUM(CASE WHEN p.cursed = true THEN 1 ELSE 0 END) " +
           "FROM ExposurePhoto p " +
           "WHERE p.authorUuid IS NOT NULL AND p.authorUuid <> '' " +
           "GROUP BY p.authorUuid " +
           "ORDER BY MAX(p.takenAt) DESC")
    List<Object[]> aggregateByPlayer();

    /** Busca com filtros e ordenação — Photo Library quando entra na pasta do player. */
    @Query("SELECT p FROM ExposurePhoto p WHERE " +
           "  (:uuid IS NULL OR p.authorUuid = :uuid) AND " +
           "  (:minBytes = 0 OR p.sizeBytes >= :minBytes) AND " +
           "  (:maxBytes = 0 OR p.sizeBytes <= :maxBytes) AND " +
           "  (:fromMs = 0 OR p.takenAt >= :fromTime) AND " +
           "  (:toMs = 0 OR p.takenAt <= :toTime) AND " +
           "  (:onlyCursed = false OR p.cursed = true)")
    List<ExposurePhoto> search(@Param("uuid") String uuid,
                               @Param("minBytes") long minBytes,
                               @Param("maxBytes") long maxBytes,
                               @Param("fromMs") long fromMs,
                               @Param("toMs") long toMs,
                               @Param("fromTime") java.time.Instant fromTime,
                               @Param("toTime") java.time.Instant toTime,
                               @Param("onlyCursed") boolean onlyCursed,
                               Pageable p);
}
