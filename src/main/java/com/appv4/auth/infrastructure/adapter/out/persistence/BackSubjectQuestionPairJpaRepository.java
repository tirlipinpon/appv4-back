package com.appv4.auth.infrastructure.adapter.out.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

/** Paires d'un Memory, dans l'ordre saisi (SCRUM-175). */
public interface BackSubjectQuestionPairJpaRepository extends JpaRepository<BackSubjectQuestionPairEntity, Long> {

	List<BackSubjectQuestionPairEntity> findByQuestionIdOrderBySortOrderAsc(long questionId);

	/** Les anciennes paires partent avant d'écrire la correction. */
	void deleteByQuestionId(long questionId);
}
