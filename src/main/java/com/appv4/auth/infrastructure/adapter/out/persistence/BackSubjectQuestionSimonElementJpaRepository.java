package com.appv4.auth.infrastructure.adapter.out.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Éléments des questions Simon, dans l'ordre saisi. */
public interface BackSubjectQuestionSimonElementJpaRepository
		extends JpaRepository<BackSubjectQuestionSimonElementEntity, Long> {

	List<BackSubjectQuestionSimonElementEntity> findByQuestionIdInOrderByQuestionIdAscSortOrderAsc(
			List<Long> questionIds);

	@Modifying(flushAutomatically = true)
	@Query("delete from BackSubjectQuestionSimonElementEntity row where row.questionId = :questionId")
	void deleteByQuestionId(@Param("questionId") long questionId);
}
