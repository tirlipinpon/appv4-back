package com.appv4.auth.infrastructure.adapter.out.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

/** Énoncés d'une matière, du plus ancien au plus récent (SCRUM-174). */
public interface BackSubjectQuestionJpaRepository extends JpaRepository<BackSubjectQuestionEntity, Long> {

	List<BackSubjectQuestionEntity> findBySubjectIdOrderByIdAsc(long subjectId);
}
