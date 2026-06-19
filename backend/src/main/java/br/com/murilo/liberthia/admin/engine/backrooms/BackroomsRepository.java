package br.com.murilo.liberthia.admin.engine.backrooms;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BackroomsRepository extends JpaRepository<BackroomsEntry, Long> {
    Page<BackroomsEntry> findAllByOrderByOccurredAtDesc(Pageable p);
    List<BackroomsEntry> findTop20ByLevelIdOrderByOccurredAtDesc(String levelId);
    List<BackroomsEntry> findTop10ByPlayerUuidOrderByOccurredAtDesc(String uuid);
}
