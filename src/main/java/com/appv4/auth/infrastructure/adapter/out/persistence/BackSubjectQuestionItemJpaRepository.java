package com.appv4.auth.infrastructure.adapter.out.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Éléments d'une question Ordre, dans l'ordre saisi (SCRUM-176). */
public interface BackSubjectQuestionItemJpaRepository extends JpaRepository<BackSubjectQuestionItemEntity, Long> {

	List<BackSubjectQuestionItemEntity> findByQuestionIdInOrderByQuestionIdAscSortOrderAsc(List<Long> questionIds);

	/** Les anciens éléments partent avant d'écrire la correction. */
	@Modifying(flushAutomatically = true)
	@Query("delete from BackSubjectQuestionItemEntity row where row.questionId = :questionId")
	void deleteByQuestionId(@Param("questionId") long questionId);
}
