package com.appv4.auth.infrastructure.adapter.out.persistence;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

interface BackSubjectQuestionOrderOptionsJpaRepository
		extends JpaRepository<BackSubjectQuestionOrderOptionsEntity, Long> {

	List<BackSubjectQuestionOrderOptionsEntity> findByQuestionIdIn(Collection<Long> questionIds);

	void deleteByQuestionId(long questionId);
}
