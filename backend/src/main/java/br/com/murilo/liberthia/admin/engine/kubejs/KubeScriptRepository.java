package br.com.murilo.liberthia.admin.engine.kubejs;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KubeScriptRepository extends JpaRepository<KubeScript, Long> {
    Page<KubeScript> findAllByOrderByUpdatedAtDesc(Pageable p);
    Page<KubeScript> findByStatusOrderByUpdatedAtDesc(String status, Pageable p);
    Page<KubeScript> findByAuthorUuidOrderByUpdatedAtDesc(String uuid, Pageable p);
}
