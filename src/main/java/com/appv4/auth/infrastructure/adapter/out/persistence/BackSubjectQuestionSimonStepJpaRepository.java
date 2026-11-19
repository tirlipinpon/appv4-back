package com.appv4.auth.infrastructure.adapter.out.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Pas des suites Simon, du premier au dernier. */
public interface BackSubjectQuestionSimonStepJpaRepository
		extends JpaRepository<BackSubjectQuestionSimonStepEntity, Long> {

	List<BackSubjectQuestionSimonStepEntity> findByQuestionIdInOrderByQuestionIdAscSortOrderAsc(
			List<Long> questionIds);

	@Modifying(flushAutomatically = true)
	@Query("delete from BackSubjectQuestionSimonStepEntity row where row.questionId = :questionId")
	void deleteByQuestionId(@Param("questionId") long questionId);
}
