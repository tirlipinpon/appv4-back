package com.appv4.auth.infrastructure.adapter.out.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Image WebP d'une question à cliquer (SCRUM-181). */
public interface BackSubjectQuestionClickImageJpaRepository
		extends JpaRepository<BackSubjectQuestionClickImageEntity, Long> {

	List<BackSubjectQuestionClickImageEntity> findByQuestionIdIn(List<Long> questionIds);

	/** L'ancienne image part avant d'écrire la correction. */
	@Modifying(flushAutomatically = true)
	@Query("delete from BackSubjectQuestionClickImageEntity row where row.questionId = :questionId")
	void deleteByQuestionId(@Param("questionId") long questionId);
}
