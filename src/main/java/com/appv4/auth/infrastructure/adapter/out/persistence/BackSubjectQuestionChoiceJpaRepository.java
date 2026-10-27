package com.appv4.auth.infrastructure.adapter.out.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

/** Propositions d'une question, dans l'ordre saisi (SCRUM-174). */
public interface BackSubjectQuestionChoiceJpaRepository extends JpaRepository<BackSubjectQuestionChoiceEntity, Long> {

	List<BackSubjectQuestionChoiceEntity> findByQuestionIdOrderBySortOrderAsc(long questionId);

	/** Les anciennes propositions partent avant d'écrire la correction. */
	void deleteByQuestionId(long questionId);
}
