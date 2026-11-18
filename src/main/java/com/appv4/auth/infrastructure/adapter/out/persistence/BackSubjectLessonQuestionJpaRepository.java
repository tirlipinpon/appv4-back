package com.appv4.auth.infrastructure.adapter.out.persistence;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BackSubjectLessonQuestionJpaRepository
		extends JpaRepository<BackSubjectLessonQuestionEntity, BackSubjectLessonQuestionEntity.Pk> {

	List<BackSubjectLessonQuestionEntity> findByLessonIdInOrderBySortOrderAscQuestionIdAsc(
			Collection<Long> lessonIds);
}
