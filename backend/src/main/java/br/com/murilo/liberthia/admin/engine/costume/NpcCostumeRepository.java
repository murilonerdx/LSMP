package br.com.murilo.liberthia.admin.engine.costume;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NpcCostumeRepository extends JpaRepository<NpcCostume, Long> {
    Page<NpcCostume> findAllByOrderByUpdatedAtDesc(Pageable p);
    Page<NpcCostume> findByArchetypeOrderByUpdatedAtDesc(String archetype, Pageable p);
}
