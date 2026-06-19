package br.com.murilo.liberthia.admin.engine.autogift;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AutoGiftRunRepository extends JpaRepository<AutoGiftRun, Long> {
    Page<AutoGiftRun> findByRuleIdOrderByRanAtDesc(Long ruleId, Pageable p);
    Page<AutoGiftRun> findAllByOrderByRanAtDesc(Pageable p);
}
