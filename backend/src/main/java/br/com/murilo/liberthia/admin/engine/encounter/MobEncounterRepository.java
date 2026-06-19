package br.com.murilo.liberthia.admin.engine.encounter;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MobEncounterRepository extends JpaRepository<MobEncounter, Long> {
    Page<MobEncounter> findAllByOrderByUpdatedAtDesc(Pageable p);
}
