package com.appv4.auth.infrastructure.adapter.out.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

/** Image WebP d'un puzzle (SCRUM-183). */
public interface BackSubjectQuestionPuzzleImageJpaRepository
		extends JpaRepository<BackSubjectQuestionPuzzleImageEntity, Long> {

	List<BackSubjectQuestionPuzzleImageEntity> findByQuestionIdIn(List<Long> questionIds);
}
