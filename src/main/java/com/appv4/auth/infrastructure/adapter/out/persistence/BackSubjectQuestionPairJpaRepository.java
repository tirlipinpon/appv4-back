package com.appv4.auth.infrastructure.adapter.out.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Paires d'un Memory, dans l'ordre saisi (SCRUM-175). */
public interface BackSubjectQuestionPairJpaRepository extends JpaRepository<BackSubjectQuestionPairEntity, Long> {

	List<BackSubjectQuestionPairEntity> findByQuestionIdInOrderByQuestionIdAscSortOrderAsc(List<Long> questionIds);

	/** Les anciennes paires partent avant d'écrire la correction. */
	@Modifying(flushAutomatically = true)
	@Query("delete from BackSubjectQuestionPairEntity row where row.questionId = :questionId")
	void deleteByQuestionId(@Param("questionId") long questionId);
}
