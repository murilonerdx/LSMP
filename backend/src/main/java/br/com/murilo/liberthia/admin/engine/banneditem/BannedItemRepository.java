package br.com.murilo.liberthia.admin.engine.banneditem;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BannedItemRepository extends JpaRepository<BannedItem, Long> {
    Optional<BannedItem> findByItemId(String itemId);
    List<BannedItem> findByActiveTrue();
    List<BannedItem> findAllByOrderByBannedAtDesc();
}
