package com.appv4.auth.infrastructure.adapter.out.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Matières enregistrées pour un professeur (SCRUM-161).
 */
public interface BackTeacherSubjectJpaRepository extends JpaRepository<BackTeacherSubjectEntity, Long> {

	List<BackTeacherSubjectEntity> findByUserIdOrderByIdAsc(long userId);

	Optional<BackTeacherSubjectEntity> findByIdAndUserId(long id, long userId);
}
