package br.com.murilo.liberthia.admin.engine.forbidden;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ForbiddenInvocationRepository extends JpaRepository<ForbiddenInvocation, Long> {

    @Query("SELECT i FROM ForbiddenInvocation i ORDER BY i.ts DESC")
    List<ForbiddenInvocation> findRecent(Pageable pageable);
}
