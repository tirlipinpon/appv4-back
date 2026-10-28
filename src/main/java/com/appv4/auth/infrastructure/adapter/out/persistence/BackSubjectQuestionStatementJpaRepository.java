package com.appv4.auth.infrastructure.adapter.out.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Phrases d'une question Vrai ou faux, dans l'ordre saisi (SCRUM-177). */
public interface BackSubjectQuestionStatementJpaRepository
		extends JpaRepository<BackSubjectQuestionStatementEntity, Long> {

	List<BackSubjectQuestionStatementEntity> findByQuestionIdInOrderByQuestionIdAscSortOrderAsc(List<Long> questionIds);

	/** Les anciennes phrases partent avant d'écrire la correction. */
	@Modifying(flushAutomatically = true)
	@Query("delete from BackSubjectQuestionStatementEntity row where row.questionId = :questionId")
	void deleteByQuestionId(@Param("questionId") long questionId);
}
