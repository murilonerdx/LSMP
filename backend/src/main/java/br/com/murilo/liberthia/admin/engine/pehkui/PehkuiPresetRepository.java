package br.com.murilo.liberthia.admin.engine.pehkui;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PehkuiPresetRepository extends JpaRepository<PehkuiPreset, Long> {
    Optional<PehkuiPreset> findByName(String name);
    List<PehkuiPreset> findAllByOrderByNameAsc();
}
