package br.com.murilo.liberthia.admin.engine.waystones;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WaystoneRepository extends JpaRepository<Waystone, Long> {
    Page<Waystone> findAllByOrderByCreatedAtDesc(Pageable p);
    Page<Waystone> findByPublicAccessTrueOrderByUseCountDesc(Pageable p);
    Page<Waystone> findByDimensionOrderByName(String dim, Pageable p);
    Page<Waystone> findByOwnerUuidOrderByCreatedAtDesc(String uuid, Pageable p);
}
