package br.com.murilo.liberthia.admin.engine.dialog;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DialogTreeRepository extends JpaRepository<DialogTree, Long> {
    Page<DialogTree> findAllByOrderByUpdatedAtDesc(Pageable p);
    Optional<DialogTree> findByNpcTag(String tag);
}
