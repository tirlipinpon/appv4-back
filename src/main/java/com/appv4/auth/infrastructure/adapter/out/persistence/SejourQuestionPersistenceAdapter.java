package com.appv4.auth.infrastructure.adapter.out.persistence;

import java.util.Optional;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import com.appv4.auth.application.port.out.SejourQuestionPort;

/**
 * Énoncé et matière d'une question, lus pour le séjour (BACK-79).
 *
 * <p><b>Business :</b> le nom est celui de la matière qui porte la question.
 * <p><b>Technique :</b> JDBC sur {@code back_subject_questions} et
 * {@code back_teacher_subjects}. Absent du profil {@code test}.
 * <p><b>Métier / flow :</b> adapter out de {@link SejourQuestionPort}.
 */
@Component
@Profile("!test")
public class SejourQuestionPersistenceAdapter implements SejourQuestionPort {

	private final JdbcClient jdbc;

	public SejourQuestionPersistenceAdapter(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	@Override
	public Optional<QuestionCaption> find(long questionId) {
		return jdbc.sql("""
				select q.prompt, s.name as subject_name
				from back_subject_questions q
				join back_teacher_subjects s on s.id = q.subject_id
				where q.id = :questionId
				""")
				.param("questionId", questionId)
				.query((rs, row) -> new QuestionCaption(rs.getString("prompt"), rs.getString("subject_name")))
				.optional();
	}
}
