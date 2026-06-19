package br.com.murilo.liberthia.admin.engine.security;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface SecurityEventRepository extends JpaRepository<SecurityEvent, Long> {
    Page<SecurityEvent> findAllByOrderByOccurredAtDesc(Pageable p);
    Page<SecurityEvent> findByOwnerUuidOrderByOccurredAtDesc(String uuid, Pageable p);
    Page<SecurityEvent> findByIntruderUuidOrderByOccurredAtDesc(String uuid, Pageable p);
    Page<SecurityEvent> findByKindOrderByOccurredAtDesc(String kind, Pageable p);
    List<SecurityEvent> findByOccurredAtAfterOrderByOccurredAtDesc(Instant after);
}
