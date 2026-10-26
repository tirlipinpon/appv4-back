package com.appv4.auth.infrastructure.adapter.out.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Accès professeurs ↔ matières (SCRUM-172).
 */
public interface BackTeacherSubjectAccessJpaRepository
		extends JpaRepository<BackTeacherSubjectAccessEntity, Long> {

	List<BackTeacherSubjectAccessEntity> findByUserIdOrderByIdAsc(long userId);

	List<BackTeacherSubjectAccessEntity> findBySubjectIdOrderByIdAsc(long subjectId);

	Optional<BackTeacherSubjectAccessEntity> findBySubjectIdAndUserId(long subjectId, long userId);

	boolean existsBySubjectIdAndUserId(long subjectId, long userId);

	long countBySubjectId(long subjectId);

	@Modifying(clearAutomatically = true)
	@Query("delete from BackTeacherSubjectAccessEntity a where a.subjectId = :subjectId")
	int deleteBySubjectId(@Param("subjectId") long subjectId);

	@Modifying(clearAutomatically = true)
	@Query("delete from BackTeacherSubjectAccessEntity a where a.subjectId = :subjectId and a.userId = :userId")
	int deleteBySubjectIdAndUserId(@Param("subjectId") long subjectId, @Param("userId") long userId);
}
