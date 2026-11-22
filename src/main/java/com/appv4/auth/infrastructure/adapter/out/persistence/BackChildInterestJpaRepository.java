package com.appv4.auth.infrastructure.adapter.out.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Copies de centres d'intérêt (BACK-72).
 */
public interface BackChildInterestJpaRepository extends JpaRepository<BackChildInterestEntity, Long> {

	List<BackChildInterestEntity> findByChildId(long childId);

	void deleteByChildId(long childId);
}
