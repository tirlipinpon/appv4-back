package com.appv4.auth.infrastructure.adapter.out.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Lecture de l'annuaire d'écoles (SCRUM-163).
 */
public interface BackSchoolJpaRepository extends JpaRepository<BackSchoolEntity, Long> {

	List<BackSchoolEntity> findAllByOrderByIdAsc();
}
