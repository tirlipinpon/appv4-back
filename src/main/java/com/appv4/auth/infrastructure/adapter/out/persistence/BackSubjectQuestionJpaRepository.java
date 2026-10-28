package com.appv4.auth.infrastructure.adapter.out.persistence;

import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Énoncés d'une matière, du plus ancien au plus récent (SCRUM-174). */
public interface BackSubjectQuestionJpaRepository extends JpaRepository<BackSubjectQuestionEntity, Long> {

	List<BackSubjectQuestionEntity> findBySubjectIdOrderByIdAsc(long subjectId);

	Optional<BackSubjectQuestionEntity> findByIdAndSubjectId(long id, long subjectId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select question from BackSubjectQuestionEntity question where question.id = :id")
	Optional<BackSubjectQuestionEntity> findForUpdate(@Param("id") long id);
}
