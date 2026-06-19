package br.com.murilo.liberthia.admin.engine.storyquest;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoryQuestRepository extends JpaRepository<StoryQuest, Long> {
    Page<StoryQuest> findAllByOrderByUpdatedAtDesc(Pageable p);
}
