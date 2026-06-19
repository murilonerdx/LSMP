package br.com.murilo.liberthia.admin.engine.cinema;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface CinemaRepository extends JpaRepository<CinemaSession, Long> {
    Page<CinemaSession> findAllByOrderByStartsAtDesc(Pageable p);
    Page<CinemaSession> findByStatusOrderByStartsAtAsc(String status, Pageable p);
    List<CinemaSession> findByStatusAndStartsAtLessThanEqual(String status, Instant at);
}
