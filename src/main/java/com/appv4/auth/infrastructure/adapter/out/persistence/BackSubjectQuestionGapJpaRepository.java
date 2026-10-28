package com.appv4.auth.infrastructure.adapter.out.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Mots d'une Case vide, mots justes puis leurres (SCRUM-179). */
public interface BackSubjectQuestionGapJpaRepository extends JpaRepository<BackSubjectQuestionGapEntity, Long> {

	List<BackSubjectQuestionGapEntity> findByQuestionIdInOrderByQuestionIdAscDecoyAscSortOrderAsc(
			List<Long> questionIds);

	/** Les anciens mots partent avant d'écrire la correction. */
	@Modifying(flushAutomatically = true)
	@Query("delete from BackSubjectQuestionGapEntity row where row.questionId = :questionId")
	void deleteByQuestionId(@Param("questionId") long questionId);
}
