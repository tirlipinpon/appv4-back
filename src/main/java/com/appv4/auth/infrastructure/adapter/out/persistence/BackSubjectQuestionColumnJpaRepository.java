package com.appv4.auth.infrastructure.adapter.out.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Colonnes d'une question classement, dans l'ordre saisi. */
public interface BackSubjectQuestionColumnJpaRepository
		extends JpaRepository<BackSubjectQuestionColumnEntity, Long> {

	List<BackSubjectQuestionColumnEntity> findByQuestionIdInOrderByQuestionIdAscSortOrderAsc(
			List<Long> questionIds);

	@Modifying(flushAutomatically = true)
	@Query("delete from BackSubjectQuestionColumnEntity row where row.questionId = :questionId")
	void deleteByQuestionId(@Param("questionId") long questionId);
}
