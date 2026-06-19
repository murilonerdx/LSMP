package br.com.murilo.liberthia.admin.engine.wardrobe;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WardrobeRepository extends JpaRepository<WardrobeItem, Long> {
    Page<WardrobeItem> findAllByOrderByCreatedAtDesc(Pageable p);
    Page<WardrobeItem> findByKindOrderByCreatedAtDesc(String kind, Pageable p);
    Page<WardrobeItem> findAllByOrderByUpvotesDesc(Pageable p);
}
