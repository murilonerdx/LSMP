package br.com.murilo.liberthia.admin.engine.etched;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EtchedDiscRepository extends JpaRepository<EtchedDisc, Long> {
    List<EtchedDisc> findAllByOrderByCreatedAtDesc();
    List<EtchedDisc> findByCategoryOrderByCreatedAtDesc(String category);
}
