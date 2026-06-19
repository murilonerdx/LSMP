package br.com.murilo.liberthia.admin.engine.loot;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LootItemRepository extends JpaRepository<LootItem, Long> {
    List<LootItem> findAllByOrderByRarityAscNameAsc();
    List<LootItem> findByEnabledTrue();
    List<LootItem> findByEnabledTrueAndCategory(String category);
    List<LootItem> findByEnabledTrueAndRarity(String rarity);
}
