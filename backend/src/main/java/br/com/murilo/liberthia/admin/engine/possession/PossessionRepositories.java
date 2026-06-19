package br.com.murilo.liberthia.admin.engine.possession;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
interface PossessionEntityRepository extends JpaRepository<PossessionEntity, String> {}

@Repository
interface PossessionActiveRepository extends JpaRepository<PossessionActive, Long> {
    List<PossessionActive> findByEndsAtAfter(Instant t);
    List<PossessionActive> findByEndsAtBefore(Instant t);
}
