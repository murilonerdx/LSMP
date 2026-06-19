package br.com.murilo.liberthia.admin.engine.replay;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReplayRepository extends JpaRepository<Replay, Long> {
    Page<Replay> findAllByOrderByUpdatedAtDesc(Pageable p);
}
