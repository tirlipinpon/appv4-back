package com.appv4.auth.infrastructure.adapter.out.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BackSubjectQuestionAttemptPointJpaRepository
		extends JpaRepository<BackSubjectQuestionAttemptPointEntity, BackSubjectQuestionAttemptPointKey> {

	List<BackSubjectQuestionAttemptPointEntity> findByQuestionIdOrderBySortOrderAsc(long questionId);

	void deleteByQuestionId(long questionId);
}
