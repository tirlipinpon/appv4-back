package com.appv4.auth.infrastructure.adapter.out.persistence;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Énoncés d'une matière, du plus ancien au plus récent (SCRUM-174). */
public interface BackSubjectQuestionJpaRepository extends JpaRepository<BackSubjectQuestionEntity, Long> {

	List<BackSubjectQuestionEntity> findBySubjectIdAndIdGreaterThanOrderByIdAsc(long subjectId, long afterId, Pageable page);

	Optional<BackSubjectQuestionEntity> findByIdAndSubjectId(long id, long subjectId);

	/**
	 * Totaux par matière. Chaque ligne est {@code [subjectId, count]}.
	 * Les matières sans question n'apparaissent pas.
	 */
	@Query("""
			select question.subjectId, count(question)
			from BackSubjectQuestionEntity question
			where question.subjectId in :subjectIds
			group by question.subjectId
			""")
	List<Object[]> countGroupedBySubjectId(@Param("subjectIds") Collection<Long> subjectIds);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select question from BackSubjectQuestionEntity question where question.id = :id")
	Optional<BackSubjectQuestionEntity> findForUpdate(@Param("id") long id);
}
