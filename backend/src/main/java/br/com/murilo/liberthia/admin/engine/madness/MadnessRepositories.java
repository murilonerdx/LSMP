package br.com.murilo.liberthia.admin.engine.madness;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
interface MadnessStateRepository extends JpaRepository<MadnessState, String> {}

@Repository
interface MadnessConfigRepository extends JpaRepository<MadnessConfig, Integer> {}
