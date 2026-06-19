package br.com.murilo.liberthia.admin.engine.cursed;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
interface CurseRepository extends JpaRepository<Curse, String> {}

@Repository
interface CurseBindingRepository extends JpaRepository<CurseBinding, Long> {}
