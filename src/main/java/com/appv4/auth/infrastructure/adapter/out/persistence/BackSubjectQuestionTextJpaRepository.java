package com.appv4.auth.infrastructure.adapter.out.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Réponse attendue d'une question libre (SCRUM-198). */
public interface BackSubjectQuestionTextJpaRepository extends JpaRepository<BackSubjectQuestionTextEntity, Long> {

	List<BackSubjectQuestionTextEntity> findByQuestionIdIn(List<Long> questionIds);

	/** L'ancienne réponse part avant d'écrire la correction. */
	@Modifying(flushAutomatically = true)
	@Query("delete from BackSubjectQuestionTextEntity row where row.questionId = :questionId")
	void deleteByQuestionId(@Param("questionId") long questionId);
}
