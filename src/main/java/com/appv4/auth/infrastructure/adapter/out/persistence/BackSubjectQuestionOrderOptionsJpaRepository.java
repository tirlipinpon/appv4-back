package com.appv4.auth.infrastructure.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

interface BackSubjectQuestionOrderOptionsJpaRepository
		extends JpaRepository<BackSubjectQuestionOrderOptionsEntity, Long> {

	void deleteByQuestionId(long questionId);
}
