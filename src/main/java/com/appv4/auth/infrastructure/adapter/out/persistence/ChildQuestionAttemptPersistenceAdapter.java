package com.appv4.auth.infrastructure.adapter.out.persistence;

import org.springframework.context.annotation.Profile;
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

	public ChildQuestionAttemptPersistenceAdapter(
			BackChildAttemptJpaRepository attempts, BackChildAttemptAnswerJpaRepository answers) {
		this.attempts = attempts;
		this.answers = answers;
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
				attempt.passed()));
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
		return new StoredAttempt(
				attemptId,
				saved.getChildId(),
				saved.getSubjectId(),
				saved.getQuestionId(),
				saved.getFormCode(),
				saved.getScorePercent(),
				saved.isPassed());
	}
}
