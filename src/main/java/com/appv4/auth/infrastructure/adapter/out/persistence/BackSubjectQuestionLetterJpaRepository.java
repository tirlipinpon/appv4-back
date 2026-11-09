package com.appv4.auth.infrastructure.adapter.out.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Réponse attendue d'une saisie lettre par lettre (SCRUM-182). */
public interface BackSubjectQuestionLetterJpaRepository extends JpaRepository<BackSubjectQuestionLetterEntity, Long> {

	List<BackSubjectQuestionLetterEntity> findByQuestionIdIn(List<Long> questionIds);

	/** L'ancien mot part avant d'écrire la correction. */
	@Modifying(flushAutomatically = true)
	@Query("delete from BackSubjectQuestionLetterEntity row where row.questionId = :questionId")
	void deleteByQuestionId(@Param("questionId") long questionId);
}
