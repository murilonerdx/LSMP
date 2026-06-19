package br.com.murilo.liberthia.admin.engine.forbidden;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ForbiddenRuleRepository extends JpaRepository<ForbiddenRule, String> {
    List<ForbiddenRule> findByEnabledTrue();
}
