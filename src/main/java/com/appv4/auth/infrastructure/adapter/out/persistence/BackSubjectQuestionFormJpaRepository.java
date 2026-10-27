package com.appv4.auth.infrastructure.adapter.out.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

/** Forme déjà notée sur une matière (SCRUM-174). */
public interface BackSubjectQuestionFormJpaRepository extends JpaRepository<BackSubjectQuestionFormEntity, Long> {

	Optional<BackSubjectQuestionFormEntity> findBySubjectIdAndFormCode(long subjectId, String formCode);
}
