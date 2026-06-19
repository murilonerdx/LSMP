package br.com.murilo.liberthia.admin.engine.autogift;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface AutoGiftRuleRepository extends JpaRepository<AutoGiftRule, Long> {
    List<AutoGiftRule> findByActiveTrueAndNextRunAtBefore(Instant cutoff);
    List<AutoGiftRule> findAllByOrderByCreatedAtDesc();
}
