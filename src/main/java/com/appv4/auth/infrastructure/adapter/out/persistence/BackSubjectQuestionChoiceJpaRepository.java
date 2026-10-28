package com.appv4.auth.infrastructure.adapter.out.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Propositions d'une question, dans l'ordre saisi (SCRUM-174). */
public interface BackSubjectQuestionChoiceJpaRepository extends JpaRepository<BackSubjectQuestionChoiceEntity, Long> {

	List<BackSubjectQuestionChoiceEntity> findByQuestionIdInOrderByQuestionIdAscSortOrderAsc(List<Long> questionIds);

	/** Les anciennes propositions partent avant d'écrire la correction. */
	@Modifying(flushAutomatically = true)
	@Query("delete from BackSubjectQuestionChoiceEntity row where row.questionId = :questionId")
	void deleteByQuestionId(@Param("questionId") long questionId);
}
