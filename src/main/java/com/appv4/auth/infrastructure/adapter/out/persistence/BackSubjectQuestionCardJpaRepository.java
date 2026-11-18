package com.appv4.auth.infrastructure.adapter.out.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Cartes d'une question classement, dans l'ordre saisi. */
public interface BackSubjectQuestionCardJpaRepository extends JpaRepository<BackSubjectQuestionCardEntity, Long> {

	List<BackSubjectQuestionCardEntity> findByQuestionIdInOrderByQuestionIdAscSortOrderAsc(List<Long> questionIds);

	@Modifying(flushAutomatically = true)
	@Query("delete from BackSubjectQuestionCardEntity row where row.questionId = :questionId")
	void deleteByQuestionId(@Param("questionId") long questionId);
}
