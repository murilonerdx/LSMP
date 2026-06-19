package br.com.murilo.liberthia.admin.engine.cutscene2;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface Cutscene2Repository extends JpaRepository<Cutscene2, Long> {
    Page<Cutscene2> findAllByOrderByUpdatedAtDesc(Pageable p);
}
