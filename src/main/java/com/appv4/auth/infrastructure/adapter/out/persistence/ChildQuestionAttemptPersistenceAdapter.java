package com.appv4.auth.infrastructure.adapter.out.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.appv4.auth.application.port.out.ChildQuestionAttemptPort;

/**
 * Persiste les tentatives enfant (BACK-16).
 *
 * <p><b>Business :</b> une validation = une ligne attempt + ses réponses.
 * <p><b>Technique :</b> JPA sur {@code back_child_attempts}. Absent du profil {@code test}.
 * <p><b>Métier / flow :</b> adapter out du port {@link ChildQuestionAttemptPort}.
 */
@Component
@Profile("!test")
public class ChildQuestionAttemptPersistenceAdapter implements ChildQuestionAttemptPort {

	private final BackChildAttemptJpaRepository attempts;
	private final BackChildAttemptAnswerJpaRepository answers;
	private final JdbcClient jdbc;

	public ChildQuestionAttemptPersistenceAdapter(
			BackChildAttemptJpaRepository attempts, BackChildAttemptAnswerJpaRepository answers, JdbcClient jdbc) {
		this.attempts = attempts;
		this.answers = answers;
		this.jdbc = jdbc;
	}

	@Override
	@Transactional
	public Optional<StoredAttempt> lockAndFind(long childId, UUID submissionId) {
		jdbc.sql("select id from back_children where id = :child for no key update")
				.param("child", childId).query(Long.class).optional();
		return attempts.findByChildIdAndSubmissionId(childId, submissionId).map(ChildQuestionAttemptPersistenceAdapter::stored);
	}

	@Override
	@Transactional
	public StoredAttempt insert(NewAttempt attempt) {
		BackChildAttemptEntity saved = attempts.save(new BackChildAttemptEntity(
				attempt.childId(),
				attempt.subjectId(),
				attempt.questionId(),
				attempt.formCode(),
				attempt.scorePercent(),
				attempt.passed(),
				attempt.submissionId()));
		long attemptId = saved.getId();
		for (AnswerLine line : attempt.answers()) {
			answers.save(new BackChildAttemptAnswerEntity(
					attemptId,
					line.targetKind(),
					line.targetId(),
					line.answerBool(),
					line.answerChoiceId(),
					line.correct()));
		}
		return stored(saved);
	}

	private static StoredAttempt stored(BackChildAttemptEntity saved) {
		return new StoredAttempt(
				saved.getId(),
				saved.getChildId(),
				saved.getSubjectId(),
				saved.getQuestionId(),
				saved.getFormCode(),
				saved.getScorePercent(),
				saved.isPassed());
	}
}
