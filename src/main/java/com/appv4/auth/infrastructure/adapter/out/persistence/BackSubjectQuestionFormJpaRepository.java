package com.appv4.auth.infrastructure.adapter.out.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Forme déjà notée sur une matière (SCRUM-174). */
public interface BackSubjectQuestionFormJpaRepository extends JpaRepository<BackSubjectQuestionFormEntity, Long> {

	Optional<BackSubjectQuestionFormEntity> findBySubjectIdAndFormCode(long subjectId, String formCode);

	/** Idempotent même si deux professeurs ajoutent simultanément la première question. */
	@Modifying
	@Query(value = """
			insert into back_subject_question_forms (subject_id, form_code, label, sort_order)
			values (:subjectId, :formCode, :label, :sortOrder)
			on conflict (subject_id, form_code) do nothing
			""", nativeQuery = true)
	int insertIfAbsent(@Param("subjectId") long subjectId, @Param("formCode") String formCode,
			@Param("label") String label, @Param("sortOrder") int sortOrder);
}
