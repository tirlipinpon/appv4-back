package com.appv4.auth.infrastructure.adapter.out.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

/** Pièces d'un puzzle (SCRUM-183). */
public interface BackSubjectQuestionPuzzlePieceJpaRepository
		extends JpaRepository<BackSubjectQuestionPuzzlePieceEntity, Long> {

	List<BackSubjectQuestionPuzzlePieceEntity> findByQuestionIdInOrderByQuestionIdAscSortOrderAsc(
			List<Long> questionIds);
}
