package com.appv4.auth.infrastructure.adapter.out.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Enfants d'un parent (SCRUM-156).
 */
public interface BackChildJpaRepository extends JpaRepository<BackChildEntity, Long> {

	List<BackChildEntity> findByParentUserIdOrderByIdAsc(long parentUserId);

	Optional<BackChildEntity> findByIdAndParentUserId(long id, long parentUserId);
}
